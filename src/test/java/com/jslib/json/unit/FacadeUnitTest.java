package com.jslib.json.unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.List;

import com.jslib.api.json.Json;
import com.jslib.json.JsonImpl;
import com.jslib.lang.GType;

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
    assertThat(page, notNullValue());
    assertThat("index.htm", equalTo(page.name));
    assertThat(State.ACTIVE, equalTo(page.state));
  }

  public void testParseHomogeneousArray() throws IOException
  {
    final String jsonArray = "[{name:\"index.htm\",state:\"ACTIVE\"},{name:\"verboten.htm\",state:\"BANNED\"}]";
    StringReader reader = new StringReader(jsonArray);
    Page[] pages = json.parse(reader, Page[].class);

    assertThat(pages, notNullValue());
    assertThat(pages.length, equalTo(2));
    assertThat(pages[0].name, equalTo("index.htm"));
    assertThat(pages[0].state, equalTo(State.ACTIVE));
    assertThat(pages[1].name, equalTo("verboten.htm"));
    assertThat(pages[1].state, equalTo(State.BANNED));
  }

  public void testParseMixedObjects() throws IOException
  {
    StringReader reader = new StringReader("[123.45,{name:\"index.htm\",state:\"ACTIVE\"},null,false]");
    Object[] parameters = json.parse(reader, new Type[]
    {
        double.class, Page.class, String.class, boolean.class
    });

    assertThat(parameters, notNullValue());
    assertThat(parameters.length, equalTo(4));

    assertTrue(parameters[0] instanceof Double);
    assertTrue(parameters[1] instanceof Page);
    assertTrue(parameters[3] instanceof Boolean);

    assertThat(parameters[0], equalTo(123.45));
    assertThat(((Page)parameters[1]).name, equalTo("index.htm"));
    assertThat(((Page)parameters[1]).state, equalTo(State.ACTIVE));
    assertThat(parameters[2], nullValue());
    assertFalse((Boolean)parameters[3]);
  }

  public void testStringify() throws IOException
  {
    Page page = new Page();
    page.name = "index.htm";
    page.state = State.ACTIVE;
    StringWriter writer = new StringWriter();
    json.stringify(writer, page);
    assertThat(writer.toString(), equalTo("{\"name\":\"index.htm\",\"state\":\"ACTIVE\"}"));
  }

  public void testParseObjectFromString()
  {
    final String jsonObject = "{\"name\":\"index.htm\",\"state\":\"ACTIVE\"}";
    Page page = json.parse(jsonObject, Page.class);

    assertThat(page, notNullValue());
    assertThat(page.name, equalTo("index.htm"));
    assertThat(page.state, equalTo(State.ACTIVE));
  }

  public void testParsePersonListFromString()
  {
    final String jsonArray = "[{\"name\":\"index.htm\",\"state\":\"ACTIVE\"}, {\"name\":\"verboten.htm\",\"state\":\"BANNED\"}]";
    List<Page> pages = json.parse(jsonArray, new GType(List.class, Page.class));

    assertThat(pages, notNullValue());
    assertThat(pages, hasSize(2));

    assertThat(pages.get(0).name, equalTo("index.htm"));
    assertThat(pages.get(0).state, equalTo(State.ACTIVE));
    assertThat(pages.get(1).name, equalTo("verboten.htm"));
    assertThat(pages.get(1).state, equalTo(State.BANNED));
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
