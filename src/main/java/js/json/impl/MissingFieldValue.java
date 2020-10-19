package js.json.impl;

import java.lang.reflect.Type;

import js.converter.Converter;

/**
 * NOP implementation for missing fields. This helper is a special object value helper used by parser when a property from JSON
 * stream has no related field on target object.
 * 
 * @author Iulian Rotaru
 */
public class MissingFieldValue extends ObjectValue {
	public MissingFieldValue(Converter converter) {
		super(converter);
	}

	@Override
	public Object instance() {
		return null;
	}

	@Override
	public Type getType() {
		return null;
	}

	@Override
	public Type getValueType() {
		return null;
	}

	@Override
	public void set(Object value) {
	}

	@Override
	public void setValue(Object value) {
	}
}
