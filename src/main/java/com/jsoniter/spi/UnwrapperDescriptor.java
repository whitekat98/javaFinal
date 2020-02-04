package com.jsoniter.spi;

import com.jsoniter.output.JsonStream;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

public class UnwrapperDescriptor {

    private Method method;

    private boolean isMap;

    private TypeLiteral mapValueTypeLiteral;

    public UnwrapperDescriptor(Method method) {
        this.setMethod(method);
        if (isMapUnwrapper(method)) {
            this.setMap(true);
            Type mapType = method.getGenericReturnType();
            setMapValueTypeLiteral(TypeLiteral.create(Object.class));
            if (mapType instanceof ParameterizedType) {
                ParameterizedType pType = (ParameterizedType) mapType;
                Type[] typeArgs = pType.getActualTypeArguments();
                if (typeArgs.length == 2) {
                    setMapValueTypeLiteral(TypeLiteral.create(typeArgs[1]));
                }
            }
        } else if (isStreamUnwrapper(method)) {
            this.setMap(false);
        } else {
            throw new JsonException("invalid unwrapper method signature: " + method);
        }
    }

    private boolean isMapUnwrapper(Method method) {
        if (method.getParameterTypes().length != 0) {
            return false;
        }
        return Map.class.isAssignableFrom(method.getReturnType());
    }

    private boolean isStreamUnwrapper(Method method) {
        if (method.getReturnType() != void.class) {
            return false;
        }
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length != 1) {
            return false;
        }
        return parameterTypes[0] == JsonStream.class;
    }

	public boolean isMap() {
		return isMap;
	}

	public void setMap(boolean isMap) {
		this.isMap = isMap;
	}

	public Method getMethod() {
		return method;
	}

	public void setMethod(Method method) {
		this.method = method;
	}

	public TypeLiteral getMapValueTypeLiteral() {
		return mapValueTypeLiteral;
	}

	public void setMapValueTypeLiteral(TypeLiteral mapValueTypeLiteral) {
		this.mapValueTypeLiteral = mapValueTypeLiteral;
	}
}
