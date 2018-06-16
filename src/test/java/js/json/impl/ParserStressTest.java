package js.json.impl;

import java.io.StringReader;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Ignore;

import js.lang.GType;
import junit.framework.TestCase;

@Ignore
public class ParserStressTest extends TestCase
{
  private final static int THREADS_COUNT = 10000;
  private final static int TESTS_COUNT = 1000000;

  public void testParsePerson() throws Throwable
  {
    for(int i = 0; i < TESTS_COUNT; ++i) {
      personParsing();
    }
  }

  public void testConcurentParsePerson() throws InterruptedException
  {
    Thread[] threads = new Thread[THREADS_COUNT];

    final List<String> errorMessages = new ArrayList<String>();

    for(int i = 0; i < THREADS_COUNT; ++i) {
      threads[i] = new Thread(new Runnable()
      {
        @Override
        public void run()
        {
          try {
            for(int i = 0; i < 1000; ++i) {
              personParsing();
            }
          }
          catch(Throwable e) {
            String message = e.getMessage();
            errorMessages.add(message);
            TestCase.fail("Error on test: " + message);
          }
        }
      });
      threads[i].start();
    }

    for(Thread thread : threads) {
      thread.join();
    }

    for(String message : errorMessages) {
      System.out.println(message);
    }
    assertEquals(0, errorMessages.size());
  }

  public void testParseArguments() throws Throwable
  {
    for(int i = 0; i < TESTS_COUNT; ++i) {
      arrayParsing();
    }
  }

  public void testConcurentParseArguments() throws InterruptedException
  {
    Thread[] threads = new Thread[THREADS_COUNT];

    final List<String> errorMessages = new ArrayList<String>();

    for(int i = 0; i < THREADS_COUNT; ++i) {
      threads[i] = new Thread(new Runnable()
      {
        @Override
        public void run()
        {
          try {
            for(int i = 0; i < 1000; ++i) {
              arrayParsing();
            }
          }
          catch(Throwable e) {
            String message = e.getMessage();
            errorMessages.add(message);
            TestCase.fail("Error on test: " + message);
          }
        }
      });
      threads[i].start();
    }

    for(Thread thread : threads) {
      thread.join();
    }

    for(String message : errorMessages) {
      System.out.println(message);
    }
    assertEquals(0, errorMessages.size());
  }

  private static void personParsing() throws Throwable
  {
    final String json = "{\"name\":\"John Doe\",\"birthday\":\"1964-03-15T14:30:00Z\",\"age\":46}";

    StringReader reader = new StringReader(json);
    Parser parser = new Parser();
    Person person = parser.parse(reader, Person.class);

    // do not reuse date format; is not thread safe
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    assertNotNull(person);
    assertEquals("Invalid person name.", "John Doe", person.name);
    assertEquals("Invalid person birthday.", "1964-03-15T16:30:00Z", df.format(person.birthday));
    assertEquals("Invalid person age.", 46, person.age);
  }

  @SuppressWarnings("unchecked")
  private static void arrayParsing() throws Throwable
  {
    final String json = "[\"string\",{\"name\":\"John Doe\",\"age\":46},[\"item1\",\"item2\"],\"1964-03-15T14:30:00Z\"]";
    final Type[] formalTypes = new Type[]
    {
        String.class, Person.class, new GType(List.class, String.class), Date.class
    };

    StringReader reader = new StringReader(json);
    Parser parser = new Parser();
    Object[] arguments = parser.parse(reader, formalTypes);

    assertNotNull(arguments);
    assertEquals(4, arguments.length);
    assertEquals("string", arguments[0]);
    assertEquals("John Doe", ((Person)arguments[1]).name);
    assertEquals(46, ((Person)arguments[1]).age);
    assertEquals("item1", ((List<String>)arguments[2]).get(0));
    assertEquals("item2", ((List<String>)arguments[2]).get(1));

    // do not reuse date format; is not thread safe
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    assertEquals("Invalid person birthday.", "1964-03-15T16:30:00Z", df.format(arguments[3]));
  }

  private static class Person
  {
    String name;
    Date birthday;
    int age;
  }
}
