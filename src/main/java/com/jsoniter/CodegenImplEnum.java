package com.jsoniter;

import com.jsoniter.spi.ClassInfo;

import java.util.*;

class CodegenImplEnum {
	
	private CodegenImplEnum() {
	    throw new IllegalStateException("Utility class");
	}
	
    public static String genEnum(ClassInfo classInfo) {
        StringBuilder lines = new StringBuilder();
        append(lines, "if (iter.readNull()) { return null; }");
        append(lines, "com.jsoniter.spi.Slice field = com.jsoniter.CodegenAccess.readSlice(iter);");
        append(lines, "switch (field.len()) {");
        append(lines, renderTriTree(buildTriTree(Arrays.asList(classInfo.clazz.getEnumConstants()))));
        append(lines, "}"); // end of switch
        append(lines, String.format("throw iter.reportError(\"decode enum\", field + \" is not valid enum for %s\");", classInfo.clazz.getName()));
        return lines.toString();
    }

    private static Map<Integer, Object> buildTriTree(List<Object> allConsts) {
        Map<Integer, Object> trieTree = new HashMap<Integer, Object>();
        for (Object e : allConsts) {
        	iterateObjects(trieTree, e, e.toString());
        }
        return trieTree;
    }

        
        
    private static String renderTriTree(Map<Integer, Object> trieTree) {
        StringBuilder switchBody = new StringBuilder();
        for (Map.Entry<Integer, Object> entry : trieTree.entrySet()) {
            Integer len = entry.getKey();
            Map<Byte, Object> current = (Map<Byte, Object>) entry.getValue();
            addFieldDispatch(switchBody, len, 0, current, new ArrayList<Byte>());
            append(switchBody, "case " + len + ": ");
           
            append(switchBody, "break;");
        }
        return switchBody.toString();
    }

    private static void addFieldDispatch(
            StringBuilder lines, int len, int i, Map<Byte, Object> current, List<Byte> bytesToCompare) {
        for (Map.Entry<Byte, Object> entry : current.entrySet()) {
            boolean flagContinue1 = false;
            boolean flagContinue2 = false;
            Byte b = entry.getKey();
            if (i == len - 1) {
                addFieldDispatchAppend(lines, i, bytesToCompare, b);
                Object e = entry.getValue();
                append(lines, String.format("return %s.%s;", e.getClass().getName(), e.toString()));
                append(lines, "}");
                flagContinue1 = true;

            }
            if (!flagContinue1) {
                Map<Byte, Object> next = (Map<Byte, Object>) entry.getValue();
                if (next.size() == 1) {
                    ArrayList<Byte> nextBytesToCompare = new ArrayList<Byte>(bytesToCompare);
                    nextBytesToCompare.add(b);
                    addFieldDispatch(lines, len, i + 1, next, nextBytesToCompare);
                    flagContinue2 = true;
                }
                if (!flagContinue2) {
                    addFieldDispatchAppend(lines, i, bytesToCompare, b);
                    addFieldDispatch(lines, len, i + 1, next, new ArrayList<Byte>());
                    append(lines, "}");
                }
            }
        }
    }

    static void iterateObjects(Map<Integer, Object> trieTree, Object e, String s) {
        byte[] fromNameBytes = s.getBytes();
        Map<Byte, Object> current = (Map<Byte, Object>) trieTree.get(fromNameBytes.length);
        
        for (int i = 0; i < fromNameBytes.length - 1; i++) {
            byte b = fromNameBytes[i];
            Map<Byte, Object> next = (Map<Byte, Object>) current.get(b);
            if (next == null) {
                next = new HashMap<Byte, Object>();
                current.put(b, next);
            }
            current = next;
        }if (current == null) {
            current = new HashMap<Byte, Object>();
            trieTree.put(fromNameBytes.length, current);
        }
        current.put(fromNameBytes[fromNameBytes.length - 1], e);
    }
    
    private static void addFieldDispatchAppend(StringBuilder lines, int i, List<Byte> bytesToCompare, Byte b) {
        append(lines, "if (");
        for (int j = 0; j < bytesToCompare.size(); j++) {
            Byte a = bytesToCompare.get(j);
            append(lines, String.format("field.at(%d)==%s && ", i - bytesToCompare.size() + j, a));
        }
        append(lines, String.format("field.at(%d)==%s", i, b));
        append(lines, ") {");
    }

    private static void append(StringBuilder lines, String str) {
        lines.append(str);
        lines.append("\n");
    }
}
