package com.jsoniter.any;

import com.jsoniter.ValueType;
import com.jsoniter.output.JsonStream;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

class LongAny extends Any {

    private long val;

    public LongAny(long val) {
        this.val = val;
    }

    @Override
    public BigDecimal toBigDecimal() {
        return BigDecimal.valueOf(val);
    }

    @Override
    public String toString() {
        return String.valueOf(val);
    }

   @Override
    public Object object() {
        return val;
    }

    @Override
    public long toLong() {
        return val;
    }
    @Override
    public double toDouble() {
        return val;
    }

    @Override
    public int toInt() {
        return (int) val;
    }
    @Override
    public ValueType valueType() {
        return ValueType.NUMBER;
    }

 
    @Override
    public float toFloat() {
        return val;
    }


    @Override
    public BigInteger toBigInteger() {
        return BigInteger.valueOf(val);
    }

    @Override
    public Any set(long newVal) {
        this.val = newVal;
        return this;
    }

    @Override
    public boolean toBoolean() {
        return val != 0;
    }

    @Override
    public void writeTo(JsonStream stream) throws IOException {
        stream.writeVal(val);
    }
    
    @Override
    public boolean equals(Object obj) {
    	boolean tmp;
    	Long vall = val;
        if (! super.equals(obj)) {
          return false;
        }
        LongAny fobj = (LongAny) obj;
        if(vall.equals(fobj.object())) {
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
