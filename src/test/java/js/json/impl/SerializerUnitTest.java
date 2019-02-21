package js.json.impl;

import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import js.json.impl.Serializer;
import js.lang.OrdinalEnum;
import junit.framework.TestCase;

@SuppressWarnings("unused")
public class SerializerUnitTest extends TestCase
{
  public void testNull() throws Throwable
  {
    assertEquals("null", exercise(null));
  }

  public void testString() throws Throwable
  {
    assertEquals("\"John Doe\"", exercise("John Doe"));
  }

  public void testBoolean() throws Throwable
  {
    assertEquals("true", exercise(true));
    assertEquals("false", exercise(false));
  }

  public void testNumber() throws Throwable
  {
    assertEquals("123.45", exercise(123.45));
  }

  public void testEnum() throws Throwable
  {
    assertEquals("\"ALIVE\"", exercise(State.ALIVE));
  }

  public void testOrdinalEnum() throws Throwable
  {
    assertEquals("1", exercise(OrdinalState.ALIVE));
  }

  public void testDate() throws Throwable
  {
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    df.setTimeZone(TimeZone.getTimeZone("UTC"));
    assertEquals("\"1964-03-15T14:30:00Z\"", exercise(df.parse("1964-03-15 14:30:00.000")));
  }

  public void testFlatObject() throws Throwable
  {
    Person person = new Person("John Doe");
    assertEquals("{\"name\":\"John Doe\",\"state\":\"ALIVE\"}", exercise(person));
  }

  public void testFlatObjectWithInboundClass() throws Throwable
  {
    Person person = new Person("John Doe");
    assertEquals("{\"class\":\"js.json.impl.SerializerUnitTest$Person\",\"name\":\"John Doe\",\"state\":\"ALIVE\"}", exerciseWithClass(person));
  }

  public void testArrayOfStrings() throws Throwable
  {
    String[] strings = new String[]
    {
        "John Doe", "picture.png"
    };
    assertEquals("[\"John Doe\",\"picture.png\"]", exercise(strings));
  }

  public void testListOfPersons() throws Throwable
  {
    List<Person> persons = new ArrayList<Person>();
    assertEquals("[]", exercise(persons));
    persons.add(new Person("John Doe"));
    assertEquals("[{\"name\":\"John Doe\",\"state\":\"ALIVE\"}]", exercise(persons));
  }

  public void testMapOfStrings() throws Throwable
  {
    Map<String, String> map = new HashMap<String, String>();
    map.put("name", "John Doe");
    map.put("picture", "picture.png");
    String json = exercise(map);
    assertTrue(json.contains("\"name\":\"John Doe\""));
    assertTrue(json.contains("\"picture\":\"picture.png\""));
  }

  public void testInheritance() throws Throwable
  {
    Child child = new Child("Anonymous", "John Doe");
    assertEquals("{\"parent\":\"Anonymous\",\"name\":\"John Doe\",\"state\":\"ALIVE\"}", exercise(child));
  }

  public void testDeepInheritance() throws Throwable
  {
    Nephew nephew = new Nephew("Anonymous", "John Doe");
    assertEquals("{\"birthPlace\":\"Earth, Romania, Iasi\",\"parent\":\"Anonymous\",\"name\":\"John Doe\",\"state\":\"ALIVE\"}", exercise(nephew));
  }

  public void testInheritanceWithEmptySubclass() throws Throwable
  {
    EmptyChild child = new EmptyChild("John Doe");
    assertEquals("{\"name\":\"John Doe\",\"state\":\"ALIVE\"}", exercise(child));
  }

  /**
   * Anonymous inner class has synthetic 'this$0' field for outer class. This should not be included into serialized
   * object.
   */
  public void testSyntheticField() throws Throwable
  {
    class Message
    {
      private String text = "message text";
    }
    assertNotNull(Message.class.getDeclaredField("this$0"));
    assertEquals("{\"text\":\"message text\"}", exercise(new Message()));
  }

  private static String exercise(Object value) throws Throwable
  {
    Serializer serializer = new Serializer();
    StringWriter writer = new StringWriter();
    serializer.serialize(writer, value);
    return writer.toString();
  }

  private static String exerciseWithClass(Object value) throws Throwable
  {
    Serializer serializer = new Serializer(true);
    StringWriter writer = new StringWriter();
    serializer.serialize(writer, value);
    return writer.toString();
  }

  // ----------------------------------------------------------------------------------------------
  // FIXTURE

  private static class Person
  {
    static String species = "homo sapiens";

    transient int id;
    String name;
    State state;

    Person(String name)
    {
      this.name = name;
      this.state = State.ALIVE;
    }
  }

  private static class Child extends Person
  {
    static int milenia = 2000;

    String parent;

    Child(String parent, String name)
    {
      super(name);
      this.parent = parent;
    }
  }

  private static class EmptyChild extends Person
  {
    EmptyChild(String name)
    {
      super(name);
    }
  }

  private static class Nephew extends Child
  {
    String birthPlace = "Earth, Romania, Iasi";

    Nephew(String parent, String name)
    {
      super(parent, name);
    }
  }

  private static enum State
  {
    NONE, ALIVE, DEAD
  }

  private static enum OrdinalState implements OrdinalEnum
  {
    NONE, ALIVE, DEAD
  }
}
