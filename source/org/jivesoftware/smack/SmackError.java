package org.jivesoftware.smack;

public enum SmackError {
    NO_RESPONSE_FROM_SERVER("No response from server.");
    
    private String message;
    
    private SmackError(String errMessage) {
        message = errMessage;
    }
    
    public String getErrorMessage() {
        return message;
    }
    
    public static SmackError getErrorCode(String message) {
        for (SmackError code : values()) {
            if (code.message.equals(message)) {
                return code;
            }
        }
        return null;
    }
}
