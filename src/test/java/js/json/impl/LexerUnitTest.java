package js.json.impl;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import js.util.Strings;
import junit.framework.TestCase;

public class LexerUnitTest extends TestCase
{

  public void testDelimiters()
  {
    for(String json : new String[]
    {
        "{[:,]}", " \r\n\t{ \r\n\t[ \r\n\t: \r\n\t, \r\n\t] \r\n\t} \r\n\t"
    }) {
      Token[] tokens = exercise(json);

      assertEquals(Token.LEFT_BRACE, tokens[0].ordinal());
      assertEquals(Token.LEFT_SQUARE, tokens[1].ordinal());
      assertEquals(Token.COLON, tokens[2].ordinal());
      assertEquals(Token.COMMA, tokens[3].ordinal());
      assertEquals(Token.RIGHT_SQUARE, tokens[4].ordinal());
      assertEquals(Token.RIGHT_BRACE, tokens[5].ordinal());

      for(Token token : tokens) {
        assertNull(token.value());
      }
    }
  }

  public void testStringValue()
  {
    for(String json : new String[]
    {
        "\"John Doe\"", " \r\n\t\"John Doe\" \r\n\t"
    }) {
      Token[] tokens = exercise(json);

      assertEquals(1, tokens.length);
      assertEquals(Token.VALUE, tokens[0].ordinal());
      assertEquals("John Doe", tokens[0].value());
    }
  }

  public void testEscapedStringValue()
  {
    String json = "\"\\\"\\/\\b\\f\\n\\r\\t\\u00A9\"";
    Token[] tokens = exercise(json);

    assertEquals(1, tokens.length);
    assertEquals(Token.VALUE, tokens[0].ordinal());
    assertEquals("\"/\b\f\n\r\t©", tokens[0].value());
  }

  public void testNumberValue()
  {
    for(String json : new String[]
    {
        "1234.56", " \r\n\t1234.56 \r\n\t"
    }) {
      Token[] tokens = exercise(json);

      assertEquals(1, tokens.length);
      assertEquals(Token.VALUE, tokens[0].ordinal());
      assertEquals("1234.56", tokens[0].value());
    }
  }

  public void testHexadecimalNumberValue()
  {
    for(String json : new String[]
    {
        "0x1234", " \r\n\t0x1234 \r\n\t"
    }) {
      Token[] tokens = exercise(json);

      assertEquals(1, tokens.length);
      assertEquals(Token.VALUE, tokens[0].ordinal());
      assertEquals("0x1234", tokens[0].value());
    }
  }

  public void testBooleanValue()
  {
    for(String json : new String[]
    {
        "true", " \r\n\ttrue \r\n\t"
    }) {
      Token[] tokens = exercise(json);
      assertEquals(1, tokens.length);
      assertEquals(Token.VALUE, tokens[0].ordinal());
      assertEquals("true", tokens[0].value());
    }

    for(String json : new String[]
    {
        "false", " \r\n\tfalse \r\n\t"
    }) {
      Token[] tokens = exercise(json);
      assertEquals(1, tokens.length);
      assertEquals(Token.VALUE, tokens[0].ordinal());
      assertEquals("false", tokens[0].value());
    }
  }

  public void testNullValue()
  {
    for(String json : new String[]
    {
        "null", " \r\n\tnull \r\n\t"
    }) {
      Token[] tokens = exercise(json);

      assertEquals(1, tokens.length);
      assertEquals(Token.VALUE, tokens[0].ordinal());
      assertNull(tokens[0].value());
    }
  }

  public void testFlatObject()
  {
    for(String json : new String[]
    {
        "{\"name\":\"John Doe\",\"picture\":\"picture.png\"}", "{name:\"John Doe\",picture:\"picture.png\"}",
        " \r\n\t{ \r\n\t\"name\" \r\n\t: \r\n\t\"John Doe\" \r\n\t, \r\n\t\"picture\" \r\n\t: \r\n\t\"picture.png\" \r\n\t} \r\n\t",
        " \r\n\t{ \r\n\tname \r\n\t: \r\n\t\"John Doe\" \r\n\t, \r\n\tpicture \r\n\t: \r\n\t\"picture.png\" \r\n\t} \r\n\t"
    }) {
      Token[] tokens = exercise(json);

      assertEquals(9, tokens.length);

      assertEquals(Token.LEFT_BRACE, tokens[0].ordinal());
      assertNull(tokens[0].value());

      assertEquals(Token.NAME, tokens[1].ordinal());
      assertEquals("name", tokens[1].value());

      assertEquals(Token.COLON, tokens[2].ordinal());
      assertNull(tokens[2].value());

      assertEquals(Token.VALUE, tokens[3].ordinal());
      assertEquals("John Doe", tokens[3].value());

      assertEquals(Token.COMMA, tokens[4].ordinal());
      assertNull(tokens[4].value());

      assertEquals(Token.NAME, tokens[5].ordinal());
      assertEquals("picture", tokens[5].value());

      assertEquals(Token.COLON, tokens[6].ordinal());
      assertNull(tokens[6].value());

      assertEquals(Token.VALUE, tokens[7].ordinal());
      assertEquals("picture.png", tokens[7].value());

      assertEquals(Token.RIGHT_BRACE, tokens[8].ordinal());
      assertNull(tokens[8].value());
    }
  }

  public void testFlatArray()
  {
    for(String json : new String[]
    {
        "[\"John Doe\",\"picture.png\"]", " \r\n\t[ \r\n\t\"John Doe\" \r\n\t, \r\n\t\"picture.png\" \r\n\t] \r\n\t"
    }) {
      Token[] tokens = exercise(json);

      assertEquals(5, tokens.length);

      assertEquals(Token.LEFT_SQUARE, tokens[0].ordinal());
      assertNull(tokens[0].value());

      assertEquals(Token.ITEM, tokens[1].ordinal());
      assertEquals("John Doe", tokens[1].value());

      assertEquals(Token.COMMA, tokens[2].ordinal());
      assertNull(tokens[2].value());

      assertEquals(Token.ITEM, tokens[3].ordinal());
      assertEquals("picture.png", tokens[3].value());

      assertEquals(Token.RIGHT_SQUARE, tokens[4].ordinal());
      assertNull(tokens[4].value());
    }
  }

  public void testNestedObjects()
  {
    for(String json : new String[]
    {
        "{\"person\":{\"name\":\"John Doe\",\"picture\":\"picture.png\"}}", "{person:{name:\"John Doe\",picture:\"picture.png\"}}",
        " \r\n\t{ \r\n\t\"person\" \r\n\t: \r\n\t{ \r\n\t\"name\" \r\n\t: \r\n\t\"John Doe\" \r\n\t, \r\n\t\"picture\" \r\n\t: \r\n\t\"picture.png\" \r\n\t} \r\n\t} \r\n\t",
        " \r\n\t{ \r\n\tperson \r\n\t: \r\n\t{ \r\n\tname \r\n\t: \r\n\t\"John Doe\" \r\n\t, \r\n\tpicture \r\n\t: \r\n\t\"picture.png\" \r\n\t} \r\n\t} \r\n\t"
    }) {
      Token[] tokens = exercise(json);

      assertEquals(13, tokens.length);

      assertEquals(Token.LEFT_BRACE, tokens[0].ordinal());
      assertNull(tokens[0].value());

      assertEquals(Token.NAME, tokens[1].ordinal());
      assertEquals("person", tokens[1].value());

      assertEquals(Token.COLON, tokens[2].ordinal());
      assertNull(tokens[2].value());

      assertEquals(Token.LEFT_BRACE, tokens[3].ordinal());
      assertNull(tokens[3].value());

      assertEquals(Token.NAME, tokens[4].ordinal());
      assertEquals("name", tokens[4].value());

      assertEquals(Token.COLON, tokens[5].ordinal());
      assertNull(tokens[5].value());

      assertEquals(Token.VALUE, tokens[6].ordinal());
      assertEquals("John Doe", tokens[6].value());

      assertEquals(Token.COMMA, tokens[7].ordinal());
      assertNull(tokens[7].value());

      assertEquals(Token.NAME, tokens[8].ordinal());
      assertEquals("picture", tokens[8].value());

      assertEquals(Token.COLON, tokens[9].ordinal());
      assertNull(tokens[9].value());

      assertEquals(Token.VALUE, tokens[10].ordinal());
      assertEquals("picture.png", tokens[10].value());

      assertEquals(Token.RIGHT_BRACE, tokens[11].ordinal());
      assertNull(tokens[11].value());

      assertEquals(Token.RIGHT_BRACE, tokens[12].ordinal());
      assertNull(tokens[12].value());
    }
  }

  public void testNestedArrays()
  {
    for(String json : new String[]
    {
        "[\"person\",[\"John Doe\",\"picture.png\"]]",
        " \r\n\t[ \r\n\t\"person\" \r\n\t, \r\n\t[ \r\n\t\"John Doe\" \r\n\t, \r\n\t\"picture.png\" \r\n\t] \r\n\t] \r\n\t"
    }) {
      Token[] tokens = exercise(json);

      assertEquals(9, tokens.length);

      assertEquals(Token.LEFT_SQUARE, tokens[0].ordinal());
      assertNull(tokens[0].value());

      assertEquals(Token.ITEM, tokens[1].ordinal());
      assertEquals("person", tokens[1].value());

      assertEquals(Token.COMMA, tokens[2].ordinal());
      assertNull(tokens[2].value());

      assertEquals(Token.LEFT_SQUARE, tokens[3].ordinal());
      assertNull(tokens[3].value());

      assertEquals(Token.ITEM, tokens[4].ordinal());
      assertEquals("John Doe", tokens[4].value());

      assertEquals(Token.COMMA, tokens[5].ordinal());
      assertNull(tokens[5].value());

      assertEquals(Token.ITEM, tokens[6].ordinal());
      assertEquals("picture.png", tokens[6].value());

      assertEquals(Token.RIGHT_SQUARE, tokens[7].ordinal());
      assertNull(tokens[7].value());

      assertEquals(Token.RIGHT_SQUARE, tokens[8].ordinal());
      assertNull(tokens[8].value());
    }
  }

  public void testComplexGraph() throws IOException
  {
    String[] probes = new String[2];
    probes[0] = "{\"person\":{\"name\":\"John Doe\",\"picture\":\"picture.png\"},\"children\":[\"Adam\",{\"name\":\"Eva\",\"genre\":\"female\"}],\"age\":120,\"id\":0x10}";
    probes[1] = Strings.load(new File("fixture/person.json"));

    for(String json : probes) {
      Token[] tokens = exercise(json);

      assertEquals(37, tokens.length);

      assertEquals(Token.LEFT_BRACE, tokens[0].ordinal());
      assertNull(tokens[0].value());

      assertEquals(Token.NAME, tokens[1].ordinal());
      assertEquals("person", tokens[1].value());

      assertEquals(Token.COLON, tokens[2].ordinal());
      assertNull(tokens[2].value());

      assertEquals(Token.LEFT_BRACE, tokens[3].ordinal());
      assertNull(tokens[3].value());

      assertEquals(Token.NAME, tokens[4].ordinal());
      assertEquals("name", tokens[4].value());

      assertEquals(Token.COLON, tokens[5].ordinal());
      assertNull(tokens[5].value());

      assertEquals(Token.VALUE, tokens[6].ordinal());
      assertEquals("John Doe", tokens[6].value());

      assertEquals(Token.COMMA, tokens[7].ordinal());
      assertNull(tokens[7].value());

      assertEquals(Token.NAME, tokens[8].ordinal());
      assertEquals("picture", tokens[8].value());

      assertEquals(Token.COLON, tokens[9].ordinal());
      assertNull(tokens[9].value());

      assertEquals(Token.VALUE, tokens[10].ordinal());
      assertEquals("picture.png", tokens[10].value());

      assertEquals(Token.RIGHT_BRACE, tokens[11].ordinal());
      assertNull(tokens[11].value());

      assertEquals(Token.COMMA, tokens[12].ordinal());
      assertNull(tokens[12].value());

      assertEquals(Token.NAME, tokens[13].ordinal());
      assertEquals("children", tokens[13].value());

      assertEquals(Token.COLON, tokens[14].ordinal());
      assertNull(tokens[14].value());

      assertEquals(Token.LEFT_SQUARE, tokens[15].ordinal());
      assertNull(tokens[15].value());

      assertEquals(Token.ITEM, tokens[16].ordinal());
      assertEquals("Adam", tokens[16].value());

      assertEquals(Token.COMMA, tokens[17].ordinal());
      assertNull(tokens[17].value());

      assertEquals(Token.LEFT_BRACE, tokens[18].ordinal());
      assertNull(tokens[18].value());

      assertEquals(Token.NAME, tokens[19].ordinal());
      assertEquals("name", tokens[19].value());

      assertEquals(Token.COLON, tokens[20].ordinal());
      assertNull(tokens[20].value());

      assertEquals(Token.VALUE, tokens[21].ordinal());
      assertEquals("Eva", tokens[21].value());

      assertEquals(Token.COMMA, tokens[22].ordinal());
      assertNull(tokens[22].value());

      assertEquals(Token.NAME, tokens[23].ordinal());
      assertEquals("genre", tokens[23].value());

      assertEquals(Token.COLON, tokens[24].ordinal());
      assertNull(tokens[24].value());

      assertEquals(Token.VALUE, tokens[25].ordinal());
      assertEquals("female", tokens[25].value());

      assertEquals(Token.RIGHT_BRACE, tokens[26].ordinal());
      assertNull(tokens[26].value());

      assertEquals(Token.RIGHT_SQUARE, tokens[27].ordinal());
      assertNull(tokens[27].value());

      assertEquals(Token.COMMA, tokens[28].ordinal());
      assertNull(tokens[28].value());

      assertEquals(Token.NAME, tokens[29].ordinal());
      assertEquals("age", tokens[29].value());

      assertEquals(Token.COLON, tokens[30].ordinal());
      assertNull(tokens[30].value());

      assertEquals(Token.VALUE, tokens[31].ordinal());
      assertEquals("120", tokens[31].value());

      assertEquals(Token.COMMA, tokens[32].ordinal());
      assertNull(tokens[32].value());

      assertEquals(Token.NAME, tokens[33].ordinal());
      assertEquals("id", tokens[33].value());

      assertEquals(Token.COLON, tokens[34].ordinal());
      assertNull(tokens[34].value());

      assertEquals(Token.VALUE, tokens[35].ordinal());
      assertEquals("0x10", tokens[35].value());

      assertEquals(Token.RIGHT_BRACE, tokens[36].ordinal());
      assertNull(tokens[36].value());
    }
  }

  /**
   * Exercise lexer. Returns array of token value instances; do not use token directly because token enumeration
   * instance is singleton, that is a single instance per constant - token type and on multiple tokens of the same type
   * all will have the same value.
   * 
   * @param json
   * @return
   */
  private static Token[] exercise(String json)
  {
    List<Token> tokens = new ArrayList<>();
    Lexer lexer = new Lexer(new StringReader(json));
    try {
      for(;;) {
        Token token = lexer.read();
        if(token.ordinal() == Token.EOF) {
          break;
        }
        tokens.add(token);
      }
    }
    catch(Throwable e) {
      e.printStackTrace();
    }
    return tokens.toArray(new Token[tokens.size()]);
  }

  public void testWhiteSpaces()
  {
    String json = "{\r\n\t\"name\" : \"John Doe\",\r\n\t\"picture\": \"picture.png\",\r\n\t\"id\" : 0x10\r\n}";
    Token[] tokens = exercise(json);

    assertEquals(13, tokens.length);

    assertEquals(Token.LEFT_BRACE, tokens[0].ordinal());
    assertNull(tokens[0].value());

    assertEquals(Token.NAME, tokens[1].ordinal());
    assertEquals("name", tokens[1].value());

    assertEquals(Token.COLON, tokens[2].ordinal());
    assertNull(tokens[2].value());

    assertEquals(Token.VALUE, tokens[3].ordinal());
    assertEquals("John Doe", tokens[3].value());

    assertEquals(Token.COMMA, tokens[4].ordinal());
    assertNull(tokens[4].value());

    assertEquals(Token.NAME, tokens[5].ordinal());
    assertEquals("picture", tokens[5].value());

    assertEquals(Token.COLON, tokens[6].ordinal());
    assertNull(tokens[6].value());

    assertEquals(Token.VALUE, tokens[7].ordinal());
    assertEquals("picture.png", tokens[7].value());

    assertEquals(Token.COMMA, tokens[8].ordinal());
    assertNull(tokens[8].value());

    assertEquals(Token.NAME, tokens[9].ordinal());
    assertEquals("id", tokens[9].value());

    assertEquals(Token.COLON, tokens[10].ordinal());
    assertNull(tokens[10].value());

    assertEquals(Token.VALUE, tokens[11].ordinal());
    assertEquals("0x10", tokens[11].value());

    assertEquals(Token.RIGHT_BRACE, tokens[12].ordinal());
    assertNull(tokens[12].value());
  }
}
