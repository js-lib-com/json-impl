package js.json.impl;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.List;

import js.json.Json;
import js.lang.GType;
import junit.framework.TestCase;

public class FacadeUnitTest extends TestCase
{
  private Json json;

  @Override
  protected void setUp() throws Exception
  {
    json = new JsonImpl();
  }

  public void testParseSingleObject() throws IOException
  {
    StringReader reader = new StringReader("{name:\"index.htm\",state:\"ACTIVE\"}");
    Page page = json.parse(reader, Page.class);
    assertNotNull(page);
    assertEquals("index.htm", page.name);
    assertEquals(State.ACTIVE, page.state);
  }

  public void testParseHomogeneousArray() throws IOException
  {
    final String jsonArray = "[{name:\"index.htm\",state:\"ACTIVE\"},{name:\"verboten.htm\",state:\"BANNED\"}]";
    StringReader reader = new StringReader(jsonArray);
    Page[] pages = json.parse(reader, Page[].class);

    assertNotNull(pages);
    assertEquals(2, pages.length);
    assertEquals("index.htm", pages[0].name);
    assertEquals(State.ACTIVE, pages[0].state);
    assertEquals("verboten.htm", pages[1].name);
    assertEquals(State.BANNED, pages[1].state);
  }

  public void testParseMixedObjects() throws IOException
  {
    StringReader reader = new StringReader("[123.45,{name:\"index.htm\",state:\"ACTIVE\"},null,false]");
    Object[] parameters = json.parse(reader, new Type[]
    {
        double.class, Page.class, String.class, boolean.class
    });

    assertNotNull(parameters);
    assertEquals(4, parameters.length);

    assertTrue(parameters[0] instanceof Double);
    assertTrue(parameters[1] instanceof Page);
    assertTrue(parameters[3] instanceof Boolean);

    assertEquals(123.45, parameters[0]);
    assertEquals("index.htm", ((Page)parameters[1]).name);
    assertEquals(State.ACTIVE, ((Page)parameters[1]).state);
    assertNull(parameters[2]);
    assertFalse((Boolean)parameters[3]);
  }

  public void testStringify() throws IOException
  {
    Page page = new Page();
    page.name = "index.htm";
    page.state = State.ACTIVE;
    StringWriter writer = new StringWriter();
    json.stringify(writer, page);
    assertEquals("{\"name\":\"index.htm\",\"state\":\"ACTIVE\"}", writer.toString());
  }

  public void testStringifyObject() throws IOException
  {
    Page page = new Page();
    page.name = "index.htm";
    page.state = State.ACTIVE;
    StringWriter writer = new StringWriter();
    json.stringifyObject(writer, page);

    assertEquals("{\"class\":\"js.json.impl.FacadeUnitTest$Page\",\"name\":\"index.htm\",\"state\":\"ACTIVE\"}", writer.toString());
  }

  public void testParseObjectFromString()
  {
    final String jsonObject = "{\"name\":\"index.htm\",\"state\":\"ACTIVE\"}";
    Page page = json.parse(jsonObject, Page.class);

    assertNotNull(page);
    assertEquals("index.htm", page.name);
    assertEquals(State.ACTIVE, page.state);
  }

  public void testParsePersonListFromString()
  {
    final String jsonArray = "[{\"name\":\"index.htm\",\"state\":\"ACTIVE\"}, {\"name\":\"verboten.htm\",\"state\":\"BANNED\"}]";
    List<Page> pages = json.parse(jsonArray, new GType(List.class, Page.class));

    assertNotNull(pages);
    assertEquals(2, pages.size());
    assertEquals("index.htm", pages.get(0).name);
    assertEquals(State.ACTIVE, pages.get(0).state);
    assertEquals("verboten.htm", pages.get(1).name);
    assertEquals(State.BANNED, pages.get(1).state);
  }

  private static enum State
  {
    NONE, ACTIVE, BANNED
  }

  private static class Page
  {
    String name;
    State state;
  }
}
