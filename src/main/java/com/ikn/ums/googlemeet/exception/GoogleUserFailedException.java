package com.ikn.ums.googlemeet.exception;

public class GoogleUserFailedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String errorCode;
    private final String errorMessage;

    public GoogleUserFailedException(String errorCode, String errorMessage) {
        super(errorMessage);               
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public GoogleUserFailedException(String errorCode, String errorMessage, Throwable cause) {
        super(errorMessage, cause);      
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
