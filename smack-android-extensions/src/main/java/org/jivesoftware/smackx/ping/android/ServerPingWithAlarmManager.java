/**
 *
 * Copyright © 2014-2017 Florian Schmaus
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jivesoftware.smackx.ping.android;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Logger;

import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.util.Async;
import org.jivesoftware.smackx.ping.PingManager;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;

/**
 * Send automatic server pings with the help of {@link AlarmManager}.
 * <p>
 * Smack's {@link PingManager} uses a <code>ScheduledThreadPoolExecutor</code> to schedule the
 * automatic server pings, but on Android, those scheduled pings are not reliable. This is because
 * the Android device may go into deep sleep where the system will not continue to run this causes
 * <ul>
 * <li>the system time to not move forward, which means that the time spent in deep sleep is not
 * counted towards the scheduled delay time</li>
 * <li>the scheduled Runnable is not run while the system is in deep sleep.</li>
 * </ul>
 * That is the reason Android comes with an API to schedule those tasks: AlarmManager. Which this
 * class uses to determine every 30 minutes if a server ping is necessary. The interval of 30
 * minutes is the ideal trade-off between reliability and low resource (battery) consumption.
 * </p>
 * <p>
 * In order to use this class you need to call {@link #onCreate(Context)} <b>once</b>, for example
 * in the <code>onCreate()</code> method of your Service holding the XMPPConnection. And to avoid
 * leaking any resources, you should call {@link #onDestroy()} when you no longer need any of its
 * functionality.
 * </p>
 */
public final class ServerPingWithAlarmManager extends Manager {

    private static final Logger LOGGER = Logger.getLogger(ServerPingWithAlarmManager.class
            .getName());

    private static final String PING_ALARM_ACTION = "org.igniterealtime.smackx.ping.ACTION";

    private static final Map<XMPPConnection, ServerPingWithAlarmManager> INSTANCES = new WeakHashMap<XMPPConnection, ServerPingWithAlarmManager>();

    static {
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(XMPPConnection connection) {
                getInstanceFor(connection);
            }
        });
    }

    public static synchronized ServerPingWithAlarmManager getInstanceFor(XMPPConnection connection) {
        ServerPingWithAlarmManager serverPingWithAlarmManager = INSTANCES.get(connection);
        if (serverPingWithAlarmManager == null) {
            serverPingWithAlarmManager = new ServerPingWithAlarmManager(connection);
            INSTANCES.put(connection, serverPingWithAlarmManager);
        }
        return serverPingWithAlarmManager;
    }

    private boolean mEnabled = true;

    private ServerPingWithAlarmManager(XMPPConnection connection) {
        super(connection);
    }

    /**
     * If enabled, ServerPingWithAlarmManager will call {@link PingManager#pingServerIfNecessary()}
     * for the connection of this instance every half hour.
     *
     * @param enabled
     */
    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    private static final BroadcastReceiver ALARM_BROADCAST_RECEIVER = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            LOGGER.fine("Ping Alarm broadcast received");
            Set<Entry<XMPPConnection, ServerPingWithAlarmManager>> managers;
            synchronized (ServerPingWithAlarmManager.class) {
                // Make a copy to avoid ConcurrentModificationException when
                // iterating directly over INSTANCES and the Set is modified
                // concurrently by creating a new ServerPingWithAlarmManager.
                managers = new HashSet<>(INSTANCES.entrySet());
            }
            for (Entry<XMPPConnection, ServerPingWithAlarmManager> entry : managers) {
                XMPPConnection connection = entry.getKey();
                if (entry.getValue().isEnabled()) {
                    LOGGER.fine("Calling pingServerIfNecessary for connection "
                            + connection);
                    final PingManager pingManager = PingManager.getInstanceFor(connection);
                    // Android BroadcastReceivers have a timeout of 60 seconds.
                    // The connections reply timeout may be higher, which causes
                    // timeouts of the broadcast receiver and a subsequent ANR
                    // of the App of the broadcast receiver. We therefore need
                    // to call pingServerIfNecessary() in a new thread to avoid
                    // this. It could happen that the device gets back to sleep
                    // until the Thread runs, but that's a risk we are willing
                    // to take into account as it's unlikely.
                    Async.go(new Runnable() {
                        @Override
                        public void run() {
                            pingManager.pingServerIfNecessary();
                        }
                    }, "PingServerIfNecessary (" + connection.getConnectionCounter() + ')');
                } else {
                    LOGGER.fine("NOT calling pingServerIfNecessary (disabled) on connection "
                            + connection.getConnectionCounter());
                }
            }
        }
    };

    private static Context sContext;
    private static PendingIntent sPendingIntent;
    private static AlarmManager sAlarmManager;

    /**
     * Register a pending intent with the AlarmManager to be broadcasted every half hour and
     * register the alarm broadcast receiver to receive this intent. The receiver will check all
     * known questions if a ping is Necessary when invoked by the alarm intent.
     *
     * @param context
     */
    public static void onCreate(Context context) {
        sContext = context;
        context.registerReceiver(ALARM_BROADCAST_RECEIVER, new IntentFilter(PING_ALARM_ACTION));
        sAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        sPendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(PING_ALARM_ACTION), 0);
        sAlarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_HALF_HOUR,
                AlarmManager.INTERVAL_HALF_HOUR, sPendingIntent);
    }

    /**
     * Unregister the alarm broadcast receiver and cancel the alarm.
     */
    public static void onDestroy() {
        sContext.unregisterReceiver(ALARM_BROADCAST_RECEIVER);
        sAlarmManager.cancel(sPendingIntent);
    }
}
