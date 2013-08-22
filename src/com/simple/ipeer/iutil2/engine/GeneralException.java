package com.simple.ipeer.iutil2.engine;

public class GeneralException extends Exception {

	private static final long serialVersionUID = -4321164457362940502L;

	public GeneralException() { }

	public GeneralException(String message) {
		super(message);
	}

	public GeneralException(Throwable cause) {
		super(cause);
	}

	public GeneralException(String message, Throwable cause) {
		super(message, cause);
	}

	public GeneralException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
