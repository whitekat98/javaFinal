package com.jsoniter.any;

import com.jsoniter.ValueType;
import com.jsoniter.output.JsonStream;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

class FalseAny extends Any {

    public static final FalseAny INSTANCE = new FalseAny();



    @Override
    public Object object() {
        return Boolean.FALSE;
    }

    @Override
    public ValueType valueType() {
        return ValueType.BOOLEAN;
    }
    @Override
    public int toInt() {
        return 0;
    }
    @Override
    public BigDecimal toBigDecimal() {
        return BigDecimal.ZERO;
    }

    @Override
    public boolean toBoolean() {
        return false;
    }

    @Override
    public float toFloat() {
        return 0;
    }

    @Override
    public double toDouble() {
        return 0;
    }
    @Override
    public long toLong() {
        return 0;
    }

    @Override
    public String toString() {
        return "false";
    }
    @Override
    public BigInteger toBigInteger() {
        return BigInteger.ZERO;
    }



    @Override
    public void writeTo(JsonStream stream) throws IOException {
        stream.writeFalse();
    }
    
    
    @Override
    public boolean equals(Object obj) {
    	boolean tmp;
        if (! super.equals(obj)) {
          return false;
        }
        FalseAny fobj = (FalseAny) obj;
        if(Boolean.FALSE.equals(fobj.object())) {
        	tmp = true;
        	} else {
        		tmp = false;
        	}                
        return tmp;
        }
    
    @Override
    public int hashCode() {
        return 1;
    }
}
