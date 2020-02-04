package com.jsoniter.spi;

import java.lang.reflect.Type;

import com.jsoniter.annotation.JsonProperty;

public class EmptyExtension implements Extension {

    @Override
    public Type chooseImplementation(Type type) {
        return type;
    }

    @Override
    public boolean canCreate(Class clazz) {
        return false;
    }

    @Override
    public Object create(Class clazz) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Decoder createDecoder(String cacheKey, Type type) {
        return null;
    }

    @Override
    public Encoder createEncoder(String cacheKey, Type type) {
        return null;
    }

    @Override
    public void updateClassDescriptor(ClassDescriptor desc) {
    	throw new UnsupportedOperationException();
    }

	protected JsonProperty getJsonProperty() {
		// TODO Auto-generated method stub
		return null;
	}
}
