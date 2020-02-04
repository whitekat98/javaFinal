package com.jsoniter;

import java.io.IOException;

class IterImplObject {
	
	private static final String CON = "expect :";
	private static final String CON1 = "readObject";
	
	private IterImplObject() {
	    throw new IllegalStateException("Utility class");
	}

    public static final String readObject(JsonIterator iter) throws IOException {
        byte c = IterImpl.nextToken(iter);
        switch (c) {
            case 'n':
                IterImpl.skipFixedBytes(iter, 3);
                return null;
            case '{':
                c = IterImpl.nextToken(iter);
                if (c == '"') {
                    iter.unreadByte();
                    String field = iter.readString();
                    if (IterImpl.nextToken(iter) != ':') {
                        throw iter.reportError(CON1, CON);
                    }
                    return field;
                }
                if (c == '}') {
                    return null; // end of object
                }
                throw iter.reportError(CON1, "expect \" after {");
            case ',':
                String field = iter.readString();
                if (IterImpl.nextToken(iter) != ':') {
                    throw iter.reportError(CON1, CON);
                }
                return field;
            case '}':
                return null; // end of object
            default:
                throw iter.reportError(CON1, "expect { or , or } or n, but found: " + (char)c);
        }
    }

    public static final boolean readObjectCB(JsonIterator iter, JsonIterator.ReadObjectCallback cb, Object attachment) throws IOException {
        byte c = IterImpl.nextToken(iter);
        if ('{' == c) {
            c = IterImpl.nextToken(iter);
            
            rad(c, iter, attachment, cb);
            
            if ('}' == c) {
                return true;
            }
            throw iter.reportError("readObjectCB", "expect \" after {");
        }
        if ('n' == c) {
            IterImpl.skipFixedBytes(iter, 3);
            return true;
        }
        throw iter.reportError("readObjectCB", "expect { or n");
    }
    
    
    private static boolean rad(byte c, JsonIterator iter, Object attachment, JsonIterator.ReadObjectCallback cb) throws IOException {
    	
    	if ('"' == c) {
            iter.unreadByte();
            String field = iter.readString();
            if (IterImpl.nextToken(iter) != ':') {
                throw iter.reportError(CON1, CON);
            }
            if (!cb.handle(iter, field, attachment)) {
                return false;
            }
            while (IterImpl.nextToken(iter) == ',') {
                field = iter.readString();
                if (IterImpl.nextToken(iter) != ':') {
                    throw iter.reportError(CON1, CON);
                }
                if (!cb.handle(iter, field, attachment)) {
                    return false;
                }
            }
            return true;
        }
		return false;
    }
}
