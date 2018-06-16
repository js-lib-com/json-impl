package js.json.impl;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

import js.converter.Converter;
import js.util.Classes;

/**
 * Parser collection value.
 * 
 * @author Iulian Rotaru
 */
final class CollectionValue extends Value {
	/** String converter to/from object. */
	private Converter converter;

	/** Collection instance. */
	private Collection<Object> instance;

	/** The actual type of collection parameterized type. */
	private Type type;

	/**
	 * Construct parser collection with elements of given type.
	 * 
	 * @param type collection elements type.
	 */
	CollectionValue(Converter converter, Type type) {
		this.converter = converter;

		if (!(type instanceof ParameterizedType)) {
			throw new JsonParserException("This JSON parser mandates generic collections usage but got |%s|.", type);
		}

		ParameterizedType parameterizedType = (ParameterizedType) type;
		this.instance = Classes.newCollection(parameterizedType.getRawType());
		this.type = parameterizedType.getActualTypeArguments()[0];
	}

	/**
	 * Get collection instance. Since this method is invoked after {@link #set(Object)} returned collection is initialized from
	 * JSON characters stream.
	 * 
	 * @return collection instance.
	 */
	@Override
	public Object instance() {
		return instance;
	}

	/**
	 * Get the actual type of this collection parameterized type.
	 * 
	 * @return collection component type.
	 */
	@Override
	public Type getType() {
		return type;
	}

	/**
	 * Collect parsed item from JSON characters stream.
	 * 
	 * @param value parsed collection item.
	 */
	@Override
	public void set(Object value) {
		if (value == null) {
			instance.add(null);
		} else if (value instanceof String) {
			if (!(type instanceof Class)) {
				throw new IllegalStateException(String.format("Expect primitive value as String but got type |%s| is parameterized.", type));
			}
			instance.add(converter.asObject((String) value, (Class<?>) type));
		} else {
			instance.add(value);
		}
	}
}
