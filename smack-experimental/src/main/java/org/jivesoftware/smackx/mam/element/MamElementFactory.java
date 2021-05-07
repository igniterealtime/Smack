package org.jivesoftware.smackx.mam.element;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.forward.packet.Forwarded;
import org.jivesoftware.smackx.mam.MamManager;
import org.jivesoftware.smackx.rsm.packet.RSMSet;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jxmpp.jid.Jid;

import java.util.List;

/**
 * A factory that creates extension elements and IQs for a specific MAM version. {@link #getSupportedMamNamespace()}
 * returns the namespace of the MAM version the elements are created for. Used by {@link MamManager}.
 */
public interface MamElementFactory {

    String getSupportedMamNamespace();

    MamElements.MamResultExtension newResultExtension(String queryId, String id, Forwarded<Message> forwarded);

    default MamFinIQ newFinIQ(String queryId, RSMSet rsmSet, boolean complete, boolean stable) {
        return new MamFinIQ(getSupportedMamNamespace(), queryId, rsmSet, complete, stable);
    }

    default MamPrefsIQ newPrefsIQ() {
        return new MamPrefsIQ(getSupportedMamNamespace());
    }

    default MamPrefsIQ newPrefsIQ(List<Jid> alwaysJids, List<Jid> neverJids, MamPrefsIQ.DefaultBehavior defaultBehavior) {
        return new MamPrefsIQ(getSupportedMamNamespace(), alwaysJids, neverJids, defaultBehavior);
    }

    default MamQueryIQ newQueryIQ(String queryId, String node, DataForm dataForm) {
        return new MamQueryIQ(getSupportedMamNamespace(), queryId, node, dataForm);
    }

}
