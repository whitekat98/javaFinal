package com.jsoniter.spi;

import java.lang.reflect.*;
import java.util.*;

import static java.lang.reflect.Modifier.isTransient;

public class ClassDescriptor {

    private ClassInfo classInfo;
    private Class clazz;
    private Map<String, Type> lookup;
    private ConstructorDescriptor ctor;
    private List<Binding> fields;
    private List<Binding> setters;
    private List<Binding> getters;
    private List<WrapperDescriptor> bindingTypeWrappers;
    private List<Method> keyValueTypeWrappers;
    private List<UnwrapperDescriptor> unwrappers;
    private boolean asExtraForUnknownProperties;
    private Binding onMissingProperties;
    private Binding onExtraProperties;

    public ClassInfo getClassInfo() {
		return classInfo;
	}

	public void setClassInfo(ClassInfo classInfo) {
		this.classInfo = classInfo;
	}

	public Class getClazz() {
		return clazz;
	}

	public void setClazz(Class clazz) {
		this.clazz = clazz;
	}

	public Map<String, Type> getLookup() {
		return lookup;
	}

	public void setLookup(Map<String, Type> lookup) {
		this.lookup = lookup;
	}

	public ConstructorDescriptor getCtor() {
		return ctor;
	}

	public void setCtor(ConstructorDescriptor ctor) {
		this.ctor = ctor;
	}

	public List<Binding> getFields() {
		return fields;
	}

	public void setFields(List<Binding> fields) {
		this.fields = fields;
	}

	public List<Binding> getSetters() {
		return setters;
	}

	public void setSetters(List<Binding> setters) {
		this.setters = setters;
	}

	public List<Binding> getGetters() {
		return getters;
	}

	public void setGetters(List<Binding> getters) {
		this.getters = getters;
	}

	public List<WrapperDescriptor> getBindingTypeWrappers() {
		return bindingTypeWrappers;
	}

	public void setBindingTypeWrappers(List<WrapperDescriptor> bindingTypeWrappers) {
		this.bindingTypeWrappers = bindingTypeWrappers;
	}

	public List<Method> getKeyValueTypeWrappers() {
		return keyValueTypeWrappers;
	}

	public void setKeyValueTypeWrappers(List<Method> keyValueTypeWrappers) {
		this.keyValueTypeWrappers = keyValueTypeWrappers;
	}

	public List<UnwrapperDescriptor> getUnwrappers() {
		return unwrappers;
	}

	public void setUnwrappers(List<UnwrapperDescriptor> unwrappers) {
		this.unwrappers = unwrappers;
	}

	public boolean isAsExtraForUnknownProperties() {
		return asExtraForUnknownProperties;
	}

	public void setAsExtraForUnknownProperties(boolean asExtraForUnknownProperties) {
		this.asExtraForUnknownProperties = asExtraForUnknownProperties;
	}

	public Binding getOnMissingProperties() {
		return onMissingProperties;
	}

	public void setOnMissingProperties(Binding onMissingProperties) {
		this.onMissingProperties = onMissingProperties;
	}

	public Binding getOnExtraProperties() {
		return onExtraProperties;
	}

	public void setOnExtraProperties(Binding onExtraProperties) {
		this.onExtraProperties = onExtraProperties;
	}

	private ClassDescriptor() {
    }

    public static ClassDescriptor getDecodingClassDescriptor(ClassInfo classInfo, boolean includingPrivate) {
        Class clazz = classInfo.clazz;
        Map<String, Type> lookup = collectTypeVariableLookup(classInfo.type);
        ClassDescriptor desc = new ClassDescriptor();

        desc.setClassInfo(classInfo);
        desc.setClazz(clazz);
        desc.setLookup(lookup);
        desc.setCtor(getCtor(clazz));
        desc.setSetters(getSetters(lookup, classInfo, includingPrivate));
        desc.setGetters(new ArrayList<Binding>());
        desc.setFields(getFields(lookup, classInfo, includingPrivate));
        desc.setBindingTypeWrappers(new ArrayList<WrapperDescriptor>());
        desc.setKeyValueTypeWrappers(new ArrayList<Method>());
        desc.setUnwrappers(new ArrayList<UnwrapperDescriptor>());

        for (Extension extension : JsoniterSpi.getExtensions()) {
            extension.updateClassDescriptor(desc);
        }

        for (Binding field : desc.getFields()) {
            if (field.getValueType() instanceof Class) {
                Class valueClazz = (Class) field.getValueType();

                if (valueClazz.isArray()) {
                    field.setValueCanReuse(false);
                    continue;
                }
            }
            field.setValueCanReuse(field.getValueTypeLiteral().nativeType == null);
        }
        decodingDeduplicate(desc);
        if (includingPrivate) {
            if (desc.getCtor().getCtor() != null) {
                desc.getCtor().getCtor().setAccessible(true);
            }
            if (desc.getCtor().getStaticFactory() != null) {
                desc.getCtor().getStaticFactory().setAccessible(true);
            }
            for (WrapperDescriptor setter : desc.getBindingTypeWrappers()) {
                setter.getMethod().setAccessible(true);
            }
        }
        getdeco(includingPrivate, desc);
        
        return desc;
    }
    
    private static void getdeco(boolean includingPrivate, ClassDescriptor desc ) {
    	
    	for (Binding binding : desc.allDecoderBindings()) {
            if (binding.getFromNames() == null) {
                binding.setFromNames(new String[]{binding.getName()});
            }
            if (binding.getField() != null && includingPrivate) {
                binding.getField().setAccessible(true);
            }
            if (binding.getMethod() != null && includingPrivate) {
                binding.getMethod().setAccessible(true);
            }
            if (binding.getDecoder() != null) {
                JsoniterSpi.addNewDecoder(binding.decoderCacheKey(), binding.getDecoder());
            }
        }
    }

    public static ClassDescriptor getEncodingClassDescriptor(ClassInfo classInfo, boolean includingPrivate) {
        Class clazz = classInfo.clazz;
        Map<String, Type> lookup = collectTypeVariableLookup(classInfo.type);
        ClassDescriptor desc = new ClassDescriptor();
        desc.setClassInfo(classInfo);
        desc.setClazz(clazz);
        desc.setLookup(lookup);
        desc.setFields(getFields(lookup, classInfo, includingPrivate));
        desc.setGetters(getGetters(lookup, classInfo, includingPrivate));
        desc.setBindingTypeWrappers(new ArrayList<WrapperDescriptor>());
        desc.setKeyValueTypeWrappers(new ArrayList<Method>());
        desc.setUnwrappers(new ArrayList<UnwrapperDescriptor>());
        for (Extension extension : JsoniterSpi.getExtensions()) {
            extension.updateClassDescriptor(desc);
        }
        encodingDeduplicate(desc);
        for (Binding binding : desc.allEncoderBindings()) {
            if (binding.getToNames() == null) {
                binding.setToNames(new String[]{binding.getName()});
            }
            if (binding.getField() != null && includingPrivate) {
                binding.getField().setAccessible(true);
            }
            if (binding.getMethod() != null && includingPrivate) {
                binding.getMethod().setAccessible(true);
            }
            if (binding.getEncoder() != null) {
                JsoniterSpi.addNewEncoder(binding.encoderCacheKey(), binding.getEncoder());
            }
        }
        return desc;
    }

    private static void decodingDeduplicate(ClassDescriptor desc) {
        HashMap<String, Binding> byFromName = new HashMap<String, Binding>();
        HashMap<String, Binding> byFieldName = new HashMap<String, Binding>();

        for (Binding field : desc.getFields()) {
            for (String fromName : field.getFromNames()) {

                if (byFromName.containsKey(fromName)) {
                    throw new JsonException("field decode from same name: " + fromName);
                }
                byFromName.put(fromName, field);
            }
            byFieldName.put(field.getName(), field);
        }
        ArrayList<Binding> iteratingSetters = new ArrayList<Binding>(desc.getSetters());
        Collections.reverse(iteratingSetters);
        for (Binding setter : iteratingSetters) {
            if (setter.getFromNames().length == 0) {
                continue;
            }
            Binding existing = byFieldName.get(setter.getName());
            if (existing != null) {
                existing.setFromNames(new String[0]);
            }
            deduplicateByFromName(byFromName, setter);
        }
        for (WrapperDescriptor wrapper : desc.getBindingTypeWrappers()) {
            for (Binding param : wrapper.getparameters()) {
                deduplicateByFromName(byFromName, param);
            }
        }

        for (Binding param : desc.getCtor().getParameters()) {

            deduplicateByFromName(byFromName, param);
        }
    }

    private static void deduplicateByFromName(Map<String, Binding> byFromName, Binding setter) {
        for (String fromName : setter.getFromNames()) {
            Binding existing = byFromName.get(fromName);
            if (existing == null) {
                byFromName.put(fromName, setter);
                continue;
            }
            existing.setFromNames(new String[0]);
        }
    }

    private static void encodingDeduplicate(ClassDescriptor desc) {
        HashMap<String, Binding> byToName = new HashMap<String, Binding>();
        HashMap<String, Binding> byFieldName = new HashMap<String, Binding>();
        
        enco(byToName, byFieldName, desc);

        for (Binding getter : new ArrayList<Binding>(desc.getGetters())) {
            if (getter.getToNames().length == 0) {

                continue;
            }
            Binding existing = byFieldName.get(getter.getName());
            if (existing != null) {
                existing.setToNames(new String[0]);
            }
            for (String toName : getter.getToNames()) {
                existing = byToName.get(toName);
                if (existing == null) {
                    byToName.put(toName, getter);
                    continue;
                }
                existing.setToNames(new String[0]);
            }
        }
    }
    
    private static void enco(HashMap<String, Binding> byToName, HashMap<String, Binding> byFieldName, ClassDescriptor desc ) {
    	
    	for (Binding field : desc.getFields()) {
            for (String toName : field.getToNames()) {

                if (byToName.containsKey(toName)) {
                    throw new JsonException("field encode to same name: " + toName);
                }
                byToName.put(toName, field);
            }
            byFieldName.put(field.getName(), field);
        }
    }

    private static ConstructorDescriptor getCtor(Class clazz) {
        ConstructorDescriptor cctor = new ConstructorDescriptor();
        if (JsoniterSpi.canCreate(clazz)) {
            cctor.setObjectFactory(JsoniterSpi.getObjectFactory(clazz));
            return cctor;
        }
        try {
            cctor.setCtor(clazz.getDeclaredConstructor());
        } catch (Exception e) {
            cctor.setCtor(null);
        }
        return cctor;
    }

    private static List<Binding> getFields(Map<String, Type> lookup, ClassInfo classInfo, boolean includingPrivate) {
        ArrayList<Binding> bindings = new ArrayList<Binding>();
        for (Field field : getAllFields(classInfo.clazz)) {
            
            if (includingPrivate) {
                field.setAccessible(true);
            }
            
            Binding binding = createBindingFromField(lookup, classInfo, field);
            if (!includingPrivate && !Modifier.isPublic(field.getModifiers())) {
                binding.setToNames(new String[0]);
                binding.setFromNames(new String[0]);
            }
            if (!includingPrivate && !Modifier.isPublic(field.getType().getModifiers())) {
                binding.setToNames(new String[0]);
                binding.setFromNames(new String[0]);
            }
            bindings.add(binding);
        }
        return bindings;
    }

    private static Binding createBindingFromField(Map<String, Type> lookup, ClassInfo classInfo, Field field) {
        try {
            Binding binding = new Binding(classInfo, lookup, field.getGenericType());
            binding.setFromNames(new String[]{field.getName()});
            binding.setToNames(new String[]{field.getName()});
            binding.setName(field.getName());
            binding.setAnnotations(field.getAnnotations());
            binding.setField(field);
            return binding;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new JsonException("failed to create binding for field: " + field, e);
        }
    }

    private static List<Field> getAllFields(Class clazz) {
        ArrayList<Field> allFields = new ArrayList<Field>();
        Class current = clazz;
        while (current != null) {
            allFields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return allFields;
    }

    private static List<Binding> getSetters(Map<String, Type> lookup, ClassInfo classInfo, boolean includingPrivate) {
        ArrayList<Binding> setters = new ArrayList<Binding>();
        for (Method method : getAllMethods(classInfo.clazz, includingPrivate)) {
        	
        	String methodName = method.getName();
        	Type[] paramTypes = method.getGenericParameterTypes();
        	set(method, includingPrivate);
            
            try {
                String fromName = translateSetterName(methodName);
                Field field = null;                
                field = method.getDeclaringClass().getDeclaredField(fromName);               
                Binding setter = new Binding(classInfo, lookup, paramTypes[0]);
                if (field != null && isTransient(field.getModifiers())) {
                    setter.setFromNames(new String[0]);
                } else {
                    setter.setFromNames(new String[]{fromName});
                }
                setter.setName(fromName);
                setter.setMethod(method);
                setter.setAnnotations(method.getAnnotations());
                setters.add(setter);
            } catch (JsonException e) {
                throw e;
            } catch (Exception e) {
                throw new JsonException("failed to create binding from setter: " + method, e);
            }
        }
        return setters;
    }
    
    private static void set(Method method, boolean includingPrivate ) {
    	
    	
        if (includingPrivate) {
            method.setAccessible(true);
        }
    }

    private static List<Method> getAllMethods(Class clazz, boolean includingPrivate) {
        List<Method> allMethods = Arrays.asList(clazz.getMethods());
        if (includingPrivate) {
            allMethods = new ArrayList<Method>();
            Class current = clazz;
            while (current != null) {
                allMethods.addAll(Arrays.asList(current.getDeclaredMethods()));
                current = current.getSuperclass();
            }
        }
        return allMethods;
    }

    private static String translateSetterName(String methodName) {
        if (!methodName.startsWith("set")) {
            return null;
        }
        String fromName = methodName.substring("set".length());
        char[] fromNameChars = fromName.toCharArray();
        fromNameChars[0] = Character.toLowerCase(fromNameChars[0]);
        fromName = new String(fromNameChars);
        return fromName;
    }

    private static List<Binding> getGetters(Map<String, Type> lookup, ClassInfo classInfo, boolean includingPrivate) {
        ArrayList<Binding> getters = new ArrayList<Binding>();
        for (Method method : getAllMethods(classInfo.clazz, includingPrivate)) {
            
            String methodName = method.getName();
           
            String toName = methodName.substring("get".length());
            char[] toNameChars = toName.toCharArray();
            toNameChars[0] = Character.toLowerCase(toNameChars[0]);
            toName = new String(toNameChars);
            Binding getter = new Binding(classInfo, lookup, method.getGenericReturnType());
            Field field = null;
            try {
                field = method.getDeclaringClass().getDeclaredField(toName);
            } catch (NoSuchFieldException e) {
                // ignore
            }
            if (field != null && isTransient(field.getModifiers())) {
                getter.setToNames(new String[0]);
            } else {
                getter.setToNames(new String[]{toName});
            }
            getter.setName(toName);
            getter.setMethod(method);
            getter.setAnnotations(method.getAnnotations());
            getters.add(getter);
        }
        return getters;
    }
    

    private static Map<String, Type> collectTypeVariableLookup(Type type) {
        HashMap<String, Type> vars = new HashMap<String, Type>();
        if (null == type) {
            return vars;
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            Type[] actualTypeArguments = pType.getActualTypeArguments();
            Class clazz = (Class) pType.getRawType();
            for (int i = 0; i < clazz.getTypeParameters().length; i++) {
                TypeVariable variable = clazz.getTypeParameters()[i];
                vars.put(variable.getName() + "@" + clazz.getCanonicalName(), actualTypeArguments[i]);
            }
            vars.putAll(collectTypeVariableLookup(clazz.getGenericSuperclass()));
            return vars;
        }
        if (type instanceof Class) {
            Class clazz = (Class) type;
            vars.putAll(collectTypeVariableLookup(clazz.getGenericSuperclass()));
            return vars;
        }
        if (type instanceof WildcardType) {
            return vars;
        }
        throw new JsonException("unexpected type: " + type);
    }

    public List<Binding> allBindings() {
        ArrayList<Binding> bindings = new ArrayList<Binding>(8);
        bindings.addAll(getFields());
        if (getSetters() != null) {
            bindings.addAll(getSetters());
        }
        if (getGetters() != null) {
            bindings.addAll(getGetters());
        }

        if (getCtor() != null) {
            bindings.addAll(getCtor().getParameters());

        }
        if (getBindingTypeWrappers() != null) {
            for (WrapperDescriptor setter : getBindingTypeWrappers()) {
            	bindings.addAll(setter.getparameters());
            }
        }
        return bindings;
    }

    public List<Binding> allDecoderBindings() {
        ArrayList<Binding> bindings = new ArrayList<Binding>(8);

        bindings.addAll(getFields());
        bindings.addAll(getSetters());
        if (getCtor() != null) {
            bindings.addAll(getCtor().getParameters());

        }
        for (WrapperDescriptor setter : getBindingTypeWrappers()) {
        	bindings.addAll(setter.getparameters());
        }
        return bindings;
    }


    public List<Binding> allEncoderBindings() {
        ArrayList<Binding> bindings = new ArrayList<Binding>(8);
        bindings.addAll(getFields());
        bindings.addAll(getGetters());
        return bindings;
    }

    public List<EncodeTo> encodeTos() {
        HashMap<String, Integer> previousAppearance = new HashMap<String, Integer>();
        ArrayList<EncodeTo> encodeTos = new ArrayList<EncodeTo>(8);
        collectEncodeTo(encodeTos, getFields(), previousAppearance);
        collectEncodeTo(encodeTos, getGetters(), previousAppearance);
        ArrayList<EncodeTo> removedNulls = new ArrayList<EncodeTo>(encodeTos.size());
        for (EncodeTo encodeTo : encodeTos) {
            if (encodeTo != null) {
                removedNulls.add(encodeTo);
            }
        }
        return removedNulls;
    }

    private void collectEncodeTo(ArrayList<EncodeTo> encodeTos, List<Binding> fields, HashMap<String, Integer> previousAppearance) {
        for (Binding field : fields) {
            for (String toName : field.getToNames()) {
                if (previousAppearance.containsKey(toName)) {
                    encodeTos.set(previousAppearance.get(toName), null);
                }
                previousAppearance.put(toName, encodeTos.size());
                EncodeTo encodeTo = new EncodeTo();
                encodeTo.setBinding(field);
                encodeTo.setToName(toName);
                encodeTos.add(encodeTo);
            }
        }
    }

}
