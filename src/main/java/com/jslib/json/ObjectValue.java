package com.jslib.json;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.converter.Converter;
import com.jslib.lang.BugError;
import com.jslib.lang.GType;
import com.jslib.util.Classes;
import com.jslib.util.Strings;

/**
 * Generic object value handler. This class helps parser to create object instance of proper type and set its fields
 * values.
 * 
 * @author Iulian Rotaru
 */
public class ObjectValue implements Value
{
  /** Class logger. */
  private static final Log log = LogFactory.getLog(ObjectValue.class);

  /** Value converter to and from strings. */
  protected final Converter converter;

  /** Object declaring type. */
  private Type declaringType;

  private Class<?> declaringClass;

  /** Object instance. */
  protected Object instance;

  /**
   * Temporarily store currently working field name. This field name is stored by {@link #setFieldName(String)} and used
   * by {@link #setValue(Object)}. It is caller responsibility to ensure proper setters invocation order.
   */
  private String fieldName;

  /** Inherited constructor just takes care to initialize converter instance. */
  protected ObjectValue(Converter converter)
  {
    this.converter = converter;
  }

  /**
   * Create object value instance.
   * 
   * @param clazz object class.
   */
  public ObjectValue(Converter converter, Type type)
  {
    this(converter);
    this.declaringType = type;
    this.declaringClass = Classes.forType(type);
    this.instance = Classes.newInstance(this.declaringClass);
  }

  /**
   * Get wrapped object instance.
   * 
   * @return object instance.
   */
  @Override
  public Object instance()
  {
    return instance;
  }

  /**
   * Get the type of this object instance.
   * 
   * @return instance type.
   */
  @Override
  public Type getType()
  {
    return declaringType;
  }

  /**
   * Set object instance to null.
   * 
   * @param value unused, always null.
   * @throws UnsupportedOperationException if <code>value</code> is not null.
   */
  @Override
  public void set(Object value) throws UnsupportedOperationException
  {
    if(value != null) {
      throw new UnsupportedOperationException(String.format("Object value setter is not supported. Possible for JSON stream not consistent with type |%s|.", declaringType));
    }
    instance = null;
  }

  /**
   * Get the type of named field or null if field does not exist. This method takes care to resolve type variables as
   * described below.
   * <p>
   * If field type is a class return it as it is.
   * <p>
   * If field is a type variable, e.g. <code>data</code> field from sample code, delegates
   * {@link #resolveTypeVariable(Type, Type)} and return the actual type argument used on class instantiation.
   * 
   * <pre>
   * class Container&lt;T&gt;
   * {
   *   T data;
   * }
   * </pre>
   * <p>
   * If field is a parameterized type, be it object, collection or map, ensure all type arguments are resolved using the
   * same {@link #resolveTypeVariable(Type, Type)} and return a new {@link GType} instance with field raw type and
   * resolved type arguments.
   * 
   * <pre>
   * class Container&lt;T&gt;
   * {
   *   Map<String, T> data;
   * }
   * 
   * Container&lt;Integer&gt; container = new Container&lt;&gt;();
   * </pre>
   * 
   * For above example this method return <code>new GType(Map.class, String.class, Integer.class)</code>. New operator
   * for example is just to demo type argument; on this implementation instances are created reflectively.
   * <p>
   * Implemented solution for parameterized types allows for not limited nesting hierarchy. It is legal to have a grand
   * father, with father and child, all parameterized. At every level this method return <code>GType</code> with type
   * argument initialized from field declaring class type parameters.
   * 
   * @return field type or null.
   */
  public Type getValueType()
  {
    Field field;
    try {
      field = Classes.getFieldEx(declaringClass, fieldName);
    }
    catch(NoSuchFieldException e) {
      return null;
    }

    Type fieldType = field.getGenericType();
    if(fieldType instanceof Class) {
      return fieldType;
    }

    if(fieldType instanceof TypeVariable) {
      // here we have a type variable and need to find out the actual type argument from declaring class
      return resolveTypeVariable(declaringType, fieldType);
    }

    if(fieldType instanceof ParameterizedType) {
      Type[] fieldTypeArguments = ((ParameterizedType)fieldType).getActualTypeArguments();
      for(int i = 0; i < fieldTypeArguments.length; ++i) {
        if(fieldTypeArguments[i] instanceof Class) {
          continue;
        }
        fieldTypeArguments[i] = resolveTypeVariable(declaringType, fieldTypeArguments[i]);
      }
      return new GType(((ParameterizedType)fieldType).getRawType(), fieldTypeArguments);
    }

    throw new BugError("Unsupported type |%s|.", fieldType);
  }

  /**
   * Store the name for currently working field. Given <code>fieldName</code> is not checked for existence but just
   * stored into {@link #fieldName}.
   * 
   * @param fieldName current working field name.
   */
  public final void setFieldName(String fieldName)
  {
    this.fieldName = Strings.toMemberName(fieldName);
  }

  /**
   * Set value for the field identified by {@link #fieldName} stored by a previous call to
   * {@link #setFieldName(String)}. If named field is missing log to debug and abort this setter.
   * <p>
   * This setter uses {@link Classes#getFieldEx(Class, String)} to access declaring class field and benefit from
   * searching on superclass hierarchy too.
   * 
   * @param value field value, null accepted.
   */
  public void setValue(Object value)
  {
    if(fieldName == null) {
      throw new BugError("Field name is not initialized. Please call #setFieldName(String) before invoking this method.");
    }

    try {
      Field field = Classes.getFieldEx(declaringClass, fieldName);

      Type fieldType = field.getGenericType();
      if(fieldType instanceof TypeVariable) {
        fieldType = resolveTypeVariable(declaringType, fieldType);
      }
      Class<?> fieldClass = Classes.forType(fieldType);

      if(value == null && fieldClass.isPrimitive()) {
        log.warn("Attempt to assing null value to primitive field |%s#%s|. Ignore it.", instance.getClass(), field.getName());
        return;
      }
      if(value == null) {
        field.set(instance, null);
      }
      else if(value instanceof String) {
        field.set(instance, converter.asObject((String)value, fieldClass));
      }
      else {
        field.set(instance, value);
      }
    }
    catch(NoSuchFieldException e) {
      log.debug("Missing field |%s| from class |%s|. Ignore JSON value.", fieldName, declaringClass);
    }
    catch(IllegalArgumentException e) {
      log.error("Illegal argument |%s| while trying to set field |%s| from class |%s|.", value.getClass(), fieldName, declaringType);
    }
    catch(IllegalAccessException e) {
      throw new BugError(e);
    }
  }

  // ----------------------------------------------------------------------------------------------

  /**
   * Return actual type argument from declaring class, related to requested type variable. Type variable is mapped to
   * actual type argument by name. In sample below both type parameter and type variable has the same name,
   * <code>T</code>. For the same example this method will return <code>Integer.class</code>.
   * <p>
   * A type variable is a field that has a generic type. In sample we have a class with a type parameter named
   * <code>T</code>. Field <code>data</code> is a type variable since it is not defined when define the class.
   * 
   * <pre>
   * class Container&lt;T&gt;
   * {
   *   T data;
   * }
   * </pre>
   * 
   * Type variable will be resolved when create concrete class from provided type argument. In out case type argument is
   * <code>Integer</code>. After instantiation <code>data</code> field will have type <code>Integer</code>.
   * 
   * <pre>
   * Container&lt;Integer&gt; container = new Container&lt;&gt;();
   * assert container.data instanceof Integer;
   * </pre>
   * 
   * @param declaringType parameterized type of the class declaring given type variable,
   * @param typeVariable type variable, child of declaring class.
   * @return concrete type, class or parameterized type.
   */
  private static Type resolveTypeVariable(Type declaringType, Type typeVariable)
  {
    if(typeVariable instanceof GenericArrayType) {
      typeVariable = ((GenericArrayType)typeVariable).getGenericComponentType();
      // at this point typeVariable is of type TypeVariableImpl; anyway, on Android JVM it is a concrete class
      if(typeVariable instanceof Class) {
        // respond with concrete array type
        return Array.newInstance((Class<?>)typeVariable, 0).getClass();
      }
    }
    if(!(typeVariable instanceof TypeVariable)) {
      throw new BugError("Argument <typeVariable> should be of |%s| type but is |%s|.", TypeVariable.class, typeVariable.getTypeName());
    }
    if(!(declaringType instanceof ParameterizedType)) {
      throw new BugError("Type variable |%s| should be declared in a parameterized class |%s|.", typeVariable, declaringType);
    }

    ParameterizedType parameterizedDeclaringType = (ParameterizedType)declaringType;
    String typeVariableName = typeVariable.getTypeName();

    Class<?> declaringClass = Classes.forType(declaringType);
    TypeVariable<?>[] typeParameters = declaringClass.getTypeParameters();

    Type[] typeArguments = parameterizedDeclaringType.getActualTypeArguments();
    if(typeParameters.length != typeArguments.length) {
      throw new BugError("Inconsistent generic class |%s|. Type parameters count does not match type arguments.", declaringType);
    }

    // next logic assume type parameters and type arguments have the same length and order
    // length is tested above but I do not found yet formal guarantees; anyway unit tests are passing

    Type fieldType = null;
    for(int i = 0; i < typeParameters.length; ++i) {
      if(typeParameters[i].getName().equals(typeVariableName)) {
        fieldType = typeArguments[i];
      }
    }
    if(fieldType == null) {
      throw new BugError("Inconsistent generic class |%s|. Missing type variable |%s|.", declaringType, typeVariableName);
    }
    return fieldType;
  }
}
