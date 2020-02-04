package com.jsoniter.output;

import com.jsoniter.spi.ClassInfo;
import com.jsoniter.spi.Encoder;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ReflectionEncoderFactory {
	
	private ReflectionEncoderFactory () {
	    throw new IllegalStateException("Utility class");
	}

    public static Encoder.ReflectionEncoder create(ClassInfo classInfo) {
        Class clazz = classInfo.clazz;
        Type[] typeArgs = classInfo.typeArgs;
        if (clazz.isArray()) {
            return new ReflectionArrayEncoder(clazz);
        }
        if (List.class.isAssignableFrom(clazz)) {
            return new ReflectionListEncoder(typeArgs);
        }
        if (Collection.class.isAssignableFrom(clazz)) {
            return new ReflectionCollectionEncoder(typeArgs);
        }
        if (Map.class.isAssignableFrom(clazz)) {
            return new ReflectionMapEncoder(typeArgs);
        }
        if (clazz.isEnum()) {
            return new ReflectionEnumEncoder(clazz);
        }
        return new ReflectionObjectEncoder(classInfo);
    }
}
