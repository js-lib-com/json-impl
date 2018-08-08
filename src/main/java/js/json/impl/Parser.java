package js.json.impl;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;

import js.converter.Converter;
import js.converter.ConverterRegistry;
import js.lang.OrdinalEnum;
import js.log.Log;
import js.log.LogFactory;
import js.util.Types;

/**
 * Syntactic parser. Scans token generated by {@link Lexer} and initialize primitive values, object fields, array items
 * or map entries described by JSON stream. Note that this parser accepts only double quotes for delimiters mark, i.e.
 * single quotation mark is not supported; this limitation is actually implemented in lexer.
 * 
 * <h6>Extensions</h6>
 * <p>
 * This parser supports a proprietary extensions to JSON protocol. It accepts inband type information generated by
 * {@link Serializer#Serializer(boolean)}. Although this feature is public it is intended for library internal use only.
 * Also it accepts map keys of numeric and object types, see code snippet.
 * 
 * <pre>
 * {{"name":"WALLE","age":1964}:"worker",{"name":"EVA"}:"specialist"}
 * {1964:"worker",0:"specialist"}
 * {1.5:"worker",2.0:"specialist"}
 * </pre>
 * 
 * In above example, JSON protocol specs requires string for property name but this parser accepts objects and numbers,
 * both integer and double. This extension is used for map instances deserialization and is supported by
 * {@link Serializer} too.
 * <p>
 * Usually enumeration are expected as upper case string value identifying enumeration constant name. Anyway, if
 * enumeration type implements {@link OrdinalEnum} string value should be a numeric value equal with enumeration
 * constant ordinal.
 * 
 * <h6>Best Effort</h6>
 * <p>
 * This JSON parser is relaxed and follows a best effort approach. If a named property from JSON stream does not have
 * the same name field into target object, parser just warn on log and ignore. Also, fields from target object with no
 * values into JSON stream are set to null.
 * 
 * <h6>Undocumented Extension</h6>
 * <p>
 * JSON syntax mandates quotes for all string values no matter if used for property name or primitive value. This parser
 * accepts strings without quotes, of course provided string does not contain white space or reserved chars. For example
 * an array of enumerations is standard encoded ["LIGER","TIGON"] but this parser accepts [LIGER,TIGON]. Anyway, this
 * extension is for library internal use only and user space code is not encouraged to use it.
 * 
 * @author Iulian Rotaru
 */
final class Parser
{
  /** Class logger. */
  private static final Log log = LogFactory.getLog(Parser.class);

  /** Morphological parser. */
  private Lexer lexer;

  /** Current state for parser automata. */
  private State state;

  /** Flag true to process inband type information. */
  private boolean includeClass;

  /** Create parser instance and initialize automata state. */
  public Parser()
  {
    this.state = State.NONE;
    ErrorReporter.getInstance().reset();
  }

  /**
   * Parse a JSON characters stream with inband type information. This method is the counterpart of
   * {@link Serializer#Serializer(boolean)} and is designed for library internal use.
   * 
   * @param reader JSON stream with inband type information.
   * @param <T> type to auto cast on return, cast compatible with inband type from JSON stream.
   * @return newly created and initialized instance.
   * @throws IOException if IO read operation fails.
   * @throws ClassCastException if inband type from JSON stream cannot cast to auto cast type.
   */
  public <T> T parse(Reader reader) throws IOException, ClassCastException
  {
    includeClass = true;
    return parse(reader, (Type)null);
  }

  /**
   * Parse value of requested type, primitive or aggregated type, from JSON stream. This method prepare parser internal
   * state and handle exception but delegates the hard work to {@link #_parse(Type)}. Also, for user code convenience,
   * this method uses auto-cast, that is cast value to parameterized type internally avoiding the need for explicit
   * cast. Is caller responsibility to ensure cast compatibility otherwise this method throws cast exception.
   * 
   * @param reader input JSON character stream,
   * @param type expected type for value from JSON stream.
   * @return value instance initialized from JSON stream.
   * @throws IOException if reading from input character stream fails.
   * @throws JsonParserException if JSON stream is not well formed.
   * @throws ClassCastException if value instance cannot be auto-cast to requested type.
   */
  public <T> T parse(Reader reader, Type type) throws IOException, JsonParserException, ClassCastException
  {
    lexer = new Lexer(reader);
    try {
      return _parse(type);
    }
    catch(IOException e) {
      log.debug(e);
      throw e;
    }
    catch(JsonParserException e) {
      log.debug(e);
      throw e;
    }
    catch(Throwable t) {
      log.debug(t);
      throw new JsonParserException(t);
    }
  }

  /**
   * Parse mixed types JSON array and return array instance with items initialized from stream. Stream should contain a
   * JSON array that is not required to be homogeneous. Is caller responsibility to ensure JSON array from stream has
   * desired number and types. If array from stream is smaller than the number of requested types, returned not yet
   * initialized items are set to null.
   * <p>
   * Although public this method is primarily designed to parse method arguments for HTTP-RMI.
   * 
   * @param reader character stream carrying mixed types JSON array,
   * @param types expected types list.
   * @return array instance with items initialized from given JSON stream.
   * @throws JsonParserException if JSON stream format is not valid.
   * @throws IOException if stream read fails.
   * @throws ClassCastException if array item cannot cast to expected type.
   */
  public Object[] parse(Reader reader, Type[] types) throws JsonParserException, IOException, ClassCastException
  {
    lexer = new Lexer(reader);
    Token token = lexer.read();

    Object[] instances = new Object[types.length];
    if(token.ordinal() == Token.EOF) {
      log.warn("Empty JSON stream for mixed types array. Return array with all items set to null.");
      return instances;
    }

    if(token.ordinal() != Token.LEFT_SQUARE) {
      throw new JsonParserException("Invalid JSON stream for mixed types, aka arguments, array. Bad start token. Expected LEFT_SUQARE but got %s.", token);
    }

    try {
      for(int i = 0; i < types.length; i++) {
        state = State.NONE;
        instances[i] = _parse(types[i]);
        token = lexer.read();
        if(token.ordinal() == Token.RIGHT_SQUARE) {
          break;
        }
        assert token.ordinal() == Token.COMMA;
      }
      return instances;
    }
    catch(IOException e) {
      log.debug(e);
      throw e;
    }
    catch(JsonParserException e) {
      log.debug(e);
      throw e;
    }
    catch(Throwable t) {
      log.debug(t);
      throw new JsonParserException(t);
    }
  }

  /**
   * Create instance or expected <code>type</code>, parse tokens from internal lexer and initialize instance fields. It
   * is caller responsibility to ensure JSON stream describe an instance compatible to expected type.
   * <p>
   * Note that if type is strict object named fields from JSON stream are searched on superclass too.
   * 
   * @param type expected type.
   * @return newly created instance of requested type.
   * @throws JsonParserException if lexer fails to decode JSON character stream.
   * @throws IOException if IO read operation fails.
   */
  @SuppressWarnings("unchecked")
  private <T> T _parse(Type type) throws JsonParserException, IOException
  {
    Value value = includeClass ? null : getValueInstance(type);
    includeClass = false;
    Token token = null;

    TOKENS_LOOP: for(;;) {
      token = lexer.read();

      switch(state) {

      case NONE:
        switch(token.ordinal()) {
        case Token.VALUE:
          // item is used here to support multiple types parsing
          // multiple types are actually a JSON array but every item with its own type
        case Token.ITEM:
          value.set(token.value());
          break TOKENS_LOOP;

        case Token.LEFT_BRACE:
          if(value instanceof MapValue) {
            state = State.WAIT_FOR_KEY;
          }
          else {
            state = State.WAIT_FOR_NAME_OR_CLASS;
          }
          continue;

        case Token.LEFT_SQUARE:
          state = State.WAIT_FOR_ITEM;
          continue;

        // array end token on parser state none means empty JSON array; break parsing loop
        case Token.RIGHT_SQUARE:
          lexer.unread(token);
          break TOKENS_LOOP;

        case Token.EOF:
          throw new JsonParserException("Closed reader. No data available for parsing.");

        default:
          throw new JsonParserException("Invalid start token %s.", token, this.state);
        }

      case WAIT_FOR_NAME_OR_CLASS:
        if(token.ordinal() == Token.NAME) {
          if("class".equals(token.value())) {
            if(value != null) {
              throw new JsonParserException("Illegal state. User requested type argument on JSON with inbound class.");
            }
            token = lexer.read();
            if(token.ordinal() != Token.COLON) {
              throw new JsonParserException("Expected COLON but got %s.", token);
            }
            token = lexer.read();

            value = getValueInstance(loadClass(token.value()));
            state = State.WAIT_FOR_COMMA_OR_RIGHT_BRACE;
            continue;
          }
        }
        // fall through WAIT_FOR_NAME case

      case WAIT_FOR_NAME:
        switch(token.ordinal()) {
        case Token.RIGHT_BRACE: // empty object
          break TOKENS_LOOP;

        case Token.NAME:
          if(!(value instanceof ObjectValue)) {
            throw new JsonParserException("Invalid value helper |%s| for target type |%s|.", value.getClass(), type);
          }
          ((ObjectValue)value).setFieldName(token.value());
          state = State.WAIT_FOR_COLON;
          continue;

        default:
          throw new JsonParserException("Invalid token |%s| while waiting for a name.", token);
        }

      case WAIT_FOR_COLON:
        if(token.ordinal() != Token.COLON) {
          throw new JsonParserException("Expected COLON but got |%s|.", token);
        }
        state = State.WAIT_FOR_VALUE;
        continue;

      case WAIT_FOR_VALUE:
        if(!(value instanceof ObjectValue)) {
          throw new JsonParserException("Invalid value helper |%s| for target type |%s|.", value.getClass(), type);
        }
        final ObjectValue objectValue = (ObjectValue)value;
        switch(token.ordinal()) {
        case Token.LEFT_BRACE:
        case Token.LEFT_SQUARE:
          state = State.NONE;
          lexer.unread(token);
          objectValue.setValue(_parse(objectValue.getValueType()));
          state = State.WAIT_FOR_COMMA_OR_RIGHT_BRACE;
          continue;

        case Token.VALUE:
          objectValue.setValue(token.value());
          state = State.WAIT_FOR_COMMA_OR_RIGHT_BRACE;
          continue;

        default:
          throw new JsonParserException("Expect VALUE, LEFT_BRACE or LEFT_SQUARE but got %s.", token);
        }

      case WAIT_FOR_COMMA_OR_RIGHT_BRACE:
        if(token.ordinal() == Token.COMMA) {
          if(value instanceof MapValue) {
            state = State.WAIT_FOR_KEY;
          }
          else {
            state = State.WAIT_FOR_NAME;
          }
          continue;
        }
        if(token.ordinal() != Token.RIGHT_BRACE) {
          throw new JsonParserException("Expected RIGHT_BRACE but got %s. Maybe missing comma.", token);
        }
        break TOKENS_LOOP;

      case WAIT_FOR_KEY:
        if(!(value instanceof MapValue)) {
          throw new JsonParserException("Invalid value helper |%s| for target type |%s|.", value.getClass(), type);
        }
        final MapValue mapValue = (MapValue)value;
        switch(token.ordinal()) {
        case Token.LEFT_BRACE:
          state = State.NONE;
          lexer.unread(token);
          mapValue.setKey(_parse(mapValue.keyType()));
          state = State.WAIT_FOR_COLON;
          break;

        case Token.RIGHT_BRACE: // empty map
          break TOKENS_LOOP;

        case Token.NAME:
          mapValue.setKey(token.value());
          state = State.WAIT_FOR_COLON;
          break;

        default:
          throw new JsonParserException("Unexpected token |%s| while waiting for map key.", token);
        }
        break;

      case WAIT_FOR_ITEM:
        switch(token.ordinal()) {
        case Token.LEFT_BRACE: // object inside array
        case Token.LEFT_SQUARE: // array inside array
          state = State.NONE;
          lexer.unread(token);
          value.set(_parse(value.getType()));
          state = State.WAIT_FOR_COMMA_OR_RIGHT_SQUARE;
          continue;

        case Token.RIGHT_SQUARE: // empty array
          break TOKENS_LOOP;

        case Token.ITEM:
          value.set(token.value());
          state = State.WAIT_FOR_COMMA_OR_RIGHT_SQUARE;
          continue;

        default:
          throw new JsonParserException("Expect ITEM, LEFT_BRACE or LEFT_SQUARE but got %s.", token);
        }
        // fall through WAIT_FOR_COMMA_OR_RIGHT_SQUARE case

      case WAIT_FOR_COMMA_OR_RIGHT_SQUARE:
        if(token.ordinal() == Token.COMMA) {
          state = State.WAIT_FOR_ITEM;
          continue;
        }
        if(token.ordinal() != Token.RIGHT_SQUARE) {
          throw new JsonParserException("Expected RIGHT_SQUARE but got %s. Maybe missing comma.", token);
        }
        break TOKENS_LOOP;
      }
    }

    state = State.NONE;
    return (T)value.instance();
  }

  /**
   * Get parser value helper instance suitable for handling the given type.
   * 
   * @param type type to get value helper for.
   * @return value helper instance.
   */
  private Value getValueInstance(Type type)
  {
    Converter converter = ConverterRegistry.getConverter();
    if(type == null) {
      return new MissingFieldValue(converter);
    }
    if(Types.isArray(type)) {
      return new ArrayValue(converter, type);
    }
    if(Types.isCollection(type)) {
      return new CollectionValue(converter, type);
    }
    if(type instanceof Class<?> && ConverterRegistry.hasType(type)) {
      return new PrimitiveValue(converter, (Class<?>)type);
    }
    if(Types.isPrimitiveLike(type)) {
      return new PrimitiveValue(converter, (Class<?>)type);
    }
    if(Types.isMap(type)) {
      return new MapValue(converter, type);
    }

    // at this point type should denote a not parameterized strict object
    if(!(type instanceof Class)) {
      throw new JsonParserException("Illegal state. Generic objects |%s| are not supported.", type);
    }
    return new ObjectValue(converter, (Class<?>)type);
  }

  /**
   * Load named class.
   * 
   * @param className qualified class name.
   * @return class singleton.
   * @throws JsonParserException if class not found.
   */
  @SuppressWarnings("unchecked")
  private static <T> Class<T> loadClass(String className)
  {
    ClassLoader currentThreadClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      return (Class<T>)Class.forName(className, true, currentThreadClassLoader);
    }
    catch(ClassNotFoundException unused1) {
      if(!currentThreadClassLoader.equals(Parser.class.getClassLoader())) {
        try {
          return (Class<T>)Class.forName(className, true, Parser.class.getClassLoader());
        }
        catch(ClassNotFoundException unused2) {
          throw new JsonParserException("JSON requested class |%s| not found.", className);
        }
      }
    }
    return null;
  }

  /**
   * Parser state machine.
   * 
   * @author Iulian Rotaru
   */
  private static enum State
  {
    NONE, WAIT_FOR_NAME, WAIT_FOR_NAME_OR_CLASS, WAIT_FOR_ITEM, WAIT_FOR_KEY, WAIT_FOR_COLON, WAIT_FOR_VALUE, WAIT_FOR_COMMA_OR_RIGHT_BRACE, WAIT_FOR_COMMA_OR_RIGHT_SQUARE
  }
}
