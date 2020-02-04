package com.jsoniter.output;

import com.jsoniter.spi.*;

import java.util.*;

class CodegenImplObject {
	
	private CodegenImplObject () {
	    throw new IllegalStateException("Utility class");
	}
	
    public static CodegenResult genObject(ClassInfo classInfo) {
        boolean noIndention = JsoniterSpi.getCurrentConfig().indentionStep() == 0;
        CodegenResult ctx = new CodegenResult();
        ClassDescriptor desc = ClassDescriptor.getEncodingClassDescriptor(classInfo, false);
        ctx.append(String.format("public static void encode_(%s obj, com.jsoniter.output.JsonStream stream) throws java.io.IOException {", classInfo.clazz.getCanonicalName()));
        if (hasFieldOutput(desc)) {
            int notFirst = 0;
            if (noIndention) {
                ctx.buffer('{');
            } else {
                ctx.append("stream.writeObjectStart();");
            }            
            gen1(desc, ctx, notFirst, noIndention);
            
        } else {
            ctx.buffer("{}");
        }
        ctx.append("}");
        return ctx;
    }
    
    private static void gen1(ClassDescriptor desc, CodegenResult ctx, int notFirst, boolean noIndention) {
    	
    	for (UnwrapperDescriptor unwrapper : desc.getUnwrappers()) {
            if (unwrapper.isMap()) {
                ctx.append(String.format("java.util.Map map = (java.util.Map)obj.%s();", unwrapper.getMethod().getName()));
                ctx.append("java.util.Iterator iter = map.entrySet().iterator();");
                ctx.append("while(iter.hasNext()) {");
                ctx.append("java.util.Map.Entry entry = (java.util.Map.Entry)iter.next();");
                notFirst = appendComma(ctx, notFirst);
                ctx.append("stream.writeObjectField(entry.getKey().toString());");
                ctx.append("if (entry.getValue() == null) { stream.writeNull(); } else {");
                CodegenImplNative.genWriteOp(ctx, "entry.getValue()", unwrapper.getMapValueTypeLiteral().getType(), true);
                ctx.append("}");
                ctx.append("}");
            } else {
                notFirst = appendComma(ctx, notFirst);
                ctx.append(String.format("obj.%s(stream);", unwrapper.getMethod().getName()));
            }
        }
        if (noIndention) {
            ctx.buffer('}');
        } else {
            if (notFirst == 1) { // definitely not first
                ctx.append("stream.writeObjectEnd();");
            } else if (notFirst == 2) { // // maybe not first, previous field is omitNull
                ctx.append("if (notFirst) { stream.writeObjectEnd(); } else { stream.write('}'); }");
            } else { // this is the first
                ctx.append("stream.write('}');");
            }
        }
    }


    private static boolean hasFieldOutput(ClassDescriptor desc) {
        if (!desc.getUnwrappers().isEmpty()) {
            return true;
        }
        return !desc.encodeTos().isEmpty();
    }

 
    
 

    private static int appendComma(CodegenResult ctx, int notFirst) {
        boolean noIndention = JsoniterSpi.getCurrentConfig().indentionStep() == 0;
        if (notFirst == 1) { // definitely not first
            if (noIndention) {
                ctx.buffer(',');
            } else {
                ctx.append("stream.writeMore();");
            }
        } else if (notFirst == 2) { // maybe not first, previous field is omitNull
            if (noIndention) {
                ctx.append("if (notFirst) { stream.write(','); } else { notFirst = true; }");
            } else {
                ctx.append("if (notFirst) { stream.writeMore(); } else { stream.writeIndention(); notFirst = true; }");
            }
        } else { // this is the first, do not write comma
            notFirst = 1;
            if (!noIndention) {
                ctx.append("stream.writeIndention();");
            }
        }
        return notFirst;
    }


}
