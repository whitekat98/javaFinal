package com.jsoniter.any;

import com.jsoniter.ValueType;
import com.jsoniter.output.JsonStream;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

class DoubleAny extends Any {

    private double val;

    public DoubleAny(double val) {
        this.val = val;
    }

    
   
    @Override
    public Object object() {
        return val;
    }
    @Override
    public ValueType valueType() {
        return ValueType.NUMBER;
    }


   

    @Override
    public BigInteger toBigInteger() {
        return BigInteger.valueOf((long) val);
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
    public boolean toBoolean() {
        return val != 0;
    }
    
    @Override
    public void writeTo(JsonStream stream) throws IOException {
        stream.writeVal(val);
    }
    

    @Override
    public String toString() {
        return String.valueOf(val);
    }

    @Override
    public float toFloat() {
        return (float) val;
    }
   
  
    @Override
    public Any set(double newVal) {
        this.val = newVal;
        return this;
    }

    @Override
    public long toLong() {
        return (long) val;
    } 
    @Override
    public BigDecimal toBigDecimal() {
        return BigDecimal.valueOf(val);
    }


    @Override
    public boolean equals(Object obj) {
    	boolean tmp;
    	Double vall = val;
        if (! super.equals(obj)) {
          return false;
        }
        DoubleAny fobj = (DoubleAny) obj;
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
