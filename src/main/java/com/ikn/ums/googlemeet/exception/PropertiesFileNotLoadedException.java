package com.ikn.ums.googlemeet.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PropertiesFileNotLoadedException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String errorCode;
	private String errorMessage;

    /**
     * Constructs an PropertiesFileNotLoadedException with a custom error code and message.
     *
     * @param errorCode the error code associated with the exception.
     * @param errorMessage the detailed error message.
     */
    public PropertiesFileNotLoadedException(String errorCode, String errorMessage) {
        super(String.format("ErrorCode: %s, ErrorMessage: %s", errorCode, errorMessage));
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
    
    /**
     * Default constructor with a generic message.
     */
	public PropertiesFileNotLoadedException() {
		 super("An unspecified properties file not loaded error occurred.");
	}
    
}
