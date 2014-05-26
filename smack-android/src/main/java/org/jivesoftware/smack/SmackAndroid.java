package org.jivesoftware.smack;

import java.util.logging.Logger;

import org.jivesoftware.smack.util.DNSUtil;
import org.jivesoftware.smack.util.dns.dnsjava.DNSJavaResolver;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.ResolverConfig;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

public class SmackAndroid {
	private static final Logger LOGGER = Logger.getLogger(SmackAndroid.class.getName());

	private static SmackAndroid sSmackAndroid = null;

	private BroadcastReceiver mConnectivityChangedReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			LOGGER.fine("ConnectivityChange received, calling ResolverConfig.refresh() and Lookup.refreshDefault()");
			ResolverConfig.refresh();
			Lookup.refreshDefault();
		}
	};

	private static boolean receiverRegistered = false;
	private Context mCtx;

	private SmackAndroid(Context ctx) {
		mCtx = ctx;
		DNSUtil.setDNSResolver(DNSJavaResolver.getInstance());
	}

	/**
	 * Init Smack for Android. Make sure to call
	 * SmackAndroid.onDestroy() in all the exit code paths of your
	 * application.
	 */
	public static synchronized SmackAndroid init(Context ctx) {
		if (sSmackAndroid == null) {
			sSmackAndroid = new SmackAndroid(ctx);
		}
		sSmackAndroid.maybeRegisterReceiver();
		return sSmackAndroid;
	}

	/**
	 * Cleanup all components initialized by init(). Make sure to call
	 * this method in all the exit code paths of your application.
	 */
	public synchronized void onDestroy() {
		LOGGER.fine("onDestroy: receiverRegistered=" + receiverRegistered);
		if (receiverRegistered) {
			mCtx.unregisterReceiver(mConnectivityChangedReceiver);
			receiverRegistered = false;
		}
	}

	private void maybeRegisterReceiver() {
		LOGGER.fine("maybeRegisterReceiver: receiverRegistered=" + receiverRegistered);
		if (!receiverRegistered) {
			mCtx.registerReceiver(mConnectivityChangedReceiver, new IntentFilter(
					ConnectivityManager.CONNECTIVITY_ACTION));
			receiverRegistered = true;
		}
	}
}
