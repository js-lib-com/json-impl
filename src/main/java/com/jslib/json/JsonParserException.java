package com.jslib.json;

import java.lang.reflect.InvocationTargetException;

import com.jslib.api.json.JsonException;

/**
 * Not checked exception thrown when JSON parsing process fails for some reasons.
 * 
 * @author Iulian Rotaru
 */
public class JsonParserException extends JsonException {
	/** Java serialization version. */
	private static final long serialVersionUID = 8175755582232053470L;

	/**
	 * Create parser exception with formatted message.
	 * 
	 * @param message exception formatted message,
	 * @param args optional formatted arguments.
	 */
	public JsonParserException(String message, Object... args) {
		super(buildMessage(message, args));
	}

	/**
	 * Create parser exception with error reporter and root cause throwable.
	 * 
	 * @param t root cause throwable.
	 */
	public JsonParserException(Throwable t) {
		super(buildMessage(t));
	}

	/**
	 * Build exception message from error reporter information and root cause throwable.
	 * 
	 * @param t root cause throwable.
	 * @return compiled exception message.
	 */
	private static String buildMessage(Throwable t) {
		if (t instanceof RuntimeException && t.getCause() != null) {
			t = t.getCause();
		}
		if (t instanceof InvocationTargetException) {
			t = ((InvocationTargetException) t).getTargetException();
		}
		String message = t.getMessage();
		if (message == null) {
			message = t.getClass().getCanonicalName();
		}
		return buildMessage(message);
	}

	/**
	 * Build exception message from error reporter information and formatted user defined message.
	 * 
	 * @param message user defined message,
	 * @param args optional arguments if user defined message is formatted.
	 * @return compiled exception message.
	 */
	private static String buildMessage(String message, Object[] args) {
		return buildMessage(String.format(message, args));
	}

	/**
	 * Compile message from error reporter information and given exception message.
	 * 
	 * @param message exception message.
	 * @return compiled message.
	 */
	private static String buildMessage(String message) {
		ErrorReporter errorReporter = ErrorReporter.getInstance();
		StringBuilder sb = new StringBuilder();
		sb.append("JSON parser error on char index #");
		sb.append(errorReporter.charIndex());
		sb.append(" near ...");
		sb.append(errorReporter.streamSample());
		sb.append(". ");
		sb.append(message);
		return sb.toString();
	}
}
