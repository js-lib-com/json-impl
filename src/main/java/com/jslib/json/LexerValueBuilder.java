package com.jslib.json;

/**
 * Lexer value builder with escape and unicode processing. This helper class is used by {@link Lexer} to collect values as Java
 * strings. Is parser job to convert strings into instances.
 * 
 * @author Iulian Rotaru
 */
public final class LexerValueBuilder {
	/** Internal string builder. */
	private StringBuilder builder = new StringBuilder();

	/** Unicode value builder. */
	private StringBuilder unicode = new StringBuilder(4);

	/** Value builder state machine. */
	private LexerValueBuilder.State state = State.CHAR;

	/**
	 * Append character to this string value builder.
	 * 
	 * @param c character to add.
	 */
	public boolean append(char c) {
		switch (state) {
		case CHAR:
		    if( c == '"') {
		      return false;
		    }
			if (c == '\\') {
				state = State.ESCAPE;
				break;
			}
			builder.append(c);
			break;

		case ESCAPE:
			switch (c) {
			case 'u':
				state = State.UNICODE;
				unicode.setLength(0);
				return true;

			case '"':
				builder.append('"');
				break;

			case '\\':
				builder.append('\\');
				break;

			case '/':
				builder.append('/');
				break;

			case 'b':
				builder.append('\b');
				break;

			case 'f':
				builder.append('\f');
				break;

			case 'n':
				builder.append('\n');
				break;

			case 'r':
				builder.append('\r');
				break;

			case 't':
				builder.append('\t');
				break;

			default:
				throw new JsonParserException("Bad JSON syntax. Invalid escape |%s|", c);
			}
			state = State.CHAR;
			break;

		case UNICODE:
			unicode.append(c);
			if (unicode.length() == 4) {
				builder.append((char) Integer.parseInt(unicode.toString(), 16));
				state = State.CHAR;
			}
			break;

		default:
			throw new IllegalStateException();
		}
		return true;
	}

	public void clear() {
		builder.setLength(0);
	}

	@Override
	public String toString() {
		String s = builder.toString();
		return s.equals("null") ? null : s;
	}

	/**
	 * State machine for lexer value builder .
	 * 
	 * @author Iulian Rotaru
	 */
	private static enum State {
		NONE, CHAR, ESCAPE, UNICODE
	}
}