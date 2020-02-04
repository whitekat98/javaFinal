package com.jsoniter;

import com.jsoniter.spi.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.util.*;

class Codegen {
	
	  private Codegen() {
	    throw new IllegalStateException("Utility class");
	  }


    // only read/write when generating code with synchronized protection
    private static final Set<String> generatedClassNames = new HashSet<String>();
    static CodegenAccess.StaticCodegenTarget isDoingStaticCodegen = null;

    static Decoder getDecoder(String cacheKey, Type type) {
        Decoder decoder = JsoniterSpi.getDecoder(cacheKey);
        if (decoder != null) {
            return decoder;
        }
        return gen(cacheKey, type);
    }

    
	
	private  static synchronized  Decoder gen(String cacheKey, Type type) {
    	Decoder decoder = JsoniterSpi.getDecoder(cacheKey);
        ClassInfo classInfo = new ClassInfo(type);
        gen2(cacheKey, type, decoder, classInfo);
        addPlaceholderDecoderToSupportRecursiveStructure(cacheKey);
        try {
            Config currentConfig = JsoniterSpi.getCurrentConfig();
            DecodingMode mode = currentConfig.decodingMode();
            if (mode == DecodingMode.REFLECTION_MODE) {
                decoder = ReflectionDecoderFactory.create(classInfo);
                return decoder;
            }
            if (isDoingStaticCodegen == null) {
                try {
                	
                    decoder = (Decoder) Class.forName(cacheKey).getDeclaredConstructor().newInstance();
                    return decoder;
                } catch (Exception e) {
                    if (mode == DecodingMode.STATIC_MODE) {
                        throw new JsonException("static gen should provide the decoder we need, but failed to create the decoder", e);
                    }
                }
            }
            String source = genSource(mode, classInfo);
            source = "public static java.lang.Object decode_(com.jsoniter.JsonIterator iter) throws java.io.IOException { "
                    + source + "}";

            try {
                generatedClassNames.add(cacheKey);
                if (isDoingStaticCodegen == null) {
             
                    staticGen(cacheKey, source);
                }
                return decoder;
            } catch (Exception e) {
                String msg = "failed to generate decoder for: " + classInfo + " with " + Arrays.toString(classInfo.typeArgs) + ", exception: " + e;
                msg = msg + "\n" + source;
                throw new JsonException(msg, e);
            }
        } finally {
            JsoniterSpi.addNewDecoder(cacheKey, decoder);
        }
    }

    private static Decoder gen2(String cacheKey, Type type, Decoder decoder, ClassInfo classInfo) {
    	
        if (decoder != null) {
            return decoder;
        }
        List<Extension> extensions = JsoniterSpi.getExtensions();
        for (Extension extension : extensions) {
            type = extension.chooseImplementation(type);
        }
        type = chooseImpl(type);
        for (Extension extension : extensions) {
            decoder = extension.createDecoder(cacheKey, type);
            if (decoder != null) {
                JsoniterSpi.addNewDecoder(cacheKey, decoder);
                return decoder;
            }
        }

        decoder = CodegenImplNative.NATIVE_DECODERS.get(classInfo.clazz);
        if (decoder != null) {
            return decoder;
        }
        return decoder;
    }
    
    private static void addPlaceholderDecoderToSupportRecursiveStructure(final String cacheKey) {
        JsoniterSpi.addNewDecoder(cacheKey, new Decoder() {
            @Override
            public Object decode(JsonIterator iter) throws IOException {
                Decoder decoder = JsoniterSpi.getDecoder(cacheKey);
                if (this == decoder) {
                    for(int i = 0; i < 30; i++) {
                        decoder = JsoniterSpi.getDecoder(cacheKey);
                        placeholder(decoder, cacheKey);
                    }
                    if (this == decoder) {
                        throw new JsonException("internal error: placeholder is not replaced with real decoder");
                    }
                }
                return decoder.decode(iter);
            }
        });
    }
    private static Decoder placeholder(Decoder decoder, String cacheKey) {
    	if (JsoniterSpi.getDecoder(cacheKey) == decoder) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
               
              Thread.currentThread().interrupt();  
            }
        }
		return decoder;
    }

    public static boolean canStaticAccess(String cacheKey) {
        return generatedClassNames.contains(cacheKey);
    }

    private static Type chooseImpl(Type type) {
        Type[] typeArgs = new Type[0];
        Class clazz = null;
        Class implClazz = JsoniterSpi.getTypeImplementation(clazz);
        choose(clazz, typeArgs);
        
        if (Map.class.isAssignableFrom(clazz)) {
            Type keyType = String.class;
            Type valueType = Object.class;
            if (typeArgs.length == 0) {
                // default to Map<String, Object>
            } else if (typeArgs.length == 2) {
                keyType = typeArgs[0];
                valueType = typeArgs[1];
            } else {
                throw new IllegalArgumentException(
                        "can not bind to generic collection without argument types, " +
                                "try syntax like TypeLiteral<Map<String, String>>{}");
            }
          impl(clazz);
            return GenericsHelper.createParameterizedType(new Type[]{keyType, valueType}, null, clazz);
        }
        if (implClazz != null) {
            if (typeArgs.length == 0) {
                return implClazz;
            } else {
                return GenericsHelper.createParameterizedType(typeArgs, null, implClazz);
            }
        }
        return type;
    }
    
    private static void impl ( Type keyType) {
    	
    	
         if (keyType == Object.class) {
             keyType = String.class;
         }
         MapKeyDecoders.registerOrGetExisting(keyType);
    }
    
    private static void choose( Class clazz, Type[] typeArgs) {
    	
        
        if (Collection.class.isAssignableFrom(clazz)) {
            
            if (typeArgs.length == 0) {
                // default to List<Object>
            } else {
                throw new IllegalArgumentException(
                        "can not bind to generic collection without argument types, " +
                                "try syntax like TypeLiteral<List<Integer>>{}");
            }
           
        }
       
    }
    
   

    private static void staticGen(String cacheKey, String source) throws IOException {
        createDir(cacheKey);
        String fileName = cacheKey.replace('.', '/') + ".java";
        FileOutputStream fileOutputStream = new FileOutputStream(new File(isDoingStaticCodegen.outputDir, fileName));
        try {
            OutputStreamWriter writer = new OutputStreamWriter(fileOutputStream);
            try {
                staticGen(cacheKey, writer, source);
            } finally {
                writer.close();
            }
        } finally {
            fileOutputStream.close();
        }
    }

    private static void staticGen(String cacheKey, OutputStreamWriter writer, String source) throws IOException {
        String className = cacheKey.substring(cacheKey.lastIndexOf('.') + 1);
        String packageName = cacheKey.substring(0, cacheKey.lastIndexOf('.'));
        writer.write("package " + packageName + ";\n");
        writer.write("public class " + className + " implements com.jsoniter.spi.Decoder {\n");
        writer.write(source);
        writer.write("public java.lang.Object decode(com.jsoniter.JsonIterator iter) throws java.io.IOException {\n");
        writer.write("return decode_(iter);\n");
        writer.write("}\n");
        writer.write("}\n");
    }

    private static void createDir(String cacheKey) {
        String[] parts = cacheKey.split("\\.");
        File parent = new File(isDoingStaticCodegen.outputDir);
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            File current = new File(parent, part);
            current.mkdir();
            parent = current;
        }
    }

    private static String genSource(DecodingMode mode, ClassInfo classInfo) {
        if (classInfo.clazz.isArray()) {
            return CodegenImplArray.genArray(classInfo);
        }
        if (Map.class.isAssignableFrom(classInfo.clazz)) {
            return CodegenImplMap.genMap(classInfo);
        }
        if (Collection.class.isAssignableFrom(classInfo.clazz)) {
            return CodegenImplArray.genCollection(classInfo);
        }
        if (classInfo.clazz.isEnum()) {
            return CodegenImplEnum.genEnum(classInfo);
        }
        ClassDescriptor desc = ClassDescriptor.getDecodingClassDescriptor(classInfo, false);
        if (shouldUseStrictMode(mode, desc)) {
            return CodegenImplObjectStrict.genObjectUsingStrict(desc);
        } else {
            return CodegenImplObjectHash.genObjectUsingHash(desc);
        }
    }

    private static boolean shouldUseStrictMode(DecodingMode mode, ClassDescriptor desc) {
        if (mode == DecodingMode.DYNAMIC_MODE_AND_MATCH_FIELD_STRICTLY) {
            return true;
        }
        List<Binding> allBindings = desc.allDecoderBindings();
        for (Binding binding : allBindings) {
            if (binding.isAsMissingWhenNotPresent() || binding.isAsExtraWhenPresent() || binding.isShouldSkip()) {
                // only slice support mandatory tracking
                return true;
            }
        }
        if (desc.isAsExtraForUnknownProperties()) {
            // only slice support unknown field tracking
            return true;
        }
        if (!desc.getKeyValueTypeWrappers().isEmpty()) {
            return true;
        }
        boolean hasBinding = false;
        for (Binding allBinding : allBindings) {
            if (allBinding.getFromNames().length > 0) {
                hasBinding = true;
            }
        }
        boolean tmp;
        if (!hasBinding) {
            // empty object can only be handled by strict mode
            tmp = true;
        }else {
        	tmp = false;
        }               
        return tmp;
    }

    public static void staticGenDecoders(TypeLiteral[] typeLiterals, CodegenAccess.StaticCodegenTarget staticCodegenTarget) {
        isDoingStaticCodegen = staticCodegenTarget;
        for (TypeLiteral typeLiteral : typeLiterals) {
            gen(typeLiteral.getDecoderCacheKey(), typeLiteral.getType());
        }
    }
}
