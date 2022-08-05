package com.jslib.json;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ListIterator;
import java.util.Map;
import java.util.Stack;

import com.jslib.api.json.JsonLifeCycle;
import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.converter.Converter;
import com.jslib.converter.ConverterRegistry;
import com.jslib.lang.BugError;
import com.jslib.lang.OrdinalEnum;
import com.jslib.util.Strings;
import com.jslib.util.Types;

/**
 * Serialize primitive values, enumerations, objects, arrays, collections and maps to JSON string representation. This
 * class is invoked internally by {@link JsonImpl} facade and is not intended to be reused; create a new serializer
 * instance for every value to stringify. Once instance created invoke {@link #serialize(Writer, Object)} with external
 * created characters writer instance. After serialization completes writer is flushed but left opened.
 * <p>
 * This class handle serialization in a best effort manner. If for some reason a field cannot be processed it is
 * replaced by null, but takes care to record failing condition to error log. Currently there are two conditions that
 * could lead to <code>null</code> replacement:
 * <ol>
 * <li>a value is a Hibernate PersistentCollection mapped lazily and attempt to access it outside Hibernate Session,
 * <li>circular dependency discovered, see {@link #circularDependenciesStack}.
 * </ol>
 * <p>
 * For enumeration serialization this class uses enumeration constant upper case name, as string. Anyway, if enumeration
 * implements {@link OrdinalEnum} this serializer uses enumeration ordinal as numeric value.
 * 
 * @author Iulian Rotaru
 */
public class Serializer
{
  /** Class logger. */
  protected static final Log log = LogFactory.getLog(Serializer.class);

  /** JSON keyword for null values. */
  private static final String KEYWORD_NULL = "null";

  /** External created writer instance initialized by {@link #serialize(Writer, Object)} entry point. */
  private Writer writer;

  /**
   * Circular dependencies stack keeps track of processed values. Values are pushed just before entering the actual
   * serialization and extracted at final. See {@link #serialize(Object)}.
   */
  private Stack<Object> circularDependenciesStack = new Stack<Object>();

  /** Create default serializer. */
  public Serializer()
  {
  }

  /**
   * Serialize primitive or aggregated value to given writer. Writer is flushed after serialization completes but is
   * left opened.
   * 
   * @param writer external created characters stream,
   * @param value primitive or aggregated value.
   * @throws IOException if IO write operation fails.
   */
  public void serialize(Writer writer, Object value) throws IOException
  {
    this.writer = writer instanceof BufferedWriter ? writer : new BufferedWriter(writer);
    serialize(value);
    this.writer.flush();
  }

  /**
   * Serialization worker implements core logic for serialization process. It handles both primitive and aggregated
   * values. For aggregated values delegates helpers implemented by this class: {@link #serializeArray(Object)},
   * {@link #serializeMap(Object)} and {@link #serializeObject(Object)}. Helpers method takes care of structural details
   * but for value serialization re-invoke this method. If value is null uses {@link #KEYWORD_NULL}.
   * <p>
   * Because for non primitive values this method is executed recursively it implements protection against circular
   * dependencies. When a circular dependency is discovered replace value with null and record to error log. For
   * circular dependencies protection uses {@link #circularDependenciesStack}. Value to serialize is pushed to stack
   * just before processing and extracted after. If value it is already in stack replace it with null and dump the stack
   * to error log.
   * 
   * @param value primitive or aggregated value.
   * @throws IOException if IO write operation fails.
   */
  protected void serialize(Object value) throws IOException
  {
    if(value == null) {
      write(KEYWORD_NULL);
      return;
    }

    if(circularDependenciesStack.contains(value)) {
      dumpCircularDependenciesStack(value);
      write(KEYWORD_NULL);
      return;
    }
    circularDependenciesStack.push(value);

    if(value instanceof JsonLifeCycle) {
      ((JsonLifeCycle)value).preStringify();
    }

    Converter converter = ConverterRegistry.getConverter();
    try {
      if(Types.isBoolean(value)) {
        write(converter.asString(value));
        return;
      }

      if(Types.isNumber(value) || value instanceof OrdinalEnum) {
        write(converter.asString(value));
        return;
      }

      if(Types.isArrayLike(value)) {
        serializeArray(value);
        return;
      }

      // converter manager has support for collections; it join/split collection to/from comma separated string
      // JSON needs to serialize collections as array, so take care to test for array like before converter type

      if(value instanceof String || ConverterRegistry.hasType(value.getClass())) {
        writeString(converter.asString(value));
        return;
      }

      if(Types.isMap(value)) {
        serializeMap(value);
        return;
      }

      serializeObject(value);
    }
    finally {
      circularDependenciesStack.pop();
    }
  }

  /**
   * Serialize opening square brace, array items separated by comma and closing square brace. This method invoked
   * recursively {@link #serialize(Object)} for every array item.
   * 
   * @param value
   * @throws IOException
   */
  protected void serializeArray(Object value) throws IOException
  {
    write('[');
    int index = 0;

    for(Object item : Types.asIterable(value)) {
      if(index++ > 0) {
        write(',');
      }
      serialize(item);
    }

    write(']');
  }

  /**
   * Serialize value map entries separated by comma, key/value pair being separated by colon, all in curly braces. A map
   * is serialized like an object in opening and closing curly braces. For key and value serialization this method
   * recursively invoke {@link #serialize(Object)}.
   * 
   * @param value value map.
   * @throws IOException if IO write operation fails.
   */
  protected void serializeMap(Object value) throws IOException
  {
    assert value != null;
    assert value instanceof Map;

    write('{');

    @SuppressWarnings("unchecked")
    Map<Object, Object> map = (Map<Object, Object>)value;
    int index = 0;

    for(Map.Entry<Object, Object> entry : map.entrySet()) {
      if(index++ > 0) {
        write(',');
      }
      serialize(entry.getKey());
      write(':');
      serialize(entry.getValue());
    }

    write('}');
  }

  /**
   * Serialize opening curly brace, all fields separated by comma and closing curly brace. All fields are serialized,
   * except static and transient, in no particular order. Also this method process superclass hierarchy as long as
   * superclass is part of the same package as given value object.
   * <p>
   * Note that this method hides a recursive call to {@link #serialize(Object)} via
   * {@link #serializeField(Object, Field)}.
   * 
   * @param value value object to serialize.
   * @throws IOException if IO write operation fails.
   */
  protected void serializeObject(Object value) throws IOException
  {
    assert value != null;

    write('{');
    int index = 0;

    Class<?> clazz = value.getClass();

    for(Field field : clazz.getDeclaredFields()) {
      if(field.isSynthetic()) {
        // do not include synthetic fields like outer 'this' for anonymous inner classes
        continue;
      }
      int m = field.getModifiers();
      if(Modifier.isStatic(m) || Modifier.isTransient(m)) {
        continue;
      }
      if(index++ > 0) {
        write(',');
      }
      serializeField(value, field);
    }

    // include super classes fields as long as they are in the same package
    Package classPackage = clazz.getPackage();
    Class<?> superclass = clazz.getSuperclass();
    while(superclass != null && classPackage.equals(superclass.getPackage())) {
      for(Field field : superclass.getDeclaredFields()) {
        int m = field.getModifiers();
        if(Modifier.isStatic(m) || Modifier.isTransient(m)) {
          continue;
        }
        if(index++ > 0) {
          write(',');
        }
        serializeField(value, field);
      }
      superclass = superclass.getSuperclass();
    }

    write('}');
  }

  /**
   * Serialize field name, colon as value separator and field value. Note that field value is processed by invoking
   * recursively {@link #serialize(Object)}.
   * 
   * @param value value object field belongs to,
   * @param field field reflective descriptor.
   * @throws IOException if IO write operation fails.
   */
  protected void serializeField(Object value, Field field) throws IOException
  {
    field.setAccessible(true);
    writeString(field.getName());
    write(':');
    serialize(getFieldValue(value, field));
  }

  /**
   * Get field value from given instance.
   * 
   * @param instance object instance to get field value from,
   * @param field field reflective descriptor.
   * @return instance field value.
   */
  protected static Object getFieldValue(Object instance, Field field)
  {
    try {
      return field.get(instance);
    }
    catch(IllegalArgumentException e) {
      throw new BugError("Value object |%s| is not an instance.", instance.getClass());
    }
    catch(IllegalAccessException ignore) {
      throw new RuntimeException("Field with accesibily set to true throws illegal access.");
    }
    catch(RuntimeException e) {
      // if field is a Hibernate PersistentCollection and i mapped lazy trying to access it outside Hibernate Session
      // will throw HibernateException which is a RuntimeException
      // in order to avoid Hibernate library dependency on this package uses RuntimeException to catch this condition
      // log this condition but take best effort approach: do not throw exception, instead leave value to null
      log.error("Error reading field |%s| value. Set it to null. Error cause: %s", field, e);
    }
    return null;
  }

  /**
   * Write string and takes care to escape accordingly RFC 4627.
   * <p>
   * Excerpt: ... string begins and ends with quotation marks. All Unicode characters may be placed within the quotation
   * marks except for the characters that must be escaped: quotation mark, reverse solidus, and the control characters
   * (U+0000 through U+001F).
   * 
   * @param string string to escape and write.
   * @throws IOException if IO write operation fails.
   */
  protected void writeString(String string) throws IOException
  {
    write('"');

    for(int i = 0; i < string.length(); i++) {
      char c = string.charAt(i);
      switch(c) {
      case '\\':
      case '"':
        write('\\');
        write(c);
        break;

      case '/':
        write(c);
        break;

      case '\b':
        write("\\b");
        break;

      case '\t':
        write("\\t");
        break;

      case '\n':
        write("\\n");
        break;

      case '\f':
        write("\\f");
        break;

      case '\r':
        write("\\r");
        break;

      default:
        if(c < '\u0020') {
          String unicode = "000" + Integer.toHexString(c);
          write("\\u");
          write(unicode.substring(unicode.length() - 4));
        }
        else {
          write(c);
        }
      }
    }

    write('"');
  }

  /**
   * Write a string to internal JSON stream. This method is not only convenient but isolate JSON serializer logic from
   * underlying JSON stream.
   * 
   * @param s string to write.
   * @throws IOException if IO write operation fails.
   */
  protected void write(String s) throws IOException
  {
    writer.write(s);
  }

  /**
   * Write a single character to internal JSON stream. This method is not only convenient but isolate JSON serializer
   * logic from underlying JSON stream.
   * 
   * @param c character to write.
   * @throws IOException if IO write operation fails.
   */
  protected void write(char c) throws IOException
  {
    writer.write(c);
  }

  /**
   * Dump circular dependencies stack to error log but leave stack unchanged.
   * 
   * @param value current value object on which circular dependency was discovered.
   */
  protected void dumpCircularDependenciesStack(Object value)
  {
    ListIterator<Object> it = circularDependenciesStack.listIterator(circularDependenciesStack.size());
    StringBuilder dump = new StringBuilder();
    while(it.hasPrevious()) {
      dump.append(Strings.concat("\t- ", it.previous().getClass().getName(), "\r\n"));
    }
    log.error("Circular dependecies on value object |%s|. Set it to null. Stack dump:\r\n%s", value.getClass().getName(), dump.toString());
  }
}
