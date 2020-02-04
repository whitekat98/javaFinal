package com.jsoniter.any;

import com.jsoniter.ValueType;
import com.jsoniter.output.JsonStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

class FloatAny extends Any {

    private float val;

    public FloatAny(float val) {
        this.val = val;
    }


    @Override
    public boolean toBoolean() {
        return val != 0;
    }


    @Override
    public Object object() {
        return val;
    }
 

    @Override
    public double toDouble() {
        return val;
    }
   @Override
    public ValueType valueType() {
        return ValueType.NUMBER;
    }
    @Override
    public long toLong() {
        return (long) val;
    }
    @Override
    public int toInt() {
        return (int) val;
    }


    @Override
    public float toFloat() {
        return val;
    }
    @Override
    public BigInteger toBigInteger() {
        return BigInteger.valueOf((long) val);
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
    public Any set(float newVal) {
        this.val = newVal;
        return this;
    }

    @Override
    public void writeTo(JsonStream stream) throws IOException {
        stream.writeVal(val);
    }
    
    @Override
    public boolean equals(Object obj) {
    	Float vall = val;
    	boolean tmp;
        if (! super.equals(obj)) {
          return false;
        }
        FloatAny fobj = (FloatAny) obj;
        if (vall.equals(fobj.object())){  // added fields are tested
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
