package org.jivesoftware.smackx.mam.element;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.forward.packet.Forwarded;

import javax.xml.namespace.QName;

public class Mam1ElementFactory implements MamElementFactory {

    public static final String MAM1_NAMESPACE = MamElements.MAM1_NAMESPACE;

    @Override
    public String getSupportedMamNamespace() {
        return MAM1_NAMESPACE;
    }

    @Override
    public MamElements.MamResultExtension newResultExtension(String queryId, String id, Forwarded<Message> forwarded) {
        return new Mam1ResultExtension(queryId, id, forwarded);
    }

    public static class Mam1ResultExtension extends MamElements.MamResultExtension {

        public static final QName QNAME = new QName(MAM1_NAMESPACE, ELEMENT);

        /**
         * MAM result extension constructor.
         *
         * @param queryId    TODO javadoc me please
         * @param id         TODO javadoc me please
         * @param forwarded  TODO javadoc me please
         */
        public Mam1ResultExtension(String queryId, String id, Forwarded<Message> forwarded) {
            super(MAM1_NAMESPACE, queryId, id, forwarded);
        }
    }
}
