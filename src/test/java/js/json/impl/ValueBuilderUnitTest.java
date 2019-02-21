package js.json.impl;

import js.json.impl.LexerValueBuilder;
import junit.framework.TestCase;

public class ValueBuilderUnitTest extends TestCase
{
  public void testConformance() throws Throwable
  {
    assertEquals("\"John Doe\"", exercise("\\\"John Doe\\\""));
    assertEquals("123.45", exercise("123.45"));
    assertEquals("-123.45", exercise("-123.45"));
    assertEquals("123.45e2", exercise("123.45e2"));
    assertEquals("123.45e+2", exercise("123.45e+2"));
    assertEquals("123.45e-2", exercise("123.45e-2"));
    assertEquals("123.45E2", exercise("123.45E2"));
    assertEquals("true", exercise("true"));
    assertEquals("false", exercise("false"));
  }

  public void testNull() throws Throwable
  {
    assertNull(exercise("null"));
    assertEquals("\"null\"", exercise("\\\"null\\\""));
  }

  public void testEscapeCharacters() throws Throwable
  {
    assertEquals("\"", exercise("\\\""));
    assertEquals("/", exercise("\\/"));
    assertEquals("\b", exercise("\\b"));
    assertEquals("\f", exercise("\\f"));
    assertEquals("\n", exercise("\\n"));
    assertEquals("\r", exercise("\\r"));
    assertEquals("\t", exercise("\\t"));
    assertEquals("\"/\b\f\n\r\t", exercise("\\\"\\/\\b\\f\\n\\r\\t"));
  }

  public void testEscapeUnicode() throws Throwable
  {
    assertEquals("©", exercise("\\u00A9"));
  }

  public void testComplexValue() throws Throwable
  {
    assertEquals("123.45E2\"/\b\f\n\r\t©\"John Doe\",true,false", exercise("123.45E2\\\"\\/\\b\\f\\n\\r\\t\\u00A9\\\"John Doe\\\",true,false"));
  }

  private static String exercise(String value) throws Throwable
  {
    LexerValueBuilder builder = new LexerValueBuilder();
    for(int i = 0; i < value.length(); i++) {
      builder.append(value.charAt(i));
    }
    return builder.toString();
  }
}
