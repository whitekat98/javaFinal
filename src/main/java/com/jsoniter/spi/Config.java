package com.jsoniter.spi;

import com.jsoniter.annotation.*;
import com.jsoniter.output.EncodingMode;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

public class Config extends EmptyExtension {

    private final String configName;
    private final ReBuilder builder;
    private static Map<String, Config> configs = new HashMap<String, Config>();
    private  Map<Type, String> decoderCacheKeys = new HashMap<Type, String>();
    private  Map<Type, String> encoderCacheKeys = new HashMap<Type, String>();
    private static final Map<Class, OmitValue> primitiveOmitValues = new HashMap<Class, OmitValue>();
    static {
    	primitiveOmitValues.put(boolean.class, new OmitValue.False());
    	primitiveOmitValues.put(char.class, new OmitValue.ZeroChar());
    	primitiveOmitValues.put(byte.class, new OmitValue.ZeroByte());
    	primitiveOmitValues.put(short.class, new OmitValue.ZeroShort());
    	primitiveOmitValues.put(int.class, new OmitValue.ZeroInt());
    	primitiveOmitValues.put(long.class, new OmitValue.ZeroLong());
    	primitiveOmitValues.put(float.class, new OmitValue.ZeroFloat());
    	primitiveOmitValues.put(double.class, new OmitValue.ZeroDouble());
    }

    protected Config(String configName, ReBuilder builder) {
        this.configName = configName;
        this.builder = builder;
    }

    public String configName() {
        return configName;
    }

    public String getDecoderCacheKey(Type type) {
        String cacheKey = decoderCacheKeys.get(type);
        if (cacheKey != null) {
            return cacheKey;
        }
        synchronized (this) {
            cacheKey = decoderCacheKeys.get(type);
            if (cacheKey != null) {
                return cacheKey;
            }
            cacheKey = TypeLiteral.create(type).getDecoderCacheKey(configName);
            HashMap<Type, String> newCache = new HashMap<Type, String>(decoderCacheKeys);
            newCache.put(type, cacheKey);
            decoderCacheKeys = newCache;
            return cacheKey;
        }
    }

    public String getEncoderCacheKey(Type type) {
        String cacheKey = encoderCacheKeys.get(type);
        if (cacheKey != null) {
            return cacheKey;
        }
        synchronized (this) {
            cacheKey = encoderCacheKeys.get(type);
            if (cacheKey != null) {
                return cacheKey;
            }
            cacheKey = TypeLiteral.create(type).getEncoderCacheKey(configName);
            HashMap<Type, String> newCache = new HashMap<Type, String>(encoderCacheKeys);
            newCache.put(type, cacheKey);
            encoderCacheKeys = newCache;
            return cacheKey;
        }
    }

    public DecodingMode decodingMode() {
        return builder.decodingMode;
    }

    protected ReBuilder builder() {
        return builder;
    }

    public ReBuilder copyReBuilder() {
        return builder.copy();
    }

    public int indentionStep() {
        return builder.indentionStep;
    }

    public boolean omitDefaultValue() {
        return builder.omitDefaultValue;
    }

    public boolean escapeUnicode() {
        return builder.escapeUnicode;
    }

    public EncodingMode encodingMode() {
        return builder.encodingMode;
    }

    public static class ReBuilder {

        private DecodingMode decodingMode;
        private EncodingMode encodingMode;
        private int indentionStep;
        private boolean escapeUnicode = true;
        private boolean omitDefaultValue = false;
		

        public ReBuilder() {
            String envMode = System.getenv("JSONITER_DECODING_MODE");
            if (envMode != null) {
                decodingMode = DecodingMode.valueOf(envMode);
            } else {
                decodingMode = DecodingMode.REFLECTION_MODE;
            }
            envMode = System.getenv("JSONITER_ENCODING_MODE");
            if (envMode != null) {
                encodingMode = EncodingMode.valueOf(envMode);
            } else {
                encodingMode = EncodingMode.REFLECTION_MODE;
            }
        }

        public ReBuilder decodingMode(DecodingMode decodingMode) {
            this.decodingMode = decodingMode;
            return this;
        }

        public ReBuilder encodingMode(EncodingMode encodingMode) {
            this.encodingMode = encodingMode;
            return this;
        }

        public ReBuilder indentionStep(int indentionStep) {
            this.indentionStep = indentionStep;
            return this;
        }

        public ReBuilder omitDefaultValue(boolean omitDefaultValue) {
            this.omitDefaultValue = omitDefaultValue;
            return this;
        }

        public ReBuilder escapeUnicode(boolean escapeUnicode) {
            this.escapeUnicode = escapeUnicode;
            return this;
        }

        public Config build() {
            String configName = JsoniterSpi.assignConfigName(this);
            Config config = configs.get(configName);
            if (config != null) {
                return config;
            }
            synchronized (Config.class) {
                config = configs.get(configName);
                if (config != null) {
                    return config;
                }
                config = doBuild(configName);
                HashMap<String, Config> newCache = new HashMap<String, Config>(configs);
                newCache.put(configName, config);
                configs = newCache;
                return config;
            }
        }

        protected Config doBuild(String configName) {
            return new Config(configName, this);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ReBuilder builder = (ReBuilder) o;

            if (indentionStep != builder.indentionStep) return false;
            if (escapeUnicode != builder.escapeUnicode) return false;
            if (decodingMode != builder.decodingMode) return false;
            if (omitDefaultValue != builder.omitDefaultValue) return false;
            return encodingMode == builder.encodingMode;
        }

        @Override
        public int hashCode() {
            int result = decodingMode != null ? decodingMode.hashCode() : 0;
            result = 31 * result + (encodingMode != null ? encodingMode.hashCode() : 0);
            result = 31 * result + indentionStep;
            result = 31 * result + (escapeUnicode ? 1 : 0);
            result = 31 * result + (omitDefaultValue ? 1 : 0);
            return result;
        }

        public ReBuilder copy() {
            ReBuilder builder = new ReBuilder();
            builder.encodingMode = encodingMode;
            builder.decodingMode = decodingMode;
            builder.indentionStep = indentionStep;
            builder.escapeUnicode = escapeUnicode;
            builder.omitDefaultValue = omitDefaultValue;
            return builder;
        }

        @Override
        public String toString() {
            return "Config{" +
                    "decodingMode=" + decodingMode +
                    ", encodingMode=" + encodingMode +
                    ", indentionStep=" + indentionStep +
                    ", escapeUnicode=" + escapeUnicode +
                    ", omitDefaultValue=" + omitDefaultValue +
                    '}';
        }

		public ReBuilder setDateFormat() {
			// TODO Auto-generated method stub
			return null;
		}

		public ReBuilder excludeFieldsWithoutExposeAnnotation() {
			// TODO Auto-generated method stub
			return null;
		}

		public ReBuilder serializeNulls() {
			// TODO Auto-generated method stub
			return null;
		}


		public ReBuilder setPrettyPrinting() {
			// TODO Auto-generated method stub
			return null;
		}

		public ReBuilder disableHtmlEscaping() {
			// TODO Auto-generated method stub
			return null;
		}

		public ReBuilder setVersion() {
			// TODO Auto-generated method stub
			return null;
		}
    }

    public static final Config INSTANCE = new ReBuilder().build();

    @Override
    public void updateClassDescriptor(ClassDescriptor desc) {
        JsonObject jsonObject = (JsonObject) desc.getClazz().getAnnotation(JsonObject.class);
        if (jsonObject != null) {
            if (jsonObject.asExtraForUnknownProperties()) {
                desc.setAsExtraForUnknownProperties(true);
            }
            for (String fieldName : jsonObject.unknownPropertiesWhitelist()) {
                Binding binding = new Binding(desc.getClassInfo(), desc.getLookup(), Object.class);
                binding.setName(fieldName);
                binding.setFromNames(new String[]{binding.getName()});
                binding.setToNames(new String[0]);
                binding.setShouldSkip(true);
                desc.getFields().add(binding);
            }
            for (String fieldName : jsonObject.unknownPropertiesBlacklist()) {
                Binding binding = new Binding(desc.getClassInfo(), desc.getLookup(), Object.class);
                binding.setName(fieldName);
                binding.setFromNames(new String[]{binding.getName()});
                binding.setToNames(new String[0]);
                binding.setAsExtraWhenPresent(true);
                desc.getFields().add(binding);
            }
        }
        List<Method> allMethods = new ArrayList<Method>();
        Class current = desc.getClazz();
        while (current != null) {
            allMethods.addAll(Arrays.asList(current.getDeclaredMethods()));
            current = current.getSuperclass();
        }
        updateBindings(desc);
        detectCtor(desc);
        detectStaticFactory(desc, allMethods);
        detectWrappers(desc, allMethods);
        detectUnwrappers(desc, allMethods);
    }

    private void detectUnwrappers(ClassDescriptor desc, List<Method> allMethods) {
        for (Method method : allMethods) {
            
            desc.getUnwrappers().add(new UnwrapperDescriptor(method));
        }
    }

    private void detectWrappers(ClassDescriptor desc, List<Method> allMethods) {
        for (Method method : allMethods) {
            
            JsonWrapper jsonWrapper = getJsonWrapper(method.getAnnotations());
            
            Annotation[][] annotations = method.getParameterAnnotations();
            String[] paramNames = getParamNames(method, annotations.length);
            Iterator<Binding> iter = desc.getSetters().iterator();
            while(iter.hasNext()) {
                if (method.equals(iter.next().getMethod())) {
                    iter.remove();
                }
            }
            if (JsonWrapperType.BINDING.equals(jsonWrapper.value())) {
                WrapperDescriptor wrapper = new WrapperDescriptor();
                wrapper.setMethod(method);
                detect (annotations, desc, paramNames, method, wrapper);
                
                desc.getBindingTypeWrappers().add(wrapper);
            } else if (JsonWrapperType.KEY_VALUE.equals(jsonWrapper.value())) {
                desc.getKeyValueTypeWrappers().add(method);
            } else {
                throw new JsonException("unknown json wrapper type: " + jsonWrapper.value());
            }
        }
    }
    

    private  void detect(Annotation[][] annotations, ClassDescriptor desc, String[] paramNames, Method method, WrapperDescriptor wrapper) {
    	
    	for (int i = 0; i < annotations.length; i++) {
            Annotation[] paramAnnotations = annotations[i];
            Binding binding = new Binding(desc.getClassInfo(), desc.getLookup(), method.getGenericParameterTypes()[i]);
            JsonProperty jsonProperty = getJsonProperty(paramAnnotations);
            
            if (binding.getName() == null || binding.getName().length() == 0) {
                binding.setName(paramNames[i]);
            }
            if (jsonProperty != null) {
                updateBindingWithJsonProperty(binding, jsonProperty);
            }
            binding.setFromNames(new String[]{binding.getName()});
            binding.setAnnotations(paramAnnotations);
            wrapper.setparameters(binding);
            binding.setToNames(new String[]{binding.getName()});
            
            
        }
    }

    private String[] getParamNames(Object obj, int paramCount) {
        String[] paramNames = new String[paramCount];
        try {
            Object params = reflectCall(obj, "getParameters");
            for (int i = 0; i < paramNames.length; i++) {
                paramNames[i] = (String) reflectCall(Array.get(params, i), "getName");
            }
        } catch (Exception e) {
        	//ignore
        }
        return paramNames;
    }

    private Object reflectCall(Object obj, String methodName, Object... args) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException  {
        Method method = obj.getClass().getMethod(methodName);
        return method.invoke(obj, args);
    }

    private void detectStaticFactory(ClassDescriptor desc, List<Method> allMethods) {
        for (Method method : allMethods) {
            
            
            
            desc.getCtor().setStaticMethodName(method.getName());
            desc.getCtor().setStaticFactory(method);
            desc.getCtor().setCtor(null);
            Annotation[][] annotations = method.getParameterAnnotations();
            String[] paramNames = getParamNames(method, annotations.length);
            for (int i = 0; i < annotations.length; i++) {
                Annotation[] paramAnnotations = annotations[i];
                JsonProperty jsonProperty = getJsonProperty(paramAnnotations);
                Binding binding = new Binding(desc.getClassInfo(), desc.getLookup(), method.getGenericParameterTypes()[i]);
                binding.setFromNames(new String[]{binding.getName()});
                binding.setToNames(new String[]{binding.getName()});
                if (binding.getName() == null || binding.getName().length() == 0) {
                    binding.setName(paramNames[i]);
                }

                
                binding.setAnnotations(paramAnnotations);
                desc.getCtor().getParameters().add(binding);
                if (jsonProperty != null) {
                    updateBindingWithJsonProperty(binding, jsonProperty);
                }
            }
        }
    }

    private void detectCtor(ClassDescriptor desc) {
        if (desc.getCtor() == null) {
            return;
        }
        for (Constructor ctor : desc.getClazz().getDeclaredConstructors()) {
            JsonCreator jsonCreator = getJsonCreator(ctor.getAnnotations());
            if (jsonCreator == null) {
                continue;
            }
            desc.getCtor().setStaticMethodName(null);
            desc.getCtor().setCtor(ctor);
            desc.getCtor().setStaticFactory(null);
            Annotation[][] annotations = ctor.getParameterAnnotations();
            String[] paramNames = getParamNames(ctor, annotations.length);
            for (int i = 0; i < annotations.length; i++) {
                Annotation[] paramAnnotations = annotations[i];
                JsonProperty jsonProperty = getJsonProperty(paramAnnotations);
                Binding binding = new Binding(desc.getClassInfo(), desc.getLookup(), ctor.getGenericParameterTypes()[i]);
                if (jsonProperty != null) {
                    updateBindingWithJsonProperty(binding, jsonProperty);
                }
                if (binding.getName() == null || binding.getName().length() == 0) {
                    binding.setName(paramNames[i]);
                }

                binding.setFromNames(new String[]{binding.getName()});
                binding.setToNames(new String[]{binding.getName()});
                binding.setAnnotations(paramAnnotations);
                desc.getCtor().getParameters().add(binding);

            }
        }
    }

    private void updateBindings(ClassDescriptor desc) {
        boolean globalOmitDefault = JsoniterSpi.getCurrentConfig().omitDefaultValue();
        for (Binding binding : desc.allBindings()) {
            boolean annotated = false;
            JsonIgnore jsonIgnore = getJsonIgnore(binding.getAnnotations());
            if (jsonIgnore != null) {
                annotated = true;
                if (jsonIgnore.ignoreDecoding()) {
                    binding.setFromNames(new String[0]);
                }
                if (jsonIgnore.ignoreEncoding()) {
                    binding.setToNames(new String[0]);
                }
            }
            // map JsonUnwrapper is not getter
            JsonUnwrapper jsonUnwrapper = getJsonUnwrapper(binding.getAnnotations());
            if (jsonUnwrapper != null) {
                annotated = true;
                binding.setFromNames(new String[0]);
                binding.setToNames(new String[0]);
            }
            if (globalOmitDefault) {
                binding.setDefaultValueToOmit(createOmitValue(binding.getValueType()));
            }
            JsonProperty jsonProperty = getJsonProperty(binding.getAnnotations());
            if (jsonProperty != null) {
                annotated = true;
                updateBindingWithJsonProperty(binding, jsonProperty);
            }
            up2(binding, desc);
          
            up(annotated, binding, desc);
            
        }
    }
    
    private static void up2(Binding binding, ClassDescriptor desc) {
    	
    	  if (getAnnotation(binding.getAnnotations(), JsonMissingProperties.class) != null) {
              
              // this binding will not bind from json
              // instead it will be set by jsoniter with missing property names
              binding.setFromNames(new String[0]);
              desc.setOnMissingProperties(binding);
          }
          if (getAnnotation(binding.getAnnotations(), JsonExtraProperties.class) != null) {
              
              // this binding will not bind from json
              // instead it will be set by jsoniter with extra properties
              binding.setFromNames(new String[0]);
              desc.setOnExtraProperties(binding);
          }
    }
    
    private static void up(boolean annotated, Binding binding, ClassDescriptor desc) {
    	
    	if (annotated && binding.getField() != null) {
            if (desc.getSetters() != null) {
                for (Binding setter : desc.getSetters()) {
                    if (binding.getField().getName().equals(setter.getName())) {
                        setter.setFromNames(new String[0]);
                        setter.setToNames(new String[0]);
                    }
                }
            }
            up3(binding, desc);
            
        }
    }
    
    private static void up3(Binding binding, ClassDescriptor desc) {
    	
    	if (desc.getGetters() != null) {
            for (Binding getter : desc.getGetters()) {
                if (binding.getField().getName().equals(getter.getName())) {
                    getter.setFromNames(new String[0]);
                    getter.setToNames(new String[0]);
                }
            }
        }
    }

    private static void updateBindingWithJsonProperty(Binding binding, JsonProperty jsonProperty) {
        binding.setAsMissingWhenNotPresent(jsonProperty.required());
        binding.setNullable(jsonProperty.nullable());
        binding.setCollectionValueNullable(jsonProperty.collectionValueNullable());
        String defaultValueToOmit = jsonProperty.defaultValueToOmit();
        if (!defaultValueToOmit.isEmpty()) {
            binding.setDefaultValueToOmit(OmitValue.Parsed.parse(binding.getValueType(), defaultValueToOmit));
        }
        String altName = jsonProperty.value();
        
        up2(binding, jsonProperty, altName);
        
        Class encoderClass = jsonProperty.encoder();
        if (encoderClass != Encoder.class) {
            try {                
                Constructor encoderCtor = encoderClass.getConstructor(Binding.class);
                binding.setEncoder((Encoder) encoderCtor.newInstance(binding));           
                binding.setEncoder((Encoder) encoderClass.getDeclaredConstructor().newInstance());
                
            } catch (JsonException e) {
                throw e;
            } catch (Exception e) {
                throw new JsonException(e);
            }
        }
        if (jsonProperty.implementation() != Object.class) {
            binding.setValueType(GenericsHelper.useImpl(binding.getValueType(), jsonProperty.implementation()));
            binding.setValueTypeLiteral(TypeLiteral.create(binding.getValueType()));
        }
    }
    
    private static void up2(Binding binding, JsonProperty jsonProperty, String altName) {
    	
    	if (!altName.isEmpty()) {
            if (binding.getName() == null) {
                binding.setName(altName);
            }
            binding.setFromNames(new String[]{altName});
            binding.setToNames(new String[]{altName});
        }
        if (jsonProperty.from().length > 0) {
            binding.setFromNames(jsonProperty.from());
        }
        if (jsonProperty.to().length > 0) {
            binding.setToNames(jsonProperty.to());
        }
        Class decoderClass = jsonProperty.decoder();
        if (decoderClass != Decoder.class) {
            try {
              Constructor decoderCtor = decoderClass.getConstructor(Binding.class);
              binding.setDecoder((Decoder) decoderCtor.newInstance(binding));             
              binding.setDecoder((Decoder) decoderClass.getDeclaredConstructor().newInstance());                
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new JsonException(e);
            }
        }
    }

    protected OmitValue createOmitValue(Type valueType) {
        OmitValue omitValue = primitiveOmitValues.get(valueType);
        if (omitValue != null) {
            return omitValue;
        }
        return new OmitValue.Null();
    }

    protected JsonWrapper getJsonWrapper(Annotation[] annotations) {
        return getAnnotation(annotations, JsonWrapper.class);
    }

    protected JsonUnwrapper getJsonUnwrapper(Annotation[] annotations) {
        return getAnnotation(annotations, JsonUnwrapper.class);
    }

    protected JsonCreator getJsonCreator(Annotation[] annotations) {
        return getAnnotation(annotations, JsonCreator.class);
    }

  
    protected JsonProperty getJsonProperty(Annotation[] annotations) {
        return getAnnotation(annotations, JsonProperty.class);
    }

    protected JsonIgnore getJsonIgnore(Annotation[] annotations) {
        return getAnnotation(annotations, JsonIgnore.class);
    }

    protected static <T extends Annotation> T getAnnotation(Annotation[] annotations, Class<T> annotationClass) {
        if (annotations == null) {
            return null;
        }
        for (Annotation annotation : annotations) {
            if (annotationClass.isAssignableFrom(annotation.getClass())) {
                return (T) annotation;
            }
        }
        return null;
    }
}
