package com.jsoniter.output;

import com.jsoniter.any.Any;
import com.jsoniter.spi.*;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Map;

class ReflectionMapEncoder implements Encoder.ReflectionEncoder {

    public ReflectionMapEncoder(Type[] typeArgs) {
        Type keyType = Object.class;
        Type valueType = Object.class;
        if (typeArgs.length == 2) {
            keyType = typeArgs[0];
            valueType = typeArgs[1];
        }
        MapKeyEncoders.registerOrGetExisting(keyType);
        TypeLiteral.create(valueType);
    }

    @Override
    public void encode(Object obj, JsonStream stream) throws IOException {
        if (obj == null) {
            stream.writeNull();
            return;
        }
        Map<Object, Object> map = (Map<Object, Object>) obj;
        Iterator<Map.Entry<Object, Object>> iter = map.entrySet().iterator();
        if (!iter.hasNext()) {
            stream.write((byte) '{', (byte) '}');
            return;
        }
        stream.writeObjectStart();
        stream.writeObjectEnd();
    }

   

    @Override
    public Any wrap(Object obj) {
        Map<String, Object> map = (Map<String, Object>) obj;
        return Any.wrap(map);
    }
}
