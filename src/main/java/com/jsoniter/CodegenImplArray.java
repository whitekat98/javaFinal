package com.jsoniter;

import com.jsoniter.spi.ClassInfo;

import java.lang.reflect.Type;
import java.util.*;

class CodegenImplArray {
	
	private static final String CON = "{{op}}";
	private static final String COR = "if (com.jsoniter.CodegenAccess.nextToken(iter) != ',') {";
	private static final String OB = "OBj.add(a1);";
	private static final String OB1 = "OBj.add(a2);";
	private static final String OB2 = "OBj.add(a3);";
	private static final String OBR = "return OBj;";
	private static final String CON1 = "{{clazz}} OBj = col == null ? new {{clazz}}(): ({{clazz}})com.jsoniter.CodegenAccess.reuseCollection(col);";
	
	private CodegenImplArray() {
	    throw new IllegalStateException("Utility class");
	}

    static final Set<Class> WITH_CAPACITY_COLLECTION_CLASSES = new HashSet<Class>();
    static {
    	WITH_CAPACITY_COLLECTION_CLASSES.add(ArrayList.class);
    	WITH_CAPACITY_COLLECTION_CLASSES.add(HashSet.class);
    	WITH_CAPACITY_COLLECTION_CLASSES.add(Vector.class);
    }

    public static String genArray(ClassInfo classInfo) {
    	final String COS = "if (!com.jsoniter.CodegenAccess.nextTokenIsComma(iter)) {";
        Class compType = classInfo.clazz.getComponentType();
        if (compType.isArray()) {
            throw new IllegalArgumentException("nested array not supported: " + classInfo.clazz.getCanonicalName());
        }
        StringBuilder lines = new StringBuilder();

        append(lines, "com.jsoniter.CodegenAccess.resetExistingObject(iter);"+
                "byte nextToken = com.jsoniter.CodegenAccess.readByte(iter);"+
                "if (nextToken != '[') {"+
                "if (nextToken == 'n') {"+
                "com.jsoniter.CodegenAccess.skipFixedBytes(iter, 3);"+
                "com.jsoniter.CodegenAccess.resetExistingObject(iter); return null;"+
                "} else {"+
                "nextToken = com.jsoniter.CodegenAccess.nextToken(iter);"+
                "if (nextToken == 'n') {"+
                "com.jsoniter.CodegenAccess.skipFixedBytes(iter, 3);"+
                "com.jsoniter.CodegenAccess.resetExistingObject(iter); return null;"+
                "}"+
                "}"+
                "}"+
                "nextToken = com.jsoniter.CodegenAccess.nextToken(iter);"+
                "if (nextToken == ']') {"+
                "return new {{comp}}[0];"+
                "}"+
                "com.jsoniter.CodegenAccess.unreadByte(iter);"+
                "{{comp}} a1 = {{op}};"+
                COS+
                "return new {{comp}}[]{ a1 };"+
                "}"+
                "{{comp}} a2 = {{op}};"+
                COS+
                "return new {{comp}}[]{ a1, a2 };"+
                "}"+ "{{comp}} a3 = {{op}};"+
                COS+
                "return new {{comp}}[]{ a1, a2, a3 };"+
                "}"+
                "{{comp}} a4 = ({{comp}}) {{op}};"+
                COS+
                "return new {{comp}}[]{ a1, a2, a3, a4 };"+
                "}"+
                "{{comp}} a5 = ({{comp}}) {{op}};"+
                "{{comp}}[] arr = new {{comp}}[10];"+
                "arr[0] = a1;"+
                "arr[1] = a2;"+
                "arr[2] = a3;"+
                "arr[3] = a4;"+
                "arr[4] = a5;"+
                "int i = 5;"+
                "while (com.jsoniter.CodegenAccess.nextTokenIsComma(iter)) {"+
                "if (i == arr.length) {"+
                "{{comp}}[] newArr = new {{comp}}[arr.length * 2];"+
                "System.arraycopy(arr, 0, newArr, 0, arr.length);"+
                "arr = newArr;"+
                "}"+
                "arr[i++] = {{op}};"+
                "}"+
                "{{comp}}[] result = new {{comp}}[i];"+
                "System.arraycopy(arr, 0, result, 0, i);" +
                "return result;");

        return lines.toString().replace(
                "{{comp}}", compType.getCanonicalName()).replace(
                CON, CodegenImplNative.genReadOp(compType));
    }

    public static String genCollection(ClassInfo classInfo) {
        if (WITH_CAPACITY_COLLECTION_CLASSES.contains(classInfo.clazz)) {
            return CodegenImplArray.genCollectionWithCapacity(classInfo.clazz, classInfo.typeArgs[0]);
        } else {
            return CodegenImplArray.genCollectionWithoutCapacity(classInfo.clazz, classInfo.typeArgs[0]);
        }
    }

    private static String genCollectionWithCapacity(Class clazz, Type compType) {
    	
        StringBuilder lines = new StringBuilder();
        append(lines, "{{clazz}} col = ({{clazz}})com.jsoniter.CodegenAccess.resetExistingObject(iter);");
        append(lines, "if (iter.readNull()) { com.jsoniter.CodegenAccess.resetExistingObject(iter); return null; }");
        append(lines, "if (!com.jsoniter.CodegenAccess.readArrayStart(iter)) {");
        append(lines, "return col == null ? new {{clazz}}(0): ({{clazz}})com.jsoniter.CodegenAccess.reuseCollection(col);");
        append(lines, "}");
        append(lines, "Object a1 = {{op}};");
        append(lines, COR);
        append(lines, "{{clazz}} OBj = col == null ? new {{clazz}}(1): ({{clazz}})com.jsoniter.CodegenAccess.reuseCollection(col);");
        append(lines, OB);
        append(lines, OBR);
        append(lines, "}");
        append(lines, "Object a2 = {{op}};");
        append(lines, COR);
        append(lines, "{{clazz}} OBj = col == null ? new {{clazz}}(2): ({{clazz}})com.jsoniter.CodegenAccess.reuseCollection(col);");
        append(lines, OB);
        append(lines, OB1);
        append(lines, OBR);
        append(lines, "}");
        append(lines, "Object a3 = {{op}};");
        append(lines, COR);
        append(lines, "{{clazz}} OBj = col == null ? new {{clazz}}(3): ({{clazz}})com.jsoniter.CodegenAccess.reuseCollection(col);");
        append(lines, OB);
        append(lines, OB1);
        append(lines, OB2);
        append(lines, OBR);
        append(lines, "}");
        append(lines, "Object a4 = {{op}};");
        append(lines, "{{clazz}} OBj = col == null ? new {{clazz}}(8): ({{clazz}})com.jsoniter.CodegenAccess.reuseCollection(col);");
        append(lines, OB);
        append(lines, OB1);
        append(lines, OB2);
        append(lines, "Obj.add(a4);");
        append(lines, "while (com.jsoniter.CodegenAccess.nextToken(iter) == ',') {");
        append(lines, "Obj.add({{op}});");
        append(lines, "}");
        append(lines, OBR);
        return lines.toString().replace(
                "{{clazz}}", clazz.getName()).replace(
                CON, CodegenImplNative.genReadOp(compType));
    }

    private static String genCollectionWithoutCapacity(Class clazz, Type compType) {
        StringBuilder lines = new StringBuilder();
        append(lines, "if (iter.readNull()) { com.jsoniter.CodegenAccess.resetExistingObject(iter); return null; }");
        append(lines, "{{clazz}} col = ({{clazz}})com.jsoniter.CodegenAccess.resetExistingObject(iter);");
        append(lines, "if (!com.jsoniter.CodegenAccess.readArrayStart(iter)) {");
        append(lines, "return col == null ? new {{clazz}}(): ({{clazz}})com.jsoniter.CodegenAccess.reuseCollection(col);");
        append(lines, "}");
        append(lines, "Object a1 = {{op}};");
        append(lines, COR);
        append(lines, CON1);
        append(lines, OB);
        append(lines, OBR);
        append(lines, "}");
        append(lines, "Object a2 = {{op}};");
        append(lines, COR);
        append(lines, CON1);
        append(lines, OB);
        append(lines, OB1);
        append(lines, OBR);
        append(lines, "}");
        append(lines, "Object a3 = {{op}};");
        append(lines, COR);
        append(lines, CON1);
        append(lines, OB);
        append(lines, OB1);
        append(lines, OB2);
        append(lines, OBR);
        append(lines, "}");
        append(lines, "Object a4 = {{op}};");
        append(lines, CON1);
        append(lines, OB);
        append(lines, OB1);
        append(lines, OB2);
        append(lines, "Obj.add(a4);");
        append(lines, "while (com.jsoniter.CodegenAccess.nextToken(iter) == ',') {");
        append(lines, "Obj.add({{op}});");
        append(lines, "}");

        append(lines, OBR);
        return lines.toString().replace(
                "{{clazz}}", clazz.getName()).replace(
                CON, CodegenImplNative.genReadOp(compType));
    }

    private static void append(StringBuilder lines, String str) {
        lines.append(str);
        lines.append("\n");
    }
}
