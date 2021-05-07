package org.jivesoftware.smackx.mam.element;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.forward.packet.Forwarded;

import javax.xml.namespace.QName;

public class Mam2ElementFactory implements MamElementFactory {

    public static final String MAM2_NAMESPACE = MamElements.MAM2_NAMESPACE;

    @Override
    public String getSupportedMamNamespace() {
        return MAM2_NAMESPACE;
    }

    @Override
    public MamElements.MamResultExtension newResultExtension(String queryId, String id, Forwarded<Message> forwarded) {
        return new Mam2ResultExtension(queryId, id, forwarded);
    }

    public static class Mam2ResultExtension extends MamElements.MamResultExtension {

        public static final QName QNAME = new QName(MAM2_NAMESPACE, ELEMENT);

        /**
         * MAM result extension constructor.
         *
         * @param queryId    TODO javadoc me please
         * @param id         TODO javadoc me please
         * @param forwarded  TODO javadoc me please
         */
        public Mam2ResultExtension(String queryId, String id, Forwarded<Message> forwarded) {
            super(MAM2_NAMESPACE, queryId, id, forwarded);
        }
    }
}
