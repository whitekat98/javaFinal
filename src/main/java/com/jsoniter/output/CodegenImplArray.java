package com.jsoniter.output;

import com.jsoniter.spi.ClassInfo;
import com.jsoniter.spi.JsoniterSpi;

import java.lang.reflect.Type;
import java.util.*;

class CodegenImplArray {

	private static final String CON = "__value_not_nullable";
	private static final String CON1 = "public static void encode_(java.lang.Object obj, com.jsoniter.output.JsonStream stream) throws java.io.IOException {";
	private static final String CON2 = "stream.writeArrayStart(); stream.writeIndention();";
	private static final String CON3 = "if (e == null) { stream.writeNull(); } else {";
	private static final String CON4 = "stream.write(',');";
	private static final String CON5 = "stream.writeMore();";
	private static final String CON6 = "stream.writeArrayEnd();";
	
	private CodegenImplArray () {
	    throw new IllegalStateException("Utility class");
	}
	

    public static CodegenResult genCollection(String cacheKey, ClassInfo classInfo) {
        Type[] typeArgs = classInfo.typeArgs;
        Class clazz = classInfo.clazz;
        
        if (typeArgs.length == 0) {
            // default to List<Object>        
            throw new IllegalArgumentException(
                    "can not bind to generic collection without argument types, " +
                            "try syntax like TypeLiteral<List<Integer>>{}");
        }
        if (clazz == List.class) {
            clazz = ArrayList.class;
        } else if (clazz == Set.class) {
            clazz = HashSet.class;
        }
        if (List.class.isAssignableFrom(clazz)) {
            return genList(cacheKey, clazz);
        } else {
            return genCollection(cacheKey, clazz);
        }
    }

    public static CodegenResult genArray(String cacheKey, ClassInfo classInfo) {
        boolean noIndention = JsoniterSpi.getCurrentConfig().indentionStep() == 0;
        Class clazz = classInfo.clazz;
        Class compType = clazz.getComponentType();
        if (compType.isArray()) {
            throw new IllegalArgumentException("nested array not supported: " + clazz.getCanonicalName());
        }
        boolean isCollectionValueNullable = true;
        if (cacheKey.endsWith(CON)) {
            isCollectionValueNullable = false;
        }
        if (compType.isPrimitive()) {
            isCollectionValueNullable = false;
        }
        CodegenResult ctx = new CodegenResult();
        ctx.append(CON1);
        ctx.append(String.format("%s[] arr = (%s[])obj;", compType.getCanonicalName(), compType.getCanonicalName()));
        if (isCollectionValueNullable) {
            ctx.append(CON3);
            CodegenImplNative.genWriteOp(ctx, "e", compType, true);
            ctx.append("}"); // if
        } else {
            CodegenImplNative.genWriteOp(ctx, "e", compType, false);
        }
        if (noIndention) {
            ctx.append("if (arr.length == 0) { return; }");
            ctx.buffer('[');
        } else {
            ctx.append("if (arr.length == 0) { stream.write((byte)'[', (byte)']'); return; }");
            ctx.append(CON2);
        }
        ctx.append("int i = 0;");
        ctx.append(String.format("%s e = arr[i++];", compType.getCanonicalName()));
        
        ctx.append("while (i < arr.length) {");
        if (noIndention) {
            ctx.append(CON4);
        } else {
            ctx.append(CON5);
        }
        ctx.append("e = arr[i++];");
       if (noIndention) {
            ctx.buffer(']');
        } else {
            ctx.append(CON3);
        }
        if (isCollectionValueNullable) {
            ctx.append(CON6);
            CodegenImplNative.genWriteOp(ctx, "e", compType, true);
            ctx.append("}"); // if
        } else {
            CodegenImplNative.genWriteOp(ctx, "e", compType, false);
        }
        ctx.append("}"); // while
       
        ctx.append("}"); // public static void encode_
        return ctx;
    }

    private static CodegenResult genList(String cacheKey, Type compType) {
        boolean noIndention = JsoniterSpi.getCurrentConfig().indentionStep() == 0;
        boolean isCollectionValueNullable = true;
        
        CodegenResult ctx = new CodegenResult();
        ctx.append(CON2);
        ctx.append("java.util.List list = (java.util.List)obj;");
        ctx.append("int size = list.size();");
        if (noIndention) {
            ctx.append("if (size == 0) { return; }");
            ctx.buffer('[');
        } else {
            ctx.append("if (size == 0) { stream.write((byte)'[', (byte)']'); return; }");
            ctx.append(CON1);
        }
        ctx.append("java.lang.Object e = list.get(0);");
        if (cacheKey.endsWith(CON)) {
            isCollectionValueNullable = false;
        }
        ctx.append("for (int i = 1; i < size; i++) {");
        if (noIndention) {
            ctx.append(CON4);
        } else {
            ctx.append(CON5);
        } 
       
        if (noIndention) {
            ctx.buffer(']');
        } else {
            CodegenImplNative.genWriteOp(ctx, "e", compType, false);
        } 
        if (isCollectionValueNullable) {
            ctx.append(CON3);
            CodegenImplNative.genWriteOp(ctx, "e", compType, true);
            ctx.append("}");
        } else {
            ctx.append(CON6);
        }
        ctx.append("e = list.get(i);");
        if (isCollectionValueNullable) {
            ctx.append(CON3);
            CodegenImplNative.genWriteOp(ctx, "e", compType, true);
            ctx.append("}"); // if
        } else {
            CodegenImplNative.genWriteOp(ctx, "e", compType, false);
        }
        ctx.append("}"); // for
      
        ctx.append("}"); // public static void encode_
        return ctx;
    }

    private static CodegenResult genCollection(String cacheKey, Type compType) {
        boolean noIndention = JsoniterSpi.getCurrentConfig().indentionStep() == 0;
        boolean isCollectionValueNullable = true;
        if (cacheKey.endsWith(CON)) {
            isCollectionValueNullable = false;
        }
        CodegenResult ctx = new CodegenResult();
        ctx.append(CON1);
        ctx.append("java.util.Iterator iter = ((java.util.Collection)obj).iterator();");

        ctx.append("java.lang.Object e = iter.next();");
        
        ctx.append("while (iter.hasNext()) {");

        if (noIndention) {
            ctx.append("if (!iter.hasNext()) { return; }");
            ctx.buffer('[');
        } else {
            ctx.append("if (!iter.hasNext()) { stream.write((byte)'[', (byte)']'); return; }");
            ctx.append(CON2);
        }
        if (noIndention) {
            ctx.append(CON4);
        } else {
            ctx.append(CON5);
        }
        if (isCollectionValueNullable) {
            ctx.append(CON3);
            CodegenImplNative.genWriteOp(ctx, "e", compType, true);
            ctx.append("}"); // if
        } else {
            CodegenImplNative.genWriteOp(ctx, "e", compType, false);
        }
        ctx.append("e = iter.next();"); 
        if (noIndention) {
            ctx.buffer(']');
        } else {
            ctx.append(CON6);
        }
        if (isCollectionValueNullable) {
            ctx.append(CON3);
            CodegenImplNative.genWriteOp(ctx, "e", compType, true);
            ctx.append("}"); // if
        } else {
            CodegenImplNative.genWriteOp(ctx, "e", compType, false);
        }
        ctx.append("}"); // while
      
        ctx.append("}"); // public static void encode_
        return ctx;
    }

}
