package com.jsoniter;

import com.jsoniter.spi.*;

import java.lang.reflect.Method;
import java.util.*;

import static com.jsoniter.CodegenImplObjectHash.appendVarDef;
import static com.jsoniter.CodegenImplObjectHash.appendWrappers;

class CodegenImplObjectStrict {	
	
	private static final String CON = "return obj;";
	private static final String CON1 = "iter.skip();";
	private CodegenImplObjectStrict() {
	    throw new IllegalStateException("Utility class");
	}

    static final Map<String, String> DEFAULT_VALUES = new HashMap<String, String>();
    static {
    	DEFAULT_VALUES.put("float", "0.0f");
    	DEFAULT_VALUES.put("double", "0.0d");
    	DEFAULT_VALUES.put("boolean", "false");
    	DEFAULT_VALUES.put("byte", "0");
    	DEFAULT_VALUES.put("short", "0");
    	DEFAULT_VALUES.put("int", "0");
    	DEFAULT_VALUES.put("char", "0");
    	DEFAULT_VALUES.put("long", "0");
    }

    public static String genObjectUsingStrict(ClassDescriptor desc) {
        List<Binding> allBindings = desc.allDecoderBindings();
        int lastRequiredIdx = assignMaskForRequiredProperties(allBindings);
        boolean hasRequiredBinding = lastRequiredIdx > 0;
        long expectedTracker = Long.MAX_VALUE >> (63 - lastRequiredIdx);
        Map<Integer, Object> trieTree = buildTriTree(allBindings);
        StringBuilder lines = new StringBuilder();
        /*
         * only strict mode binding support missing/extra properties tracking
         * 1. if null, return null
         * 2. if empty, return empty
         * 3. bind first field
        
         * 5. handle missing/extra properties
         * 6. create obj with args (if ctor binding)
         * 7. assign fields to obj (if ctor binding)
         * 8. apply multi param wrappers
         */
        // === if null, return null
        append(lines, "java.lang.Object existingObj = com.jsoniter.CodegenAccess.resetExistingObject(iter);");
        append(lines, "if (iter.readNull()) { return null; }");
        // === if input is empty obj, return empty obj
        if (hasRequiredBinding) {
            append(lines, "long tracker = 0;");
        }

        if (desc.getCtor().getParameters().isEmpty()) {

            append(lines, "{{clazz}} obj = {{newInst}};");
            append(lines, "if (!com.jsoniter.CodegenAccess.readObjectStart(iter)) {");
            if (hasRequiredBinding) {
                appendMissingRequiredProperties(lines, desc);
            }
            append(lines, CON);
            append(lines, "}");
            // because obj can be created without binding
            // so that fields and setters can be bind to obj directly without temp var
        } else {
        	
        	obge(lines, desc, hasRequiredBinding);

        }
        for (WrapperDescriptor wrapper : desc.getBindingTypeWrappers()) {
            for (Binding param : wrapper.getparameters()) {
                appendVarDef(lines, param);
            }
        }
        // === bind first field
        if (desc.getOnExtraProperties() != null || !desc.getKeyValueTypeWrappers().isEmpty()) {
            append(lines, "java.util.Map extra = null;");
        }
        append(lines, "com.jsoniter.spi.Slice field = com.jsoniter.CodegenAccess.readObjectFieldAsSlice(iter);");
        append(lines, "boolean once = true;");
        append(lines, "while (once) {");
        append(lines, "once = false;");
        String rendered = renderTriTree(trieTree);

        if (desc.getCtor().getParameters().isEmpty()) {
        	
        	usin(rendered, desc);
        }
        
        gen(lines, allBindings, rendered, desc, hasRequiredBinding, expectedTracker); 
        
        stric(lines, desc);
        
        appendWrappers(desc.getBindingTypeWrappers(), lines);
        append(lines, CON);
        return lines.toString()
                .replace("{{clazz}}", desc.getClazz().getCanonicalName())
                .replace("{{newInst}}", CodegenImplObjectHash.genNewInstCode(desc.getClazz(), desc.getCtor()));
    }
    
    private static void usin (String rendered, ClassDescriptor desc) {
    	
    	// if not field or setter, the value will set to temp variable
        for (Binding field : desc.getFields()) {
            if (field.getFromNames().length == 0) {
                continue;
            }
            rendered = updateBindingSetOp(rendered, field);
        }
        for (Binding setter : desc.getSetters()) {
            rendered = updateBindingSetOp(rendered, setter);
        }
    }
    private static void stric (StringBuilder lines, ClassDescriptor desc) {
    	
    	if (!desc.getCtor().getParameters().isEmpty()) {
            append(lines, String.format("%s obj = {{newInst}};", CodegenImplNative.getTypeName(desc.getClazz())));
            for (Binding field : desc.getFields()) {
                if (field.getFromNames().length == 0) {

                    continue;
                }
                append(lines, String.format("obj.%s = _%s_;", field.getField().getName(), field.getName()));
            }
            for (Binding setter : desc.getSetters()) {
                append(lines, String.format("obj.%s(_%s_);", setter.getMethod().getName(), setter.getName()));
            }
        }
    }
    
    private static void obge (StringBuilder lines, ClassDescriptor desc, boolean hasRequiredBinding) {
    	
    	for (Binding parameter : desc.getCtor().getParameters()) {

            appendVarDef(lines, parameter);
        }
        append(lines, "if (!com.jsoniter.CodegenAccess.readObjectStart(iter)) {");
        if (hasRequiredBinding) {
            appendMissingRequiredProperties(lines, desc);
        } else {
            append(lines, "return {{newInst}};");
        }
        append(lines, "}");
        for (Binding field : desc.getFields()) {
            if (field.getFromNames().length == 0) {
                continue;
            }
            appendVarDef(lines, field);
        }
        for (Binding setter : desc.getSetters()) {
            appendVarDef(lines, setter);
        }
    }
    
    private static void gen(StringBuilder lines, List<Binding> allBindings, String rendered, ClassDescriptor desc, boolean hasRequiredBinding, long expectedTracker) {
    	
    	if (hasAnythingToBindFrom(allBindings)) {
            append(lines, "switch (field.len()) {");
            append(lines, rendered);
            append(lines, "}"); // end of switch
        }
        appendOnUnknownField(lines, desc);
        append(lines, "}"); // end of while
        // === bind all fields
        append(lines, "while (com.jsoniter.CodegenAccess.nextToken(iter) == ',') {");
        append(lines, "field = com.jsoniter.CodegenAccess.readObjectFieldAsSlice(iter);");
        if (hasAnythingToBindFrom(allBindings)) {
            append(lines, "switch (field.len()) {");
            append(lines, rendered);
            append(lines, "}"); // end of switch
        }
        appendOnUnknownField(lines, desc);
        append(lines, "}"); // end of while
        if (hasRequiredBinding) {
            append(lines, "if (tracker != " + expectedTracker + "L) {");
            appendMissingRequiredProperties(lines, desc);
            append(lines, "}");
        }
        if (desc.getOnExtraProperties() != null) {
            appendSetExtraProperteis(lines, desc);
        }
        if (!desc.getKeyValueTypeWrappers().isEmpty()) {
            appendSetExtraToKeyValueTypeWrappers(lines, desc);
        }
        
    }

    private static void appendSetExtraToKeyValueTypeWrappers(StringBuilder lines, ClassDescriptor desc) {
        append(lines, "java.util.Iterator extraIter = extra.entrySet().iterator();");
        append(lines, "while(extraIter.hasNext()) {");
        for (Method wrapper : desc.getKeyValueTypeWrappers()) {
            append(lines, "java.util.Map.Entry entry = (java.util.Map.Entry)extraIter.next();");
            append(lines, "String key = entry.getKey().toString();");
            append(lines, "com.jsoniter.any.Any value = (com.jsoniter.any.Any)entry.getValue();");
            append(lines, String.format("obj.%s(key, value.object());", wrapper.getName()));
        }
        append(lines, "}");
    }

    private static void appendSetExtraProperteis(StringBuilder lines, ClassDescriptor desc) {
        Binding onExtraProperties = desc.getOnExtraProperties();
        if (GenericsHelper.isSameClass(onExtraProperties.getValueType(), Map.class)) {
            if (onExtraProperties.getField() != null) {
                append(lines, String.format("obj.%s = extra;", onExtraProperties.getField().getName()));
            } else {
                append(lines, String.format("obj.%s(extra);", onExtraProperties.getMethod().getName()));
            }
            return;
        }
        throw new JsonException("extra properties can only be Map");
    }

    private static boolean hasAnythingToBindFrom(List<Binding> allBindings) {
        for (Binding binding : allBindings) {
            if (binding.getFromNames().length > 0) {
                return true;
            }
        }
        return false;
    }

    private static int assignMaskForRequiredProperties(List<Binding> allBindings) {
        int requiredIdx = 0;
        for (Binding binding : allBindings) {
            if (binding.isAsMissingWhenNotPresent()) {
                // one bit represent one field
                binding.setMask(1L << requiredIdx);
                requiredIdx++;
            }
        }
        if (requiredIdx > 63) {
            throw new JsonException("too many required properties to track");
        }
        return requiredIdx;
    }

    private static String updateBindingSetOp(String rendered, Binding binding) {
        if (binding.getFromNames().length == 0) {
            return rendered;
        }
        while (true) {
            String marker = "_" + binding.getName() + "_";
            int start = rendered.indexOf(marker);
            if (start == -1) {
                return rendered;
            }
            int middle = rendered.indexOf('=', start);
            if (middle == -1) {
                throw new JsonException("can not find = in: " + rendered + " ,at " + start);
            }
            middle += 1;
            int end = rendered.indexOf(';', start);
            if (end == -1) {
                throw new JsonException("can not find ; in: " + rendered + " ,at " + start);
            }
            String op = rendered.substring(middle, end);
            if (binding.getField() != null) {
                if (binding.isValueCanReuse()) {
                    // reuse; then field set
                    rendered = String.format("%scom.jsoniter.CodegenAccess.setExistingObject(iter, obj.%s);obj.%s=%s%s",
                            rendered.substring(0, start), binding.getField().getName(), binding.getField().getName(), op, rendered.substring(end));
                } else {
                    // just field set
                    rendered = String.format("%sobj.%s=%s%s",
                            rendered.substring(0, start), binding.getField().getName(), op, rendered.substring(end));
                }
            } else {
                // method set
                rendered = String.format("%sobj.%s(%s)%s",
                        rendered.substring(0, start), binding.getMethod().getName(), op, rendered.substring(end));
            }
        }
    }

    private static void appendMissingRequiredProperties(StringBuilder lines, ClassDescriptor desc) {
        append(lines, "java.util.List missingFields = new java.util.ArrayList();");
        for (Binding binding : desc.allDecoderBindings()) {
            if (binding.isAsMissingWhenNotPresent()) {
                long mask = binding.getMask();
                append(lines, String.format("com.jsoniter.CodegenAccess.addMissingField(missingFields, tracker, %sL, \"%s\");",
                        mask, binding.getName()));
            }
        }

        if (desc.getOnMissingProperties() == null || !desc.getCtor().getParameters().isEmpty()) {

            append(lines, "throw new com.jsoniter.spi.JsonException(\"missing required properties: \" + missingFields);");
        } else {
            if (desc.getOnMissingProperties().getField() != null) {
                append(lines, String.format("obj.%s = missingFields;", desc.getOnMissingProperties().getField().getName()));
            } else {
                append(lines, String.format("obj.%s(missingFields);", desc.getOnMissingProperties().getMethod().getName()));
            }
        }
    }

    private static void appendOnUnknownField(StringBuilder lines, ClassDescriptor desc) {
        if (desc.isAsExtraForUnknownProperties() && desc.getOnExtraProperties() == null) {
            append(lines, "throw new com.jsoniter.spi.JsonException('extra property: ' + field.toString());".replace('\'', '"'));
        } else {
            if (desc.isAsExtraForUnknownProperties() || !desc.getKeyValueTypeWrappers().isEmpty()) {
                append(lines, "if (extra == null) { extra = new java.util.HashMap(); }");
                append(lines, "extra.put(field.toString(), iter.readAny());");
            } else {
                append(lines, CON1);
            }
        }
    }

    private static Map<Integer, Object> buildTriTree(List<Binding> allBindings) {
        Map<Integer, Object> trieTree = new HashMap<Integer, Object>();
        for (Binding field : allBindings) {
            for (String fromName : field.getFromNames()) {
                byte[] fromNameBytes = fromName.getBytes();
                Map<Byte, Object> current = (Map<Byte, Object>) trieTree.get(fromNameBytes.length);
                if (current == null) {
                    current = new HashMap<Byte, Object>();
                    trieTree.put(fromNameBytes.length, current);
                }
                for (int i = 0; i < fromNameBytes.length - 1; i++) {
                    byte b = fromNameBytes[i];
                    Map<Byte, Object> next = (Map<Byte, Object>) current.get(b);
                    if (next == null) {
                        next = new HashMap<Byte, Object>();
                        current.put(b, next);
                    }
                    current = next;
                }
                current.put(fromNameBytes[fromNameBytes.length - 1], field);
            }
        }
        return trieTree;
    }

    private static String renderTriTree(Map<Integer, Object> trieTree) {
        StringBuilder switchBody = new StringBuilder();
        for (Map.Entry<Integer, Object> entry : trieTree.entrySet()) {
            Integer len = entry.getKey();
            append(switchBody, "case " + len + ": ");
            Map<Byte, Object> current = (Map<Byte, Object>) entry.getValue();
            addFieldDispatch(switchBody, len, 0, current, new ArrayList<Byte>());
            append(switchBody, "break;");
        }
        return switchBody.toString();
    }

    private static void addFieldDispatch(
            StringBuilder lines, int len, int i, Map<Byte, Object> current, List<Byte> bytesToCompare) {
        for (Map.Entry<Byte, Object> entry : current.entrySet()) {
            Byte b = entry.getKey();
            if (i == len - 1) {
                append(lines, "if (");
                for (int j = 0; j < bytesToCompare.size(); j++) {
                    Byte a = bytesToCompare.get(j);
                    append(lines, String.format("field.at(%d)==%s && ", i - bytesToCompare.size() + j, a));
                }
                append(lines, String.format("field.at(%d)==%s", i, b));
                append(lines, ") {");
                Binding field = (Binding) entry.getValue();
                if (field.isAsExtraWhenPresent()) {
                    append(lines, String.format(
                            "throw new com.jsoniter.spi.JsonException('extra property: %s');".replace('\'', '"'),
                            field.getName()));
                } else if (field.isShouldSkip()) {
                    append(lines, CON1);
                    append(lines, "continue;");
                } else {
                    append(lines, String.format("_%s_ = %s;", field.getName(), CodegenImplNative.genField(field)));
                    if (field.isAsMissingWhenNotPresent()) {
                        append(lines, "tracker = tracker | " + field.getMask() + "L;");
                    }
                    append(lines, "continue;");
                }
                append(lines, "}");
                continue;
            }
            Map<Byte, Object> next = (Map<Byte, Object>) entry.getValue();
            
            field(next, lines, len, i, bytesToCompare, b);
            
        }
    }
    
    public static void field(Map<Byte, Object> next, StringBuilder lines, int len, int i, List<Byte> bytesToCompare, Byte b ) {
    	
    	if (next.size() == 1) {
            ArrayList<Byte> nextBytesToCompare = new ArrayList<Byte>(bytesToCompare);
            nextBytesToCompare.add(b);
            addFieldDispatch(lines, len, i + 1, next, nextBytesToCompare);
        }
        append(lines, "if (");
        for (int j = 0; j < bytesToCompare.size(); j++) {
            Byte a = bytesToCompare.get(j);
            append(lines, String.format("field.at(%d)==%s && ", i - bytesToCompare.size() + j, a));
        }
        append(lines, String.format("field.at(%d)==%s", i, b));
        append(lines, ") {");
        addFieldDispatch(lines, len, i + 1, next, new ArrayList<Byte>());
        append(lines, "}");
    }

    public static String genObjectUsingSkip(Class clazz, ConstructorDescriptor ctor) {
        StringBuilder lines = new StringBuilder();
        append(lines, "if (iter.readNull()) { return null; }");
        append(lines, "{{clazz}} obj = {{newInst}};");
        append(lines, CON1);
        append(lines, CON);
        return lines.toString()
                .replace("{{clazz}}", clazz.getCanonicalName())
                .replace("{{newInst}}", CodegenImplObjectHash.genNewInstCode(clazz, ctor));
    }

    static void append(StringBuilder lines, String str) {
        lines.append(str);
        lines.append("\n");
    }
}
