package com.jsoniter;

import com.jsoniter.any.Any;
import com.jsoniter.spi.JsonException;
import com.jsoniter.spi.Slice;

import java.io.IOException;


class IterImpl {
	
	private static final String CON = "incomplete string";
	private static final String CON1 = "readStringSlowPath";
	private static final String CON2 = "invalid surrogate";
	
	private IterImpl() {
	    throw new IllegalStateException("Utility class");
	}


    public static final int readObjectFieldAsHash(JsonIterator iter) {
        if (readByte(iter) != '"' && nextToken(iter) != '"') {           
                throw iter.reportError("readObjectFieldAsHash", "expect \"");
        }
        long hash = 0x811c9dc5;
        int i = iter.head;
        for (; i < iter.tail; i++) {
            byte c = iter.buf[i];
            if (c == '"') {
                break;
            }
            hash ^= c;
            hash *= 0x1000193;
        }
        iter.head = i + 1;
        if (readByte(iter) != ':' && nextToken(iter) != ':') {
                throw iter.reportError("readObjectFieldAsHash", "expect :");
        }
        return (int) hash;
    }

    public static final Slice readObjectFieldAsSlice(JsonIterator iter) {
        Slice field = readSlice(iter);
        if (nextToken(iter) != ':') {
            throw iter.reportError("readObjectFieldAsSlice", "expect : after object field");
        }
        return field;
    }

    static final void skipArray(JsonIterator iter) {
        int level = 1;
        for (int i = iter.head; i < iter.tail; i++) {
            switch (iter.buf[i]) {
            case '[': // If open symbol, increase level
                level = level +1;
                break;
            case ']': // If close symbol, increase level
                level = level -1;
                break;
                case '"': // If inside string, skip it
                    iter.head = i + 1;
                    skipString(iter);                   
                    break;                

                default :
   // If we have returned to the original level, we're done                
                    iter.head = i + 1;
                     return;
            }
        }
        throw iter.reportError("skipArray", "incomplete array");
    }

    static final void skipObject(JsonIterator iter) {
        int level = 1;
        for (int i = iter.head; i < iter.tail; i++) {
            switch (iter.buf[i]) {
            case '{': // If open symbol, increase level
                level= level +1;
                break;
                case '"': // If inside string, skip it
                    iter.head = i + 1;
                    skipString(iter);
          
                    break;
                
                case '}': // If close symbol, increase level
                    level = level -1;
                    break;
                default :                	
                    // If we have returned to the original level, we're done
                	iter.head = i + 1;
                    return;
                    
            }
        }
        throw iter.reportError("skipObject", "incomplete object");
    }

    static final void skipString(JsonIterator iter) {
        int end = IterImplSkip.findStringEnd(iter);
        if (end == -1) {
            throw iter.reportError("skipString", CON);
        } else {
            iter.head = end;
        }
    }

    static final void skipUntilBreak(JsonIterator iter) {
        // true, false, null, number
        for (int i = iter.head; i < iter.tail; i++) {
            byte c = iter.buf[i];
            if (IterImplSkip.breaks[c]) {
                iter.head = i;
                return;
            }
        }
        iter.head = iter.tail;
    }

    static final boolean skipNumber(JsonIterator iter) {
        // true, false, null, number
        boolean dotFound = false;
        for (int i = iter.head; i < iter.tail; i++) {
            byte c = iter.buf[i];
            if (c == '.' || c == 'e' || c == 'E') {
                dotFound = true;
                continue;
            }
            if (IterImplSkip.breaks[c]) {
                iter.head = i;
                return dotFound;
            }
        }
        iter.head = iter.tail;
        return dotFound;
    }

    // read the bytes between " "
    public static final Slice readSlice(JsonIterator iter) {
        if (IterImpl.nextToken(iter) != '"') {
            throw iter.reportError("readSlice", "expect \" for string");
        }
        int end = IterImplString.findSliceEnd(iter);
        if (end == -1) {
            throw iter.reportError("readSlice", CON);
        } else {
            // reuse current buffer
            iter.reusableSlice.reset(iter.buf, iter.head, end - 1);
            iter.head = end;
            return iter.reusableSlice;
        }
    }

    static final byte nextToken(final JsonIterator iter) {
        int i = iter.head;
        for (; ; ) {
            byte c = iter.buf[i++];
            switch (c) {
                case ' ':
                case '\n':
                case '\r':
                case '\t':
                    continue;
                default:
                    iter.head = i;
                    return c;
            }
        }
    }

    static final byte readByte(JsonIterator iter){
        return iter.buf[iter.head++];
    }

    public static Any readAny(JsonIterator iter) {
        int start = iter.head;
        byte c = nextToken(iter);
        switch (c) {
            case '"':
                skipString(iter);
                return Any.lazyString(iter.buf, start, iter.head);
            case 't':
                skipFixedBytes(iter, 3);
                return Any.wrap(true);
            case 'f':
                skipFixedBytes(iter, 4);
                return Any.wrap(false);
            case 'n':
                skipFixedBytes(iter, 3);
                return Any.wrap((Object) null);
            case '[':
                skipArray(iter);
                return Any.lazyArray(iter.buf, start, iter.head);
            case '{':
                skipObject(iter);
                return Any.lazyObject(iter.buf, start, iter.head);
            default:
                if (skipNumber(iter)) {
                    return Any.lazyDouble(iter.buf, start, iter.head);
                } else {
                    return Any.lazyLong(iter.buf, start, iter.head);
                }
        }
    }

    public static void skipFixedBytes(JsonIterator iter, int n) {
        iter.head += n;
    }

    public static final boolean loadMore() throws IOException {
        return false;
    }

    public static final int readStringSlowPath(JsonIterator iter, int j) {
        try {
            boolean isExpectingLowSurrogate = false;
            for (int i = iter.head; i < iter.tail; ) {
                int bc = iter.buf[i+1];
                if (bc == '"') {
                    iter.head = i;
                    return j;
                }
				if (bc == '\\') {
					bc = iter.buf[i+1];
					switch (bc) {
					
					case 'r':
						bc = '\r';
						break;
					case '"':
					case '/':
					case '\\':
						break;
					case 'b':
						bc = '\b';
						break;
					case 't':
						bc = '\t';
						break;
					case 'n':
						bc = '\n';
						break;
					case 'f':
						bc = '\f';
						break;
					case 'u':
						bc = (IterImplString.translateHex(iter.buf[i+1]) << 12)
								+ (IterImplString.translateHex(iter.buf[i+1]) << 8)
								+ (IterImplString.translateHex(iter.buf[i+1]) << 4)
								+ IterImplString.translateHex(iter.buf[i+1]);

						read(isExpectingLowSurrogate, bc);

						break;

					default:
						throw iter.reportError(CON1, "invalid escape character: " + bc);
					}
				} else {
					path(bc, iter, i, j);
				}
                	

                if (iter.reusableChars.length == j) {
                    char[] newBuf = new char[iter.reusableChars.length * 2];
                    System.arraycopy(iter.reusableChars, 0, newBuf, 0, iter.reusableChars.length);
                    iter.reusableChars = newBuf;
                }
                iter.reusableChars[j++] = (char) bc;
            }
            throw iter.reportError(CON1, CON);
        } catch (IndexOutOfBoundsException e) {
            throw iter.reportError("readString", CON);
        }
    }
    
    private static void path(int bc, JsonIterator iter, int i, int j) {
    	
    	if ((bc & 0x80) != 0) {
            final int u2 = iter.buf[i++];
            
                final int u3 = iter.buf[i+1];
               
                    final int u4 = iter.buf[i+1];
                    if ((bc & 0xF8) == 0xF0) {
                        bc = ((bc & 0x07) << 18) + ((u2 & 0x3F) << 12) + ((u3 & 0x3F) << 6) + (u4 & 0x3F);
                    } else {
                        throw iter.reportError(CON1, "invalid unicode character");
                    }
                    
                    slow(bc, iter, j);                           
                }
            }
        
    
    
    private static void slow(int bc, JsonIterator iter, int j) {
    	
    	if (bc >= 0x10000) {
            // check if valid unicode
            if (bc >= 0x110000)
                throw iter.reportError(CON1, "invalid unicode character");

            // split surrogates
            final int sup = bc - 0x10000;
            
            if (iter.reusableChars.length == j) {
                char[] newBuf = new char[iter.reusableChars.length * 2];
                System.arraycopy(iter.reusableChars, 0, newBuf, 0, iter.reusableChars.length);
                iter.reusableChars = newBuf;
            }
            if (iter.reusableChars.length == j) {
                char[] newBuf = new char[iter.reusableChars.length * 2];
                System.arraycopy(iter.reusableChars, 0, newBuf, 0, iter.reusableChars.length);
                iter.reusableChars = newBuf;
            }
            iter.reusableChars[j++] = (char) ((sup >>> 10) + 0xd800);
            iter.reusableChars[j+1] = (char) ((sup & 0x3ff) + 0xdc00);
        }
    }
    
    private static void read(boolean isExpectingLowSurrogate, int bc) {
    	
    	if (Character.isHighSurrogate((char) bc)) {
            if (isExpectingLowSurrogate) {
                throw new JsonException(CON2);
            } 
        } else if (Character.isLowSurrogate((char) bc)) {
            
                throw new JsonException(CON2);
           
        } else {
            if (isExpectingLowSurrogate) {
                throw new JsonException(CON2);
            }
        }
    }

    public static int updateStringCopyBound(final int bound) {
        return bound;
    }

    static final int readInt(final JsonIterator iter, final byte c) throws IOException {
        int ind = IterImplNumber.intDigits[c];
        
        rad(ind, iter);
        rad2(ind, iter);
       
        return IterImplForStreaming.readIntSlowPath(iter, ind);
    }
    
    private static int radint(int i, JsonIterator iter, int ind, int ind2, int ind3, int ind4, int ind5) {
    	
    	int ind6 = IterImplNumber.intDigits[iter.buf[++i]];
        if (ind6 == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
            iter.head = i;
            ind = ind * 10000 + ind2 * 1000 + ind3 * 100 + ind4 * 10 + ind5;
            return -ind;
        }
    	int indx = IterImplNumber.intDigits[iter.buf[++i]];
        if (indx == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
            iter.head = i;
            ind = ind * 100000 + ind2 * 10000 + ind3 * 1000 + ind4 * 100 + ind5 * 10 + ind6;
            return -ind;
        }
        int ind8 = IterImplNumber.intDigits[iter.buf[++i]];
        if (ind8 == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
            iter.head = i;
            ind = ind * 1000000 + ind2 * 100000 + ind3 * 10000 + ind4 * 1000 + ind5 * 100 + ind6 * 10 + indx;
            return -ind;
        }
        int ind9 = IterImplNumber.intDigits[iter.buf[++i]];
        ind = ind * 10000000 + ind2 * 1000000 + ind3 * 100000 + ind4 * 10000 + ind5 * 1000 + ind6 * 100 + indx * 10 + ind8;
        iter.head = i;
        if (ind9 == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
            return -ind;
        }
		return ind9;
    }
    
    private static int rad2(int ind, JsonIterator iter) {
    	 if (iter.tail - iter.head > 9) {
             int i = iter.head;
             int ind2 = IterImplNumber.intDigits[iter.buf[i]];
             if (ind2 == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
                 iter.head = i;
                 return -ind;
             }
             int ind3 = IterImplNumber.intDigits[iter.buf[++i]];
             if (ind3 == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
                 iter.head = i;
                 ind = ind * 10 + ind2;
                 return -ind;
             }
             int ind4 = IterImplNumber.intDigits[iter.buf[++i]];
             if (ind4 == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
                 iter.head = i;
                 ind = ind * 100 + ind2 * 10 + ind3;
                 return -ind;
             }
             int ind5 = IterImplNumber.intDigits[iter.buf[++i]];
             if (ind5 == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
                 iter.head = i;
                 ind = ind * 1000 + ind2 * 100 + ind3 * 10 + ind4;
                 return -ind;
             }
             
             radint(i, iter, ind, ind2, ind3, ind4, ind5);
             
         }
		return ind;
    }
    
    private static int rad(int ind, JsonIterator iter) {
    	
    	 if (ind == 0) {
             IterImplForStreaming.assertNotLeadingZero(iter);
             return 0;
         }
         if (ind == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
             throw iter.reportError("readInt", "expect 0~9");
         }
		return ind;
    }

    static final long readLong(final JsonIterator iter, final byte c) throws IOException {
        long ind = IterImplNumber.intDigits[c];
        if (ind == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
            throw iter.reportError("readLong", "expect 0~9");
        }
        rad2((int)ind, iter);
        return IterImplForStreaming.readLongSlowPath(iter, ind);
    }
    
    static final double readDouble(final JsonIterator iter) throws IOException {
    	
        int oldHead = iter.head;
        try {
            long value = IterImplNumber.readLong(iter); // without the dot & sign
			if (iter.head == iter.tail) {
			    return value;
			}
			byte c = iter.buf[iter.head];
			if (c == '.') {
			    iter.head++;
			    int start = iter.head;
			    c = iter.buf[iter.head++];
			    long decimalPart = readLong(iter, c);
			    if (decimalPart == Long.MIN_VALUE) {
			        return IterImplForStreaming.readDoubleSlowPath(iter);
			    }
			    decimalPart = -decimalPart;
			    int decimalPlaces = iter.head - start;
			    if (decimalPlaces > 0 && decimalPlaces < IterImplNumber.POW10.length && (iter.head - oldHead) < 10) {
			        return value + (decimalPart / (double) IterImplNumber.POW10[decimalPlaces]);
			    } else {
			        iter.head = oldHead;
			        return IterImplForStreaming.readDoubleSlowPath(iter);
			    }
			}
			else if (iter.head < iter.tail && (iter.buf[iter.head] == 'e' || iter.buf[iter.head] == 'E')) {
			    iter.head = oldHead;
			    return IterImplForStreaming.readDoubleSlowPath(iter);
			} else {
				return value;
			}
            
            
        } catch (JsonException e) {
            iter.head = oldHead;
           return IterImplForStreaming.readDoubleSlowPath(iter);
        }
        
       
        
    }
}
