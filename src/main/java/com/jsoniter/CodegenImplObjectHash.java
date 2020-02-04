package com.jsoniter;

import com.jsoniter.spi.*;

import java.util.*;

class CodegenImplObjectHash {	
	
	private static final String CON = "} else {";
	private CodegenImplObjectHash() {
	    throw new IllegalStateException("Utility class");
	}

    // the implementation is from dsljson, it is the fastest although has the risk not matching field strictly
    public static String genObjectUsingHash(ClassDescriptor desc) {
        Class clazz = desc.getClazz();
        StringBuilder lines = new StringBuilder();
        // === if null, return null
        append(lines, "java.lang.Object existingObj = com.jsoniter.CodegenAccess.resetExistingObject(iter);");
        append(lines, "byte nextToken = com.jsoniter.CodegenAccess.readByte(iter);");
        append(lines, "if (nextToken != '{') {");
        append(lines, "if (nextToken == 'n') {");
        append(lines, "com.jsoniter.CodegenAccess.skipFixedBytes(iter, 3);");
        append(lines, "return null;");
        append(lines, CON);
        append(lines, "nextToken = com.jsoniter.CodegenAccess.nextToken(iter);");
        append(lines, "if (nextToken == 'n') {");
        append(lines, "com.jsoniter.CodegenAccess.skipFixedBytes(iter, 3);");
        append(lines, "return null;");
        append(lines, "}");
        append(lines, "} // end of if null");
        append(lines, "} // end of if {");
        // === if empty, return empty
        // ctor requires binding

        for (Binding parameter : desc.getCtor().getParameters()) {

            appendVarDef(lines, parameter);
        }
        append(lines, "nextToken = com.jsoniter.CodegenAccess.readByte(iter);");
        append(lines, "if (nextToken != '\"') {");
        append(lines, "if (nextToken == '}') {");
        append(lines, "return {{newInst}};");
        append(lines, CON);
        append(lines, "nextToken = com.jsoniter.CodegenAccess.nextToken(iter);");
        append(lines, "if (nextToken == '}') {");
        append(lines, "return {{newInst}};");
        append(lines, CON);
        append(lines, "com.jsoniter.CodegenAccess.unreadByte(iter);");
        append(lines, "}");
        append(lines, "} // end of if end");
        append(lines, "} else { com.jsoniter.CodegenAccess.unreadByte(iter); }// end of if not quote");
        
        HashSet<Integer> knownHashes = new HashSet<Integer>();
        HashMap<String, Binding> bindings = new HashMap<String, Binding>();
        gen(desc, lines, bindings);
        
        ArrayList<String> fromNames = new ArrayList<String>(bindings.keySet());
        Collections.sort(fromNames, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                int x = calcHash(o1);
                int y = calcHash(o2);
                if (x < y) {
                	return -1;
                }
                return ((x == y) ? 0 : 1);
            }
        });
        // === bind more fields
        append(lines, "do {");
        append(lines, "switch (com.jsoniter.CodegenAccess.readObjectFieldAsHash(iter)) {");
        
        object(desc, lines, clazz, fromNames, knownHashes, bindings);
        
        
        appendWrappers(desc.getBindingTypeWrappers(), lines);
        append(lines, "return obj;");
        return lines.toString()
                .replace("{{clazz}}", clazz.getCanonicalName())
                .replace("{{newInst}}", genNewInstCode(clazz, desc.getCtor()));
    }
    
    private static void gen(ClassDescriptor desc, StringBuilder lines, HashMap<String, Binding> bindings) {
    	
    	for (Binding field : desc.getFields()) {
            if (field.getFromNames().length == 0) {
                continue;
            }
            appendVarDef(lines, field);
        }
        for (Binding setter : desc.getSetters()) {
            appendVarDef(lines, setter);
        }
        for (WrapperDescriptor setter : desc.getBindingTypeWrappers()) {
            for (Binding param : setter.getparameters()) {
                appendVarDef(lines, param);
            }
        }
        // === bind fields
        
        
        for (Binding binding : desc.allDecoderBindings()) {
            for (String fromName : binding.getFromNames()) {
                bindings.put(fromName, binding);
            }
        }
    	
    }
    
    private static String object(ClassDescriptor desc, StringBuilder lines, Class clazz, ArrayList<String> fromNames, HashSet<Integer> knownHashes, HashMap<String, Binding> bindings) {
    	
    	for (String fromName : fromNames) {
            int intHash = calcHash(fromName);
            if (intHash == 0) {
                // hash collision, 0 can not be used as sentinel
                return CodegenImplObjectStrict.genObjectUsingStrict(desc);
            }
            if (knownHashes.contains(intHash)) {
                // hash collision with other field can not be used as sentinel
                return CodegenImplObjectStrict.genObjectUsingStrict(desc);
            }
            knownHashes.add(intHash);
            append(lines, "case " + intHash + ": ");
            appendBindingSet(lines, bindings.get(fromName));
            append(lines, "continue;");
        }
        append(lines, "}");
        append(lines, "iter.skip();");
        append(lines, "} while (com.jsoniter.CodegenAccess.nextTokenIsComma(iter));");
        append(lines, CodegenImplNative.getTypeName(clazz) + " obj = {{newInst}};");
        for (Binding field : desc.getFields()) {
            if (field.getFromNames().length == 0) {
                continue;
            }
            append(lines, String.format("obj.%s = _%s_;", field.getField().getName(), field.getName()));
        }
        for (Binding setter : desc.getSetters()) {
            append(lines, String.format("obj.%s(_%s_);", setter.getMethod().getName(), setter.getName()));
        }
		return null;
    }

    public static int calcHash(String fromName) {
        long hash = 0x811c9dc5;
        for (byte b : fromName.getBytes()) {
            hash ^= b;
            hash *= 0x1000193;
        }
        return (int) hash;
    }

    private static void appendBindingSet(StringBuilder lines, Binding binding) {
        append(lines, String.format("_%s_ = %s;", binding.getName(), CodegenImplNative.genField(binding)));
    }

    static void appendWrappers(List<WrapperDescriptor> wrappers, StringBuilder lines) {
        for (WrapperDescriptor wrapper : wrappers) {
            lines.append("obj.");
            lines.append(wrapper.getMethod().getName());
            appendInvocation(lines, wrapper.getparameters());
            lines.append(";\n");
        }
    }

    static void appendVarDef(StringBuilder lines, Binding parameter) {
        String typeName = CodegenImplNative.getTypeName(parameter.getValueType());
        append(lines, String.format("%s _%s_ = %s;", typeName, parameter.getName(), CodegenImplObjectStrict.DEFAULT_VALUES.get(typeName)));
    }

    static String genNewInstCode(Class clazz, ConstructorDescriptor ctor) {
        StringBuilder code = new StringBuilder();
        if (ctor.getParameters().isEmpty()) {
            // nothing to bind, safe to reuse existing object
            code.append("(existingObj == null ? ");
        }
        if (ctor.getObjectFactory() != null) {
            code.append(String.format("(%s)com.jsoniter.spi.JsoniterSpi.create(%s.class)",
                    clazz.getCanonicalName(), clazz.getCanonicalName()));
        } else {
            if (ctor.getStaticMethodName() == null) {
                code.append(String.format("new %s", clazz.getCanonicalName()));
            } else {
                code.append(String.format("%s.%s", clazz.getCanonicalName(), ctor.getStaticMethodName()));
            }
        }
        List<Binding> params = ctor.getParameters();
        if (ctor.getObjectFactory() == null) {
            appendInvocation(code, params);
        }
        if (ctor.getParameters().isEmpty()) {
            // nothing to bind, safe to reuse existing obj
            code.append(String.format(" : (%s)existingObj)", clazz.getCanonicalName()));
        }
        return code.toString();
    }

    private static void appendInvocation(StringBuilder code, List<Binding> params) {
        code.append("(");
        boolean isFirst = true;
        for (Binding ctorParam : params) {
            if (isFirst) {
                isFirst = false;
            } else {
                code.append(",");
            }
            code.append(String.format("_%s_", ctorParam.getName()));
        }
        code.append(")");
    }

    static void append(StringBuilder lines, String str) {
        lines.append(str);
        lines.append("\n");
    }

}
