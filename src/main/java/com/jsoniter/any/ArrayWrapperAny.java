package com.jsoniter.any;

import com.jsoniter.ValueType;
import com.jsoniter.output.JsonStream;

import java.io.IOException;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

class ArrayWrapperAny extends Any {

    private final Object val;
    private List<Any> cache;

    public ArrayWrapperAny(Object val) {
        this.val = val;
    }

   
    @Override
    public boolean toBoolean() {
        return size() != 0;
    }
    
   @Override
    public long toLong() {
        return size();
    }
   
    @Override
    public Object object() {
        fillCache();
        return cache;
    }


    @Override
    public int toInt() {
        return size();
    }
    

    @Override
    public float toFloat() {
        return size();
    }
    

    @Override
    public BigDecimal toBigDecimal() {
        return BigDecimal.valueOf(size());
    }
    @Override
    public ValueType valueType() {
        return ValueType.ARRAY;
    }

  @Override
    public int size() {
        return Array.getLength(val);
    }
    @Override
    public BigInteger toBigInteger() {
        return BigInteger.valueOf(size());
    }

    @Override
    public Any get(int index) {
        return fillCacheUntil(index);
    }

    @Override
    public double toDouble() {
        return size();
    }
    @Override
    public String toString() {
        if (cache == null) {
            return JsonStream.serialize(val);
        } else {
            fillCache();
            return JsonStream.serialize(cache);
        }
    } 
    @Override
    public void writeTo(JsonStream stream) throws IOException {
        if (cache == null) {
            stream.writeVal(val);
        } else {
            fillCache();
            stream.writeVal(cache);
        }
    }





    @Override
    public Any get(Object[] keys, int idj) {
        if (idj == keys.length) {
            return this;
        }
        Object key = keys[idj];
        if (isWildcard(key)) {
            fillCache();
            ArrayList<Any> result = new ArrayList<Any>();
            for (Any element : cache) {
                Any mapped = element.get(keys, idj + 1);
                if (mapped.valueType() != ValueType.INVALID) {
                    result.add(mapped);
                }
            }
            return Any.rewrap(result);
        }
        try {
            return fillCacheUntil((Integer) key).get(keys, idj + 1);
        } catch (IndexOutOfBoundsException e) {
            return new NotFoundAny(keys, idj, object());
        } catch (ClassCastException e) {
            return new NotFoundAny(keys, idj, object());
        }
    }

    @Override
    public Iterator<Any> iterator() {
        return new WrapperIterator();
    }

    private Any fillCacheUntil(int index) {
        if (cache == null) {
            cache = new ArrayList<Any>();
        }
        if (index < cache.size()) {
            return cache.get(index);
        }
        for (int i = cache.size(); i < size(); i++) {
            Any element = Any.wrap(Array.get(val, i));
            cache.add(element);
            if (index == i) {
                return element;
            }
        }
        return new NotFoundAny(index, val);
    }

    private void fillCache() {
        if (cache == null) {
            cache = new ArrayList<Any>();
        }
        int size = size();
        if (cache.size() == size) {
            return;
        }
        for (int i = cache.size(); i < size; i++) {
            Any element = Any.wrap(Array.get(val, i));
            cache.add(element);
        }
    }

    private class WrapperIterator implements Iterator<Any> {

        private int index;
        private final int size;

        private WrapperIterator() {
            size = size();
        }

        @Override
        public boolean hasNext() {
            return index < size;
        }

        @Override
        public Any next() {
        	if(!hasNext()){
                throw new NoSuchElementException();
            }
            if (cache == null) {
                cache = new ArrayList<Any>();
            }
            if (index == cache.size()) {
                cache.add(Any.wrap(Array.get(val, index)));
            }
            return cache.get(index++);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
    
    @Override
    public boolean equals(Object obj) {
    	boolean tmp;
        if (! super.equals(obj)) {
          return false;
        }
        ArrayWrapperAny fobj = (ArrayWrapperAny) obj;
        if(val.equals(fobj.object())) {
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
