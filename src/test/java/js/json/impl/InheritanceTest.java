package js.json.impl;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import js.json.Json;
import js.util.Classes;

public class InheritanceTest
{
  private static Json json;

  @BeforeClass
  public static void beforeClass()
  {
    json = Classes.loadService(Json.class);
  }

  @Test
  public void serialization()
  {
    GrandParent grandpa = new GrandParent();
    grandpa.name = "John Doe";
    grandpa.age = 78;

    grandpa.children = new ArrayList<>();
    grandpa.children.add(new Child("Baby Boy Doe"));
    grandpa.children.add(new Child("Baby Girl Doe"));

    String value = json.stringify(grandpa);
    assertThat(value, notNullValue());
    assertThat(value, equalTo("{\"age\":78,\"name\":\"John Doe\",\"children\":[{\"name\":\"Baby Boy Doe\"},{\"name\":\"Baby Girl Doe\"}]}"));
  }

  @Test
  public void deserialization() throws Exception
  {
    Reader reader = new StringReader("{\"age\":78,\"name\":\"John Doe\",\"children\":[{\"name\":\"Baby Boy Doe\"},{\"name\":\"Baby Girl Doe\"}]}");
    GrandParent grandpa = json.parse(reader, GrandParent.class);

    assertThat(grandpa, notNullValue());
    assertThat(grandpa.name, equalTo("John Doe"));
    assertThat(grandpa.age, equalTo(78));

    assertThat(grandpa.children, notNullValue());
    assertThat(grandpa.children, hasSize(2));
    assertThat(grandpa.children.get(0).name, equalTo("Baby Boy Doe"));
    assertThat(grandpa.children.get(1).name, equalTo("Baby Girl Doe"));
  }

  private static class Child
  {
    private String name;

    @SuppressWarnings("unused")
    public Child()
    {
    }

    public Child(String name)
    {
      this.name = name;
    }
  }

  private static abstract class Parent
  {
    protected String name;
    protected List<Child> children;
  }

  private static class GrandParent extends Parent
  {
    private int age;
  }
}
