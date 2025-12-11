package com.ikn.ums.googlemeet.exception;

public class UsersNotFoundException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//instance variables
	private String code;
	private String message;
	
	//constructors
	public UsersNotFoundException() {
		super();
	}
	
	public UsersNotFoundException(String code,String message) {
		super();
		this.code = code;
		this.message = message;
	}
	
	//setters and getters
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
