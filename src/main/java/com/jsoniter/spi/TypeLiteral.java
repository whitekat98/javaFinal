package com.jsoniter.spi;

import com.jsoniter.any.Any;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class TypeLiteral {

	private static final String CON = "_array";
	
    public enum NativeType {
        FLOAT,
        DOUBLE,
        BOOLEAN,
        BYTE,
        SHORT,
        INT,
        CHAR,
        LONG,
        BIG_DECIMAL,
        BIG_INTEGER,
        STRING,
        OBJECT,
        ANY,
    }

    private static final Map<Type, NativeType> nativeTypes = new HashMap<Type, NativeType>();
    static {
    	nativeTypes.put(float.class, NativeType.FLOAT);
    	nativeTypes.put(Float.class, NativeType.FLOAT);
    	nativeTypes.put(double.class, NativeType.DOUBLE);
    	nativeTypes.put(Double.class, NativeType.DOUBLE);
    	nativeTypes.put(boolean.class, NativeType.BOOLEAN);
    	nativeTypes.put(Boolean.class, NativeType.BOOLEAN);
    	nativeTypes.put(byte.class, NativeType.BYTE);
    	nativeTypes.put(Byte.class, NativeType.BYTE);
    	nativeTypes.put(short.class, NativeType.SHORT);
    	nativeTypes.put(Short.class, NativeType.SHORT);
    	nativeTypes.put(int.class, NativeType.INT);
    	nativeTypes.put(Integer.class, NativeType.INT);
    	nativeTypes.put(char.class, NativeType.CHAR);
    	nativeTypes.put(Character.class, NativeType.CHAR);
    	nativeTypes.put(long.class, NativeType.LONG);
    	nativeTypes.put(Long.class, NativeType.LONG);
    	nativeTypes.put(BigDecimal.class, NativeType.BIG_DECIMAL);
    	nativeTypes.put(BigInteger.class, NativeType.BIG_INTEGER);
    	nativeTypes.put(String.class, NativeType.STRING);
    	nativeTypes.put(Object.class, NativeType.OBJECT);
    	nativeTypes.put(Any.class, NativeType.ANY);
    }

    private static Map<Type, TypeLiteral> typeLiteralCache = new HashMap<Type, TypeLiteral>();
    final Type type;
    final String decoderCacheKey;
    final String encoderCacheKey;
    // TODO: remove native type
    final NativeType nativeType;

    /**
     * Constructs a new type literal. Derives represented class from type parameter.
     * Clients create an empty anonymous subclass. Doing so embeds the type parameter in the
     * anonymous class's type hierarchy so we can reconstitute it at runtime despite erasure.
     */
    @SuppressWarnings("unchecked")
    protected TypeLiteral() {
        this.type = getSuperclassTypeParameter(getClass());
        nativeType = nativeTypes.get(this.type);
        decoderCacheKey = generateDecoderCacheKey(type);
        encoderCacheKey = generateEncoderCacheKey(type);
    }

    public TypeLiteral(Type type, String decoderCacheKey, String encoderCacheKey) {
        this.type = type;
        nativeType = nativeTypes.get(this.type);
        this.decoderCacheKey = decoderCacheKey;
        this.encoderCacheKey = encoderCacheKey;
    }

    private static String generateDecoderCacheKey(Type type) {
        return generateCacheKey(type, "decoder.");
    }

    private static String generateEncoderCacheKey(Type type) {
        return generateCacheKey(type, "encoder.");
    }

    private static String generateCacheKey(Type type, String prefix) {
        StringBuilder decoderClassName = new StringBuilder(prefix);
        if (type instanceof Class) {
            Class clazz = (Class) type;
            if (clazz.isAnonymousClass()) {
                throw new JsonException("anonymous class not supported: " + clazz);
            }
            if (clazz.isArray()) {
                decoderClassName.append(clazz.getCanonicalName().replace("[]", CON));
            } else {
                // for nested class $
                decoderClassName.append(clazz.getName().replace("[]", CON));
            }
        } else if (type instanceof ParameterizedType) {
            try {
                ParameterizedType pType = (ParameterizedType) type;
                Class clazz = (Class) pType.getRawType();
                decoderClassName.append(clazz.getCanonicalName().replace("[]", CON));
                for (int i = 0; i < pType.getActualTypeArguments().length; i++) {
                    String typeName = formatTypeWithoutSpecialCharacter(pType.getActualTypeArguments()[i]);
                    decoderClassName.append('_');
                    decoderClassName.append(typeName);
                }
            } catch (JsonException e) {
                throw e;
            } catch (Exception e) {
                throw new JsonException("failed to generate cache key for: " + type, e);
            }
        }  else if (type instanceof WildcardType) {
            decoderClassName.append(Object.class.getName());
        } else {
            throw new UnsupportedOperationException("do not know how to handle: " + type);
        }
        return decoderClassName.toString().replace("$", "_");
    }

    private static String formatTypeWithoutSpecialCharacter(Type type) {
        if (type instanceof Class) {
            Class clazz = (Class) type;
            return clazz.getCanonicalName();
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            String typeName = "";
            StringBuilder bld = new StringBuilder();
            for (Type typeArg : pType.getActualTypeArguments()) {
            	bld.append("_");
            	bld.append(formatTypeWithoutSpecialCharacter(typeArg));
            }
            typeName = bld.toString();
            return typeName;
        }
        if (type instanceof GenericArrayType) {
            GenericArrayType gaType = (GenericArrayType) type;
            return formatTypeWithoutSpecialCharacter(gaType.getGenericComponentType()) + CON;
        }
        if (type instanceof WildcardType) {
            return Object.class.getCanonicalName();
        }
        throw new JsonException("unsupported type: " + type + ", of class " + type.getClass());
    }

    static Type getSuperclassTypeParameter(Class<?> subclass) {
        Type superclass = subclass.getGenericSuperclass();
        if (superclass instanceof Class) {
            throw new JsonException("Missing type parameter.");
        }
        ParameterizedType parameterized = (ParameterizedType) superclass;
        return parameterized.getActualTypeArguments()[0];
    }

    public static TypeLiteral create(Type valueType) {
        TypeLiteral typeLiteral = typeLiteralCache.get(valueType);
        if (typeLiteral != null) {
            return typeLiteral;
        }
        return createNew(valueType);
    }

    private static synchronized TypeLiteral createNew(Type valueType) {
        TypeLiteral typeLiteral = typeLiteralCache.get(valueType);
        if (typeLiteral != null) {
            return typeLiteral;
        }
        HashMap<Type, TypeLiteral> copy = new HashMap<Type, TypeLiteral>(typeLiteralCache);
        typeLiteral = new TypeLiteral(valueType,
                generateDecoderCacheKey(valueType),
                generateEncoderCacheKey(valueType));
        copy.put(valueType, typeLiteral);
        typeLiteralCache = copy;
        return typeLiteral;
    }

    public Type getType() {
        return type;
    }

    public String getDecoderCacheKey() {
        return getDecoderCacheKey(JsoniterSpi.getCurrentConfig().configName());
    }

    public String getDecoderCacheKey(String configName) {
        return configName + decoderCacheKey;
    }

    public String getEncoderCacheKey() {
        return getEncoderCacheKey(JsoniterSpi.getCurrentConfig().configName());
    }

    public String getEncoderCacheKey(String configName) {
        return configName + encoderCacheKey;
    }

    public NativeType getNativeType() {
        return nativeType;
    }

    @Override
    public String toString() {
        return "TypeLiteral{" +
                "type=" + type +
                ", decoderCacheKey='" + decoderCacheKey + '\'' +
                ", encoderCacheKey='" + encoderCacheKey + '\'' +
                '}';
    }
}