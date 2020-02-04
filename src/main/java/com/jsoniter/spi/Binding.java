package com.jsoniter.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.Map;

public class Binding {
    // input

    public final Class clazz;
    public final TypeLiteral clazzTypeLiteral;
    private Annotation[] annotations;
    private Field field; // obj.XXX
    private Method method; // obj.setXXX() or obj.getXXX()
    private boolean valueCanReuse;

    // input/output
    private String name;
    private Type valueType;
    private TypeLiteral valueTypeLiteral;
    // output
    private String[] fromNames; // for decoder
    private String[] toNames; // for encoder
    private Decoder decoder;
    private Encoder encoder;
    private boolean asMissingWhenNotPresent;
    private boolean asExtraWhenPresent;
    private boolean isNullable = true;
    private boolean isCollectionValueNullable = true;
    private OmitValue defaultValueToOmit;
    // then this property will not be unknown
    // but we do not want to bind it anywhere
    private boolean shouldSkip;
    // attachment, used when generating code or reflection
    private int idx;
    private long mask;

    public Annotation[] getAnnotations() {
		return annotations;
	}

	public void setAnnotations(Annotation[] annotations) {
		this.annotations = annotations;
	}

	public Field getField() {
		return field;
	}

	public void setField(Field field) {
		this.field = field;
	}
	public String[] getFromNames() {
		return fromNames;
	}

	public void setFromNames(String[] fromNames) {
		this.fromNames = fromNames;
	}

	public Decoder getDecoder() {
		return decoder;
	}

	public void setDecoder(Decoder decoder) {
		this.decoder = decoder;
	}

	public Encoder getEncoder() {
		return encoder;
	}

	public void setEncoder(Encoder encoder) {
		this.encoder = encoder;
	}

	public boolean isAsMissingWhenNotPresent() {
		return asMissingWhenNotPresent;
	}

	public void setAsMissingWhenNotPresent(boolean asMissingWhenNotPresent) {
		this.asMissingWhenNotPresent = asMissingWhenNotPresent;
	}

	public boolean isAsExtraWhenPresent() {
		return asExtraWhenPresent;
	}

	public void setAsExtraWhenPresent(boolean asExtraWhenPresent) {
		this.asExtraWhenPresent = asExtraWhenPresent;
	}

	public boolean isNullable() {
		return isNullable;
	}

	public void setNullable(boolean isNullable) {
		this.isNullable = isNullable;
	}

	public boolean isCollectionValueNullable() {
		return isCollectionValueNullable;
	}

	public void setCollectionValueNullable(boolean isCollectionValueNullable) {
		this.isCollectionValueNullable = isCollectionValueNullable;
	}

	public OmitValue getDefaultValueToOmit() {
		return defaultValueToOmit;
	}

	public void setDefaultValueToOmit(OmitValue defaultValueToOmit) {
		this.defaultValueToOmit = defaultValueToOmit;
	}

	public int getIdx() {
		return idx;
	}

	public void setIdx(int idx) {
		this.idx = idx;
	}

	public long getMask() {
		return mask;
	}

	public void setMask(long mask) {
		this.mask = mask;
	}

	public Class getClazz() {
		return clazz;
	}

	public TypeLiteral getClazzTypeLiteral() {
		return clazzTypeLiteral;
	}

	public Binding(ClassInfo classInfo, Map<String, Type> lookup, Type valueType) {
        this.clazz = classInfo.clazz;
        this.clazzTypeLiteral = TypeLiteral.create(classInfo.type);
        this.setValueType(substituteTypeVariables(lookup, valueType));
        this.setValueTypeLiteral(TypeLiteral.create(this.getValueType()));
    }

    public String decoderCacheKey() {
        return this.getName() + "@" + this.clazzTypeLiteral.getDecoderCacheKey();
    }

    public String encoderCacheKey() {
        return this.getName() + "@" + this.clazzTypeLiteral.getEncoderCacheKey();
    }

    private static Type substituteTypeVariables(Map<String, Type> lookup, Type type) {
        if (type instanceof TypeVariable) {
            return translateTypeVariable(lookup, (TypeVariable) type);
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            Type[] args = pType.getActualTypeArguments();
            for (int i = 0; i < args.length; i++) {
                args[i] = substituteTypeVariables(lookup, args[i]);
            }
            return GenericsHelper.createParameterizedType(args, pType.getOwnerType(), pType.getRawType());
        }
        if (type instanceof GenericArrayType) {
            GenericArrayType gaType = (GenericArrayType) type;
            Type componentType = substituteTypeVariables(lookup, gaType.getGenericComponentType());
            return GenericsHelper.createGenericArrayType(componentType);
        }
        return type;
    }

    private static Type translateTypeVariable(Map<String, Type> lookup, TypeVariable var) {
        GenericDeclaration declaredBy = var.getGenericDeclaration();
        if (!(declaredBy instanceof Class)) {
            // if the <T> is not defined by class, there is no way to get the actual type
            return Object.class;
        }
        Class clazz = (Class) declaredBy;
        Type actualType = lookup.get(var.getName() + "@" + clazz.getCanonicalName());
        if (actualType == null) {
            // should not happen
            return Object.class;
        }
        if (actualType instanceof TypeVariable) {
            // translate to another variable, try again
            return translateTypeVariable(lookup, (TypeVariable) actualType);
        }
        return actualType;
    }

    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        if (getAnnotations() == null) {
            return null;
        }
        for (Annotation annotation : getAnnotations()) {
            if (annotationClass.isAssignableFrom(annotation.getClass())) {
                return (T) annotation;
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Binding binding = (Binding) o;

        if (clazz != null ? !clazz.equals(binding.clazz) : binding.clazz != null) return false;
        if (getMethod() != null ? !getMethod().equals(binding.getMethod()) : binding.getMethod() != null) return false;
        return getName() != null ? getName().equals(binding.getName()) : binding.getName() == null;
    }

    @Override
    public int hashCode() {
        int result = clazz != null ? clazz.hashCode() : 0;
        result = 31 * result + (getMethod() != null ? getMethod().hashCode() : 0);
        result = 31 * result + (getName() != null ? getName().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Binding{" +
                "clazz=" + clazz +
                ", name='" + getName() + '\'' +
                ", valueType=" + getValueType() +
                '}';
    }


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Method getMethod() {
		return method;
	}

	public void setMethod(Method method) {
		this.method = method;
	}

	public Type getValueType() {
		return valueType;
	}

	public void setValueType(Type valueType) {
		this.valueType = valueType;
	}

	public boolean isValueCanReuse() {
		return valueCanReuse;
	}

	public void setValueCanReuse(boolean valueCanReuse) {
		this.valueCanReuse = valueCanReuse;
	}


	public String[] getToNames() {
		return toNames;
	}

	public void setToNames(String[] toNames) {
		this.toNames = toNames;
	}

	public TypeLiteral getValueTypeLiteral() {
		return valueTypeLiteral;
	}

	public void setValueTypeLiteral(TypeLiteral valueTypeLiteral) {
		this.valueTypeLiteral = valueTypeLiteral;
	}

	public boolean isShouldSkip() {
		return shouldSkip;
	}

	public void setShouldSkip(boolean shouldSkip) {
		this.shouldSkip = shouldSkip;
	}

}
