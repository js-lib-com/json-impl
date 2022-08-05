package com.jslib.json;

/**
 * Store information about JSON parsing process like index of character where error occurred.
 * 
 * @author Iulian Rotaru
 */
public final class ErrorReporter {
	/** Thread storage for error reporter instances. */
	private static ThreadLocal<ErrorReporter> tls = new ThreadLocal<ErrorReporter>();

	/**
	 * Get error reporter instance. Every thread keeps its own error reporter instance created lazily.
	 * 
	 * @return thread error reporter instance.
	 */
	public static synchronized ErrorReporter getInstance() {
		ErrorReporter instance = tls.get();
		if (instance == null) {
			instance = new ErrorReporter();
			tls.set(instance);
		}
		return instance;
	}

	/** Circular buffer size. */
	private static final int BUFFER_SIZE = 64;

	/** Circular buffer that store a sample from JSON characters stream. */
	private char[] buffer = new char[BUFFER_SIZE];

	/** Circular buffer index. */
	private int index;

	/**
	 * Reset error reporter internal state. Since error reporter instance is global per thread needs to be reseted every time a
	 * new parsing process starts.
	 */
	public void reset() {
		index = 0;
	}

	/**
	 * Update error reporter circular buffer with character from JSON characters stream. This method is called by
	 * {@link CharReader#next()}.
	 * 
	 * @param c character from JSON stream.
	 */
	public void store(char c) {
		buffer[index % BUFFER_SIZE] = c;
		index++;
	}

	/**
	 * Get character index where exception occurred.
	 * 
	 * @return exception character index.
	 */
	public int charIndex() {
		return index - 1;
	}

	/**
	 * Get a sample from JSON characters stream.
	 * 
	 * @return stream sample.
	 */
	public String streamSample() {
		StringBuilder sb = new StringBuilder();
		if (index <= BUFFER_SIZE) {
			for (int i = 0; i < index; i++) {
				sb.append(buffer[i]);
			}
		} else {
			int bufferIndex = index % BUFFER_SIZE;
			for (int i = bufferIndex; i < BUFFER_SIZE; i++) {
				sb.append(buffer[i]);
			}
			for (int i = 0; i < bufferIndex; i++) {
				sb.append(buffer[i]);
			}
		}
		return sb.toString();
	}
}
