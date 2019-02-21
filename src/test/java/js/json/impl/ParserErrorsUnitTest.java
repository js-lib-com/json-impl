package js.json.impl;

import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.List;

import js.json.impl.ErrorReporter;
import js.json.impl.JsonParserException;
import js.json.impl.Parser;
import js.lang.GType;
import junit.framework.TestCase;

public class ParserErrorsUnitTest extends TestCase
{
  public void testMissingBooleanField() throws Throwable
  {
    String json = "{\"name\":\"John Doe\",\"fakeBoolean\":false}";
    Person person = exercise(json, Person.class);
    assertNotNull(person);
    assertEquals("John Doe", person.name);
  }

  public void testMissingObjectField() throws Throwable
  {
    String json = "{\"name\":\"John Doe\",\"fakeObject\":{\"name\":\"fakeName\"}}";
    Person person = exercise(json, Person.class);
    assertNotNull(person);
    assertEquals("John Doe", person.name);
  }

  public void testMissingArrayField() throws Throwable
  {
    String json = "{\"name\":\"John Doe\",\"fakeArray\":[\"item\"]}";
    Person person = exercise(json, Person.class);
    assertNotNull(person);
    assertEquals("John Doe", person.name);
  }

  public void testEmptyStringsList() throws Throwable
  {
    String json = "[]";
    List<String> strings = exercise(json, new GType(List.class, String.class));
    assertNotNull(strings);
    assertTrue(strings.isEmpty());
  }

  public void testEmptyArgumentsList() throws Throwable
  {
    String json = "[]";
    Type[] formalTypes = new Type[]
    {
        String.class, String.class
    };
    Object[] arguments = exercise(json, formalTypes);
    assertNotNull(arguments);
    assertEquals(2, arguments.length);
    assertNull(arguments[0]);
    assertNull(arguments[1]);
  }

  public void testInvalidStringValue() throws Throwable
  {
    String json = "{\"name\":John Doe}";
    Exception exception = null;
    try {
      exercise(json, Person.class);
    }
    catch(JsonParserException e) {
      exception = e;
    }
    assertNotNull(exception);
  }

  public void testJsonParserExceptionWithErrorReporter() throws Throwable
  {
    String sample = "Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.";
    ErrorReporter errorReporter = ErrorReporter.getInstance();
    errorReporter.reset();
    for(int i = 0; i < sample.length(); i++) {
      errorReporter.store(sample.charAt(i));
    }

    JsonParserException exception = new JsonParserException("This is a fake error.", new Object[] {});
    assertEquals("JSON parser error on char index #123 near ...d do eiusmod tempor incididunt ut labore et dolore magna aliqua.. This is a fake error.",
        exception.getMessage());
  }

  public void testMissingColon() throws Throwable
  {
    String json = "{\"name\":\"John Doe\",\"picture\":\"picture.png\",\"fakeArray\",[\"item\"]}";
    try {
      exercise(json, Person.class);
      fail("Invalid JSON source should rise exception.");
    }
    catch(JsonParserException e) {
      assertEquals(
          "JSON parser error on char index #54 near ...{\"name\":\"John Doe\",\"picture\":\"picture.png\",\"fakeArray\",. Expected COLON but got |COMMA|.",
          e.getMessage());
    }
  }

  public void testMissingRightBrace() throws Throwable
  {
    String json = "{\"name\":\"John Doe\",\"picture\":\"picture.png\",\"fakeArray\":[\"item\"]";
    try {
      exercise(json, Person.class);
      fail("Invalid JSON source should rise exception.");
    }
    catch(JsonParserException e) {
      assertEquals(
          "JSON parser error on char index #62 near ...{\"name\":\"John Doe\",\"picture\":\"picture.png\",\"fakeArray\":[\"item\"]. Expected RIGHT_BRACE but got EOF. Maybe missing comma.",
          e.getMessage());
    }
  }

  public void testMissingRightSqaure() throws Throwable
  {
    String json = "{\"name\":\"John Doe\",\"picture\":\"picture.png\",\"fakeArray\":[\"item\"}";
    try {
      exercise(json, Person.class);
      fail("Invalid JSON source should rise exception.");
    }
    catch(JsonParserException e) {
      assertEquals(
          "JSON parser error on char index #62 near ...{\"name\":\"John Doe\",\"picture\":\"picture.png\",\"fakeArray\":[\"item\"}. Expected RIGHT_SQUARE but got RIGHT_BRACE. Maybe missing comma.",
          e.getMessage());
    }
  }

  public void testBadValue() throws Throwable
  {
    String json = "{\"name\":\"John Doe\",\"picture\":}\"picture.png\",\"fakeArray\":[\"item\"}";
    try {
      exercise(json, Person.class);
      fail("Invalid JSON source should rise exception.");
    }
    catch(JsonParserException e) {
      assertEquals(
          "JSON parser error on char index #29 near ...{\"name\":\"John Doe\",\"picture\":}. Expect VALUE, LEFT_BRACE or LEFT_SQUARE but got RIGHT_BRACE.",
          e.getMessage());
    }
  }

  public void testBadItem() throws Throwable
  {
    String json = "{\"name\":\"John Doe\",\"picture\":\"picture.png\",\"fakeArray\":[}\"item\"]}";
    try {
      exercise(json, Person.class);
      fail("Invalid JSON source should rise exception.");
    }
    catch(JsonParserException e) {
      assertEquals(
          "JSON parser error on char index #56 near ...{\"name\":\"John Doe\",\"picture\":\"picture.png\",\"fakeArray\":[}. Expect ITEM, LEFT_BRACE or LEFT_SQUARE but got RIGHT_BRACE.",
          e.getMessage());
    }
  }

  public void testBadStartToken() throws Throwable
  {
    String json = ",{\"name\":\"John Doe\",\"picture\":\"picture.png\",\"fakeArray\":[}\"item\"]}";
    try {
      exercise(json, Person.class);
      fail("Invalid JSON source should rise exception.");
    }
    catch(JsonParserException e) {
      assertEquals("JSON parser error on char index #0 near ...,. Invalid start token COMMA.", e.getMessage());
    }
  }

  public void testNullBooleanField() throws Throwable
  {
    String json = "{\"value\":null}";
    Flag flag = exercise(json, Flag.class);
    assertNotNull(flag);
    assertFalse(flag.value);
  }

  public void testNullIntegerField() throws Throwable
  {
    String json = "{\"value\":null}";
    Counter counter = exercise(json, Counter.class);
    assertNotNull(counter);
    assertEquals(0, counter.value);
  }

  private static <T> T exercise(String json, Type type) throws Throwable
  {
    StringReader reader = new StringReader(json);
    return new Parser().parse(reader, type);
  }

  private static Object[] exercise(String json, Type[] types) throws Throwable
  {
    StringReader reader = new StringReader(json);
    return new Parser().parse(reader, types);
  }

  private static class Person
  {
    String name;
  }

  private static class Flag
  {
    boolean value;
  }

  private static class Counter
  {
    int value;
  }
}
