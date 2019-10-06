package js.json.impl;

import static js.util.Params.notNull;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Type;

import js.json.Json;
import js.util.Files;

/**
 * JSON serialization / deserialization facade. This facade just delegates {@link Serializer} and {@link Parser}
 * functionality. It supplies methods to {@link #stringify(Writer, Object)} and {@link #parse(Reader, Type)} values to
 * and from JSON character streams. There are also convenient methods for values to JSON strings conversion, see
 * {@link #stringify(Object)} and {@link #parse(String, Type)}.
 * <p>
 * Parsing of not homogeneous arrays is supported but caller should supplies the type of every array item, see
 * {@link #parse(Reader, Type[])}.
 * <p>
 * JSON facade also provides methods for (de)serialization with inband type information, see
 * {@link #stringifyObject(Writer, Object)} and {@link #parseObject(Reader)}. This is an extension to JSON protocol that
 * include <code>class</code> property before the actual object JSON stream. Although public, this feature is intended
 * for library internal use only.
 * <p>
 * A JSON stream with inbad type information may look like below. Please note <code>class</code> property from JSON
 * start.
 * 
 * <pre>
 *  {"class":"js.net.Event","id":1234 ... }
 * </pre>
 * 
 * Implementation note: current implementation does not support inband type information for JSON arrays; only objects.
 * Also, for nested objects only outer most one have <code>class</code> field. Inner objects type is inferred
 * reflectively.
 * 
 * @author Iulian Rotaru
 */
public final class JsonImpl implements Json
{
  /** Empty array constant. */
  private static final Object[] EMPTY_ARRAY = new Object[0];

  /**
   * Serialize value to JSON character stream. Both primitive and aggregated values are allowed. If value is not
   * primitive all fields are scanned reflectively, less static and transient. If a field is aggregated on its turn
   * traverse its fields too. This allows for not restricted fields graph. Note that there is no guarantee regarding
   * fields order inside parent object.
   * <p>
   * After serialization completes <code>writer</code> is flushed but left unclosed.
   * 
   * @param writer character stream to write value on,
   * @param value value to serialize, null accepted.
   * @throws IllegalArgumentException if <code>writer</code> argument is null.
   * @throws IOException if IO write operation fails.
   */
  @Override
  public void stringify(Writer writer, Object value) throws IOException
  {
    notNull(writer, "JSON stream writer");
    Serializer serializer = new Serializer();
    serializer.serialize(writer, value);
  }

  /**
   * Deserialize value of expected type. After parsing completion used <code>reader</code> remains opened.
   * <p>
   * This method uses auto cast in order to simplify user code but is caller responsibility to ensure requested
   * <code>type</code> is cast compatible with type of variable to assign to.
   * <p>
   * Internally this method uses {@link Parser#parse(Reader, Type)} and observes the same best effort behavior.
   * 
   * @param reader character stream to read from,
   * @param type expected type.
   * @param <T> type to auto cast on return, cast compatible with <code>type</code> argument.
   * @return instance of expected type initialized from JSON character stream.
   * @throws IllegalArgumentException if <code>reader</code> or <code>type</code> argument is null.
   * @throws IOException if read operation fails.
   * @throws JsonParserException if parsing process fails perhaps due to syntax violation on input.
   * @throws ClassCastException if given <code>type</code> cannot cast to expected type variable <code>T</code>.
   */
  @Override
  public <T> T parse(Reader reader, Type type) throws IllegalArgumentException, IOException, JsonParserException, ClassCastException
  {
    notNull(reader, "JSON stream reader");
    Parser parser = new Parser();
    return parser.parse(reader, type);
  }

  /**
   * Deserialize array of mixed types, also known as arguments array. This method is able to parse not homogeneous
   * arrays; if in need for homogeneous array one could use {@link #parse(Reader, Type)} with <code>type</code> set to
   * desired array class. Every object from JSON stream array must have the type defined by <code>types</code>
   * parameter. It is caller responsibility to ensure that JSON stream array types number and order match requested
   * <code>types</code>.
   * <p>
   * Internally this method uses {@link Parser#parse(Reader, Type)} and observes the same best effort behavior. Note
   * that after parsing completion used <code>reader</code> remains opened.
   * 
   * @param reader character stream to read from,
   * @param types expected types.
   * @return newly created and initialized array.
   * @throws IllegalArgumentException if <code>reader</code> or <code>types</code> argument is null.
   * @throws IOException if IO read operation fails.
   * @throws JsonParserException if parsing process fails perhaps due to syntax violation on input.
   */
  @Override
  public Object[] parse(Reader reader, Type[] types) throws IllegalArgumentException, IOException, JsonParserException
  {
    notNull(reader, "JSON stream reader");
    notNull(types, "Expected types");
    if(types.length == 0) {
      return EMPTY_ARRAY;
    }
    Parser parser = new Parser();
    return parser.parse(reader, types);
  }

  /**
   * Serialize object instance with inband type information. An object serialized with this method must be parsed by
   * {@link #parseObject(Reader)} companion method. After serialization completes <code>writer</code> is flushed but
   * left unclosed.
   * <p>
   * Implementation note: this method uses a proprietary extension to JSON protocol. In spite it is public it is
   * designed for library internal use.
   * 
   * @param writer characters stream writer,
   * @param value object instance to serialize, null accepted.
   * @throws IllegalArgumentException if <code>writer</code> argument is null.
   * @throws IOException if write operation fails.
   */
  @Override
  public void stringifyObject(Writer writer, Object value) throws IllegalArgumentException, IOException
  {
    notNull(writer, "JSON stream writer");
    Serializer serializer = new Serializer(true);
    serializer.serialize(writer, value);
  }

  /**
   * Deserialize JSON encoded object with inband type information. This method expect a stream generated by
   * {@link #stringifyObject(Writer, Object)} companion method. After parsing completion used <code>reader</code>
   * remains opened.
   * <p>
   * Implementation note: this method uses a proprietary extension to JSON protocol. In spite it is public it is
   * designed for library internal use.
   * 
   * @param reader characters stream reader.
   * @return initialized instance auto-cast to expected parameterized type <code>T</code>.
   * @throws IllegalArgumentException if <code>reader</code> argument is null.
   * @throws IOException if read operation fails.
   * @throws JsonParserException if parsing process fails perhaps due to syntax violation on input.
   * @throws ClassCastException if inband type cannot be cast to expected parameterized type <code>T</code>.
   */
  @Override
  public <T> T parseObject(Reader reader) throws IllegalArgumentException, IOException, JsonParserException, ClassCastException
  {
    notNull(reader, "JSON stream reader");
    Parser parser = new Parser();
    return parser.parse(reader);
  }

  /**
   * Handy method for value object serialization to JSON formatted string.
   * 
   * @param value primitive or aggregate value, null accepted.
   * @return value JSON string representation.
   */
  @Override
  public String stringify(Object value)
  {
    StringWriter writer = new StringWriter();
    try {
      Serializer serializer = new Serializer();
      serializer.serialize(writer, value);
    }
    catch(IOException e) {
      // there is no reason to have IO write operation fail on string beside hardware or system resources shortage
      throw new RuntimeException(e);
    }
    finally {
      Files.close(writer);
    }
    return writer.toString();
  }

  /**
   * Parse JSON encoded value and return instance of requested type. This method creates an instance of
   * <code>type</code> and initialize its fields from JSON string. It uses auto cast in order to simplify user code but
   * is caller responsibility to ensure requested <code>type</code> is cast compatible with type of variable to assign
   * to.
   * <p>
   * Internally this method uses {@link Parser#parse(Reader, Type)} and observes the same best effort behavior.
   * 
   * @param value JSON encode value,
   * @param type desired value type.
   * @param <T> type to auto cast on return, cast compatible with <code>type</code> argument.
   * @return newly created instance or null if <code>value</code> argument is null.
   * @throws IllegalArgumentException if <code>type</code> argument is null.
   * @throws JsonParserException if given string value is not valid JSON format.
   */
  @Override
  public <T> T parse(String value, Type type) throws IllegalArgumentException, JsonParserException
  {
    if(value == null) {
      return null;
    }
    notNull(type, "Type");
    StringReader reader = new StringReader(value);
    Parser parser = new Parser();
    try {
      return parser.parse(reader, type);
    }
    catch(IOException e) {
      // there is no reason to have IO read operation fail on string beside hardware or system resources shortage
      throw new RuntimeException(e);
    }
    // do not bother to close reader since its scope is local and is not bound to target host resources
  }
}
