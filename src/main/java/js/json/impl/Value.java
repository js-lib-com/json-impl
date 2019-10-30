package js.json.impl;

import java.lang.reflect.Type;

/**
 * Parser values handlers. From parser perspective a JSON stream is a sequence of named or indexed values. These values
 * should be injected on object instances that are created on the fly.
 * <p>
 * First, {@link Parser} uses {@link #instance()} method to retrieve wrapped value then initialize it from JSON stream
 * using {@link #set(Object)}.
 * 
 * @author Iulian Rotaru
 */
public interface Value
{
  /**
   * Retrieve Java object / array / collection instance wrapped this this parser value.
   * 
   * @return value instance.
   */
  Object instance();

  /**
   * Get Java object type or array / collection component type.
   * 
   * @return value type.
   */
  Type getType();

  /**
   * Value setter or array / collection items collector. For primitive value this method is called once with parsed
   * value whereas for array or collection this method is called iteratively for every parsed element.
   * 
   * @param value value to set / collect.
   */
  void set(Object value);
}
