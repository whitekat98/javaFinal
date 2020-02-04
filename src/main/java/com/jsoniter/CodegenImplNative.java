package com.jsoniter;

import com.jsoniter.any.Any;
import com.jsoniter.spi.*;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

class CodegenImplNative {
	
	private CodegenImplNative() {
	    throw new IllegalStateException("Utility class");
	}
	private static final String DEC = "decoder for ";
    static final Map<String, String> NATIVE_READS = new HashMap<String, String>();
    static final Map<Class, Decoder> NATIVE_DECODERS = new HashMap<Class, Decoder>();
    static final String CONST1 = "decoder for ";
    static {
    	NATIVE_READS.put("float", "iter.readFloat()");
    	NATIVE_READS.put("double", "iter.readDouble()");
    	NATIVE_READS.put("boolean", "iter.readBoolean()");
    	NATIVE_READS.put("byte", "iter.readShort()");
    	NATIVE_READS.put("short", "iter.readShort()");
    	NATIVE_READS.put("int", "iter.readInt()");
    	NATIVE_READS.put("char", "iter.readInt()");
    	NATIVE_READS.put("long", "iter.readLong()");
    	NATIVE_READS.put(Float.class.getName(), "(iter.readNull() ? null : java.lang.Float.valueOf(iter.readFloat()))");
    	NATIVE_READS.put(Double.class.getName(), "(iter.readNull() ? null : java.lang.Double.valueOf(iter.readDouble()))");
    	NATIVE_READS.put(Boolean.class.getName(), "(iter.readNull() ? null : java.lang.Boolean.valueOf(iter.readBoolean()))");
    	NATIVE_READS.put(Byte.class.getName(), "(iter.readNull() ? null : java.lang.Byte.valueOf((byte)iter.readShort()))");
    	NATIVE_READS.put(Character.class.getName(), "(iter.readNull() ? null : java.lang.Character.valueOf((char)iter.readShort()))");
    	NATIVE_READS.put(Short.class.getName(), "(iter.readNull() ? null : java.lang.Short.valueOf(iter.readShort()))");
    	NATIVE_READS.put(Integer.class.getName(), "(iter.readNull() ? null : java.lang.Integer.valueOf(iter.readInt()))");
    	NATIVE_READS.put(Long.class.getName(), "(iter.readNull() ? null : java.lang.Long.valueOf(iter.readLong()))");
    	NATIVE_READS.put(BigDecimal.class.getName(), "iter.readBigDecimal()");
    	NATIVE_READS.put(BigInteger.class.getName(), "iter.readBigInteger()");
    	NATIVE_READS.put(String.class.getName(), "iter.readString()");
    	NATIVE_READS.put(Object.class.getName(), "iter.read()");
    	NATIVE_READS.put(Any.class.getName(), "iter.readAny()");
    }
    static {
    	NATIVE_DECODERS.put(float.class, new Decoder() {
            @Override
            public Object decode(JsonIterator iter) throws IOException {
                return iter.readFloat();
            }
        });
    	NATIVE_DECODERS.put(Float.class, new Decoder() {
            @Override
            public Object decode(JsonIterator iter) throws IOException {
                return iter.readNull() ? null : iter.readFloat();
            }
        });
    	NATIVE_DECODERS.put(double.class, new Decoder() {
            @Override
            public Object decode(JsonIterator iter) throws IOException {
                return iter.readDouble();
            }
        });
    	NATIVE_DECODERS.put(Double.class, new Decoder() {
            @Override
            public Object decode(JsonIterator iter) throws IOException {
                return iter.readNull() ? null : iter.readDouble();
            }
        });
    	NATIVE_DECODERS.put(boolean.class, new Decoder() {
            @Override
            public Object decode(JsonIterator iter) throws IOException {
                return iter.readBoolean();
            }
        });
    	NATIVE_DECODERS.put(Boolean.class, new Decoder() {
            @Override
            public Object decode(JsonIterator iter) throws IOException {
                return iter.readNull() ? null : iter.readBoolean();
            }
        });
    	NATIVE_DECODERS.put(byte.class, new Decoder() {
            @Override
            public Object decode(JsonIterator iter) throws IOException {
                return Byte.valueOf((byte) iter.readShort());
            }
        });
    	NATIVE_DECODERS.put(Byte.class, new Decoder() {
            @Override
            public Object decode(JsonIterator iter) throws IOException {
                return iter.readNull() ? null : (byte)iter.readShort();
            }
        });
    	NATIVE_DECODERS.put(short.class, new Decoder() {
            @Override
            public Object decode(JsonIterator iter) throws IOException {
                return iter.readShort();
            }
        });
    	NATIVE_DECODERS.put(Short.class, new Decoder() {
            @Override
            public Object decode(JsonIterator iter) throws IOException {
                return iter.readNull() ? null : iter.readShort();
            }
        });
    	NATIVE_DECODERS.put(int.class, new Decoder() {
            @Override
            public Object decode(JsonIterator iter) throws IOException {
                return iter.readInt();
            }
        });
    	NATIVE_DECODERS.put(Integer.class, new Decoder() {
            @Override
            public Object decode(JsonIterator iter) throws IOException {
                return iter.readNull() ? null : iter.readInt();
            }
        });
    	NATIVE_DECODERS.put(char.class, new Decoder() {
            @Override
            public Object decode(JsonIterator iter) throws IOException {
                return (char)iter.readInt();
            }
        });
    	NATIVE_DECODERS.put(Character.class, new Decoder() {
            @Override
            public Object decode(JsonIterator iter) throws IOException {
                return iter.readNull() ? null : (char)iter.readInt();
            }
        });
    	NATIVE_DECODERS.put(long.class, new Decoder() {
            @Override
            public Object decode(JsonIterator iter) throws IOException {
                return iter.readLong();
            }
        });
    	NATIVE_DECODERS.put(Long.class, new Decoder() {
            @Override
            public Object decode(JsonIterator iter) throws IOException {
                return iter.readNull() ? null : iter.readLong();
            }
        });
    	NATIVE_DECODERS.put(BigDecimal.class, new Decoder() {
            @Override
            public Object decode(JsonIterator iter) throws IOException {
                return iter.readBigDecimal();
            }
        });
    	NATIVE_DECODERS.put(BigInteger.class, new Decoder() {
            @Override
            public Object decode(JsonIterator iter) throws IOException {
                return iter.readBigInteger();
            }
        });
    	NATIVE_DECODERS.put(String.class, new Decoder() {
            @Override
            public Object decode(JsonIterator iter) throws IOException {
                return iter.readString();
            }
        });
    	NATIVE_DECODERS.put(Object.class, new Decoder() {
            @Override
            public Object decode(JsonIterator iter) throws IOException {
                return iter.read();
            }
        });
    	NATIVE_DECODERS.put(Any.class, new Decoder() {
            @Override
            public Object decode(JsonIterator iter) throws IOException {
                return iter.readAny();
            }
        });
    }

    public static String genReadOp(Type type) {
        String cacheKey = TypeLiteral.create(type).getDecoderCacheKey();
        return String.format("(%s)%s", getTypeName(type), genReadOp(cacheKey, type));
    }

    public static String getTypeName(Type fieldType) {
    	
    	if (fieldType instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) fieldType;
            Class clazz = (Class) pType.getRawType();
            return clazz.getCanonicalName();
        } else if (fieldType instanceof Class) {
            Class clazz = (Class) fieldType;
            return clazz.getCanonicalName();
        } else if (fieldType instanceof WildcardType) {
            return Object.class.getCanonicalName();
        } else {
            throw new JsonException("unsupported type: " + fieldType);
        }
    }

    static String genField(Binding field) {
        String fieldCacheKey = field.decoderCacheKey();
        Type fieldType = field.getValueType();
        return String.format("(%s)%s", getTypeName(fieldType), genReadOp(fieldCacheKey, fieldType));

    }

    private static String genReadOp(String cacheKey, Type valueType) {
        // the field decoder might be registered directly
        Decoder decoder = JsoniterSpi.getDecoder(cacheKey);
        if (decoder == null) {
            // if cache key is for field, and there is no field decoder specified
            // update cache key for normal type
            cacheKey = TypeLiteral.create(valueType).getDecoderCacheKey();
            decoder = JsoniterSpi.getDecoder(cacheKey);
            if (decoder == null) {
                if (valueType instanceof Class) {
                    Class clazz = (Class) valueType;
                    String nativeRead = NATIVE_READS.get(clazz.getCanonicalName());
                    if (nativeRead != null) {
                        return nativeRead;
                    }
                } else if (valueType instanceof WildcardType) {
                    return NATIVE_READS.get(Object.class.getCanonicalName());
                }
                Codegen.getDecoder(cacheKey, valueType);
                if (Codegen.canStaticAccess(cacheKey)) {
                    return String.format("%s.decode_(iter)", cacheKey);
                } else {
                    // can not use static "decode_" method to access, go through codegen cache
                    return String.format("com.jsoniter.CodegenAccess.read(\"%s\", iter)", cacheKey);
                }
            }
        }
        
        gen(cacheKey, valueType, decoder);
        read(cacheKey, valueType, decoder);
        
        return String.format("com.jsoniter.CodegenAccess.read(\"%s\", iter)", cacheKey);
    }
    
    private static String gen(String cacheKey, Type valueType, Decoder decoder) {
    	
    	
    	if (valueType == boolean.class) {
            if (!(decoder instanceof Decoder.BooleanDecoder)) {
                throw new JsonException( DEC + cacheKey + "must implement Decoder.BooleanDecoder");
            }
            return String.format("com.jsoniter.CodegenAccess.readBoolean(\"%s\", iter)", cacheKey);
        }
        if (valueType == byte.class) {
            if (!(decoder instanceof Decoder.ShortDecoder)) {
                throw new JsonException(DEC + cacheKey + "must implement Decoder.ShortDecoder");
            }
            return String.format("com.jsoniter.CodegenAccess.readShort(\"%s\", iter)", cacheKey);
        }
        if (valueType == short.class) {
            if (!(decoder instanceof Decoder.ShortDecoder)) {
                throw new JsonException(DEC + cacheKey + "must implement Decoder.ShortDecoder");
            }
            return String.format("com.jsoniter.CodegenAccess.readShort(\"%s\", iter)", cacheKey);
        }
        if (valueType == char.class) {
            if (!(decoder instanceof Decoder.IntDecoder)) {
                throw new JsonException(DEC + cacheKey + "must implement Decoder.IntDecoder");
            }
            return String.format("com.jsoniter.CodegenAccess.readInt(\"%s\", iter)", cacheKey);
        }
		return cacheKey;
    }
    
    private static String read(String cacheKey, Type valueType, Decoder decoder) {
    	
    	if (valueType == int.class) {
            if (!(decoder instanceof Decoder.IntDecoder)) {
                throw new JsonException(DEC + cacheKey + "must implement Decoder.IntDecoder");
            }
            return String.format("com.jsoniter.CodegenAccess.readInt(\"%s\", iter)", cacheKey);
        }
        if (valueType == long.class) {
            if (!(decoder instanceof Decoder.LongDecoder)) {
                throw new JsonException(DEC + cacheKey + "must implement Decoder.LongDecoder");
            }
            return String.format("com.jsoniter.CodegenAccess.readLong(\"%s\", iter)", cacheKey);
        }
        if (valueType == float.class) {
            if (!(decoder instanceof Decoder.FloatDecoder)) {
                throw new JsonException(DEC + cacheKey + "must implement Decoder.FloatDecoder");
            }
            return String.format("com.jsoniter.CodegenAccess.readFloat(\"%s\", iter)", cacheKey);
        }
        if (valueType == double.class) {
            if (!(decoder instanceof Decoder.DoubleDecoder)) {
                throw new JsonException(DEC + cacheKey + "must implement Decoder.DoubleDecoder");
            }
            return String.format("com.jsoniter.CodegenAccess.readDouble(\"%s\", iter)", cacheKey);
        }
		return cacheKey;
    }
}
