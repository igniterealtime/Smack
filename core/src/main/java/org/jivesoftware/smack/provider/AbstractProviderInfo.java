package org.jivesoftware.smack.provider;


abstract class AbstractProviderInfo {
    private String element;
    private String ns;
    private Object provider;
    
    AbstractProviderInfo(String elementName, String namespace, Object iqOrExtProvider) {
        element = elementName;
        ns = namespace;
        provider = iqOrExtProvider;
    }
    
    public String getElementName() {
        return element;
    }

    public String getNamespace() {
        return ns;
    }
    
    Object getProvider() {
        return provider;
    }
}
