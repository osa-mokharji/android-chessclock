package com.chess.backend.exceptions;

import com.chess.utilities.MonitorDataHelper;

/**
 * Base exception class for chess android application
 * <p/>
 * Created by electrolobzik (electrolobzik@gmail.com) on 01/03/2014.
 */
public class ChessException extends Exception {

	public ChessException(String detailMessage) {
		super(detailMessage);
	}

	public ChessException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	/**
	 * Throws exception as runtime
	 */
	public void throwAsRuntime() {

		throw new RuntimeException(this);
	}

	/**
	 * Logs handled exception
	 */
	public void logHandled() {

		MonitorDataHelper.logException(this);
	}
}