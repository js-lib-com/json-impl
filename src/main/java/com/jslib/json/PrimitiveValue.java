package com.jslib.json;

import java.lang.reflect.Type;

import com.jslib.converter.Converter;

/**
 * Helper class for primitive value parsing.
 * 
 * @author Iulian Rotaru
 */
public final class PrimitiveValue implements Value
{
  /** Value converter to and from strings. */
  private final Converter converter;

  /** Primitive value class. */
  private final Class<?> clazz;

  /** Primitive value boxing class. */
  private Object instance;

  /**
   * Create primitive value helper for given class.
   * 
   * @param clazz primitive value class.
   */
  public PrimitiveValue(Converter converter, Class<?> clazz)
  {
    this.converter = converter;
    this.clazz = clazz;
  }

  @Override
  public Object instance()
  {
    return instance;
  }

  @Override
  public Type getType()
  {
    return clazz;
  }

  /**
   * Set this primitive value from string value.
   * 
   * @param value string value.
   * @throws JsonParserException if value is not a string.
   */
  @Override
  public void set(Object value)
  {
    if(value == null) {
      instance = null;
      return;
    }
    if(!(value instanceof String)) {
      throw new JsonParserException("Invalid type. Expected java.lang.String but got |%s|", value.getClass());
    }
    instance = converter.asObject((String)value, clazz);
  }
}
