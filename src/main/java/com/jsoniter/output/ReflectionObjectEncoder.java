package com.jsoniter.output;

import com.jsoniter.spi.*;
import com.jsoniter.any.Any;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ReflectionObjectEncoder implements Encoder.ReflectionEncoder {

    private final ClassDescriptor desc;
    private final List<EncodeTo> fields = new ArrayList<EncodeTo>();
    private final List<EncodeTo> getters = new ArrayList<EncodeTo>();

    public ReflectionObjectEncoder(ClassInfo classInfo) {
        desc = ClassDescriptor.getEncodingClassDescriptor(classInfo, true);
        for (EncodeTo encodeTo : desc.encodeTos()) {

            Binding binding = encodeTo.getBinding();
            if (binding.getEncoder() == null) {

                // the field encoder might be registered directly
                binding.setEncoder(JsoniterSpi.getEncoder(binding.encoderCacheKey()));
            }
            if (binding.getField() != null) {
                fields.add(encodeTo);
            } else {
                getters.add(encodeTo);
            }
        }
    }

    @Override
    public void encode(Object obj, JsonStream stream) throws IOException {
        try {
            encod(obj, stream);
        } catch (JsonException e) {
            throw e;
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    @Override
    public Any wrap(Object obj) {
        HashMap<String, Object> copied = new HashMap<String, Object>();
        try {
            for (EncodeTo encodeTo : fields) {

                Object val = encodeTo.getBinding().getField().get(obj);
                copied.put(encodeTo.getToName(), val);

            }
            for (EncodeTo getter : getters) {

                Object val = getter.getBinding().getMethod().invoke(obj);
                copied.put(getter.getToName(), val);

            }
        } catch (JsonException e) {
            throw e;
        } catch (Exception e) {
            throw new JsonException(e);
        }
        return Any.wrap(copied);
    }

    private void encod(Object obj, JsonStream stream) throws IOException, IllegalAccessException, InvocationTargetException  {
        if (obj == null) {
            stream.writeNull();
            return;
        }
        stream.writeObjectStart();
        boolean notFirst = false;
        for (EncodeTo encodeTo : fields) {

            Object val = encodeTo.getBinding().getField().get(obj);

            notFirst = writeEncodeTo(stream, notFirst, encodeTo, val);
        }
        for (EncodeTo encodeTo : getters) {

            Object val = encodeTo.getBinding().getMethod().invoke(obj);

            notFirst = writeEncodeTo(stream, notFirst, encodeTo, val);
        }
        enc1(notFirst, stream, obj);

        if (notFirst) {
            stream.writeObjectEnd();
        } else {
            stream.write('}');
        }
    }
    
    private void enc1(boolean notFirst, JsonStream stream, Object obj ) throws IOException, IllegalAccessException, InvocationTargetException {
    	
    	  for (UnwrapperDescriptor unwrapper : desc.getUnwrappers()) {
              if (unwrapper.isMap()) {
                  
                  
                  enc2(notFirst, stream, unwrapper, obj);

                  
              } else {
                  if (notFirst) {
                      stream.writeMore();
                  } else {
                      notFirst = true;
                  }
                  unwrapper.getMethod().invoke(obj, stream);
              }
          }
    }
    
    private void enc2(boolean notFirst, JsonStream stream, UnwrapperDescriptor unwrapper, Object obj) throws IOException, IllegalAccessException, InvocationTargetException {
    	Map<Object, Object> map = (Map<Object, Object>) unwrapper.getMethod().invoke(obj);
    	for (Map.Entry<Object, Object> entry : map.entrySet()) {
            if (notFirst) {
                stream.writeMore();
            } else {
                notFirst = true;
            }
            stream.writeObjectField(entry.getKey().toString());
            stream.writeVal(unwrapper.getMapValueTypeLiteral(), entry.getValue());
        }
    }

    private boolean writeEncodeTo(JsonStream stream, boolean notFirst, EncodeTo encodeTo, Object val) throws IOException {

        OmitValue defaultValueToOmit = encodeTo.getBinding().getDefaultValueToOmit();

        if (!(defaultValueToOmit != null && defaultValueToOmit.shouldOmit(val))) {
            if (notFirst) {
                stream.writeMore();
            } else {
                stream.writeIndention();
                notFirst = true;
            }

            stream.writeObjectField(encodeTo.getToName());
            if (encodeTo.getBinding().getEncoder() != null) {
                encodeTo.getBinding().getEncoder().encode(val, stream);

            } else {
                stream.writeVal(val);
            }
        }
        return notFirst;
    }
}
