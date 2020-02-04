package com.jsoniter.any;

import com.jsoniter.JsonIterator;
import com.jsoniter.JsonIteratorPool;
import com.jsoniter.spi.JsonException;
import com.jsoniter.ValueType;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

class LongLazyAny extends LazyAny {

    private boolean isCached;
    private long cache;

    public LongLazyAny(byte[] data, int head, int tail) {
        super(data, head, tail);
    }

    @Override
    public ValueType valueType() {
        return ValueType.NUMBER;
    }

    @Override
    public Object object() {
        return toLong();
    }

    @Override
    public boolean toBoolean() {
        fillCache();
        return cache != 0;
    }

    @Override
    public int toInt() {
        fillCache();
        return (int) cache;
    }

    @Override
    public long toLong() {
        return (long) toFloat();
    }

    @Override
    public float toFloat() {
        return (float) toDouble();
    }

    @Override
    public double toDouble() {
        fillCache();
        return cache;
    }

    @Override
    public BigInteger toBigInteger() {
        return new BigInteger(toString());
    }

    @Override
    public BigDecimal toBigDecimal() {
        return new BigDecimal(toString());
    }

    private void fillCache() {
        if (!isCached) {
            JsonIterator iter = parse();
            try {
                cache = iter.readLong();
            } catch (IOException e) {
                throw new JsonException(e);
            } finally {
                JsonIteratorPool.returnJsonIterator(iter);
            }
            isCached = true;
        }
    }
    
    @Override
    public boolean equals(Object obj) {
    	boolean tmp;
    	Long vall = cache;
        if (! super.equals(obj)) {
          return false;
        }
        LongLazyAny fobj = (LongLazyAny) obj;
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
