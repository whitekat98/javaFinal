package com.jsoniter;

import com.jsoniter.any.Any;
import com.jsoniter.spi.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

class ReflectionObjectDecoder {

	private static final String CON = "missing required properties: ";
	
    private static Object notset = new Object() {
        @Override
        public String toString() {
            return "NOT_SET";
        }
    };
    private Map<Slice, Binding> allBindings = new HashMap<Slice, Binding>();
    private String tempCacheKey;
    private String ctorArgsCacheKey;
    private int tempCount;
    private long expectedTracker;
    private int requiredIdx;
    private int tempIdx;
    private ClassDescriptor desc;

    public ReflectionObjectDecoder(ClassInfo classInfo) {
        try {
            init(classInfo);
        } catch (JsonException e) {
            throw e;
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    private final void init(ClassInfo classInfo) {
        Class clazz = classInfo.clazz;
        desc = ClassDescriptor.getDecodingClassDescriptor(classInfo, true);

        for (Binding param : desc.getCtor().getParameters()) {

            addBinding(classInfo, param);
        }
        this.desc = desc;
        if (desc.getCtor().getObjectFactory() == null && desc.getCtor().getCtor() == null && desc.getCtor().getStaticFactory() == null) {
            throw new JsonException("no constructor for: " + desc.getClazz());
        }
        for (Binding field : desc.getFields()) {
            addBinding(classInfo, field);
        }
        for (Binding setter : desc.getSetters()) {
            addBinding(classInfo, setter);
        }
        for (WrapperDescriptor setter : desc.getBindingTypeWrappers()) {
            for (Binding param : setter.getparameters()) {
                addBinding(classInfo, param);
            }
        }
        if (requiredIdx > 63) {
            throw new JsonException("too many required properties to track");
        }
        expectedTracker = Long.MAX_VALUE >> (63 - requiredIdx);

        if (!desc.getCtor().getParameters().isEmpty() || !desc.getBindingTypeWrappers().isEmpty()) {

            tempCount = tempIdx;
            tempCacheKey = "temp@" + clazz.getCanonicalName();
            ctorArgsCacheKey = "ctor@" + clazz.getCanonicalName();
        }
    }

    private void addBinding(ClassInfo classInfo, final Binding binding) {
        if (binding.getFromNames().length == 0) {
            return;
        }
        if (binding.isAsMissingWhenNotPresent()) {
            binding.setMask(1L << requiredIdx);
            requiredIdx++;
        }
        if (binding.isAsExtraWhenPresent()) {
            binding.setDecoder(new Decoder() {
                @Override
                public Object decode(JsonIterator iter) throws IOException {
                    throw new JsonException("found should not present property: " + binding.getName());
                }
            });
        }
        if (binding.getDecoder() == null) {
            // field decoder might be special customized
            binding.setDecoder(JsoniterSpi.getDecoder(binding.decoderCacheKey()));
        }
        if (binding.getDecoder() == null) {
            binding.setDecoder(Codegen.getDecoder(binding.getValueTypeLiteral().getDecoderCacheKey(), binding.getValueType()));
        }
        binding.setIdx(tempIdx);
        for (String fromName : binding.getFromNames()) {
            Slice slice = Slice.make(fromName);
            if (allBindings.containsKey(slice)) {
                throw new JsonException("name conflict found in " + classInfo.clazz + ": " + fromName);
            }
            allBindings.put(slice, binding);
        }
        tempIdx++;
    }

    public Decoder create() {

        if (desc.getCtor().getParameters().isEmpty()) {
            if (desc.getBindingTypeWrappers().isEmpty()) {

                return new OnlyField();
            } else {
                return new WithWrapper();
            }
        } else {
            return new WithCtor();
        }
    }

    public class OnlyField implements Decoder {

        public Object decode(JsonIterator iter) throws IOException {
            try {
                return decod(iter);
            }catch (Exception e) {
                throw new JsonException(e);
            } 
        }

        private Object decod(JsonIterator iter) throws IllegalAccessException, InvocationTargetException, IOException, InstantiationException  {
            if (iter.readNull()) {
                CodegenAccess.resetExistingObject(iter);
                return null;
            }
            Object obj = CodegenAccess.existingObject(iter) == null ? createNewObject() : CodegenAccess.resetExistingObject(iter);
            dec(obj, requiredIdx, desc, iter);
            
            Map<String, Object> extra = null;
            long tracker = 0L;
            Slice fieldName = CodegenAccess.readObjectFieldAsSlice(iter);
            Binding binding = allBindings.get(fieldName);
            if (binding == null) {
                extra = onUnknownProperty(iter, fieldName, extra);
            } else {
                if (binding.isAsMissingWhenNotPresent()) {
                    tracker |= binding.getMask();
                }
                setToBinding(obj, binding, decodeBinding(iter, obj, binding));
            }
            
            deco(fieldName, binding, iter, extra, tracker, obj);
            
            if (tracker != expectedTracker) {
                if (desc.getOnMissingProperties() == null) {
                    throw new JsonException(CON + collectMissingFields(tracker));
                } else {
                    setToBinding(obj, desc.getOnMissingProperties(), collectMissingFields(tracker));
                }
            }
            setExtra(obj, extra);
            return obj;
        }
        
        private void deco(Slice fieldName, Binding binding, JsonIterator iter, Map<String, Object> extra, long tracker, Object obj ) throws IllegalAccessException, InvocationTargetException, IOException {
        	
        	while (CodegenAccess.nextToken(iter) == ',') {

                if (binding == null) {
                    extra = onUnknownProperty(iter, fieldName, extra);
                } else {
                    if (binding.isAsMissingWhenNotPresent()) {
                        tracker |= binding.getMask();
                    }
                    setToBinding(obj, binding, decodeBinding(iter, obj, binding));
                }
            }
        }
        
        private Object dec(Object obj, int requiredIdx, ClassDescriptor desc ,JsonIterator iter) throws IllegalAccessException, InvocationTargetException  {
        	
        	if (!CodegenAccess.readObjectStart(iter)) {
                if (requiredIdx > 0) {
                    if (desc.getOnMissingProperties() == null) {
                        throw new JsonException(CON + collectMissingFields(0));
                    } else {
                        setToBinding(obj, desc.getOnMissingProperties(), collectMissingFields(0));
                    }
                }
                return obj;
            }
			return iter;
        }
    }

    public class WithCtor implements Decoder {

        @Override
        public Object decode(JsonIterator iter) throws IOException {
            try {
                return decod(iter);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new JsonException(e);
            }
        }

        private Object decod(JsonIterator iter) throws IOException, IllegalAccessException, InvocationTargetException  {
            if (iter.readNull()) {
                CodegenAccess.resetExistingObject(iter);
                return null;
            }
            if (iter.tempObjects == null) {
                iter.tempObjects = new HashMap<String, Object>();
            }
            Object[] temp = (Object[]) iter.tempObjects.get(tempCacheKey);
            
            deco2(requiredIdx, iter, temp);
            
            Map<String, Object> extra = null;
            long tracker = 0L;
            Slice fieldName = CodegenAccess.readObjectFieldAsSlice(iter);
            Binding binding = allBindings.get(fieldName);
            if (binding == null) {
                extra = onUnknownProperty(iter, fieldName, extra);
            } else {
                if (binding.isAsMissingWhenNotPresent()) {
                    tracker |= binding.getMask();
                }
                temp[binding.getIdx()] = decodeBinding(iter, binding);
            }
            
            dec2(fieldName, binding, iter, extra, tracker, temp);
            
            if (tracker != expectedTracker) {
                throw new JsonException(CON + collectMissingFields(tracker));
            }
            Object obj = createNewObject(iter, temp);
            setExtra(obj, extra);
            for (Binding field : desc.getFields()) {
                Object val = temp[field.getIdx()];
                if (val != notset && field.getFromNames().length > 0) {
                    field.getField().set(obj, val);
                }
            }
            for (Binding setter : desc.getSetters()) {
                Object val = temp[setter.getIdx()];
                if (val != notset) {
                    setter.getMethod().invoke(obj, val);
                }
            }
            applyWrappers(temp, obj);
            return obj;
        } 
         
        private Object deco2( int requiredIdx, JsonIterator iter, Object[] temp)  {
        	
        	if (temp == null) {
                temp = new Object[tempCount];
                iter.tempObjects.put(tempCacheKey, temp);
            }
            Arrays.fill(temp, notset);
            if (!CodegenAccess.readObjectStart(iter)) {
                if (requiredIdx > 0) {
                    throw new JsonException(CON + collectMissingFields(0));
                }
                return createNewObject(iter, temp);
            }
			return temp;
        }
        
        private void dec2(Slice fieldName, Binding binding, JsonIterator iter, Map<String, Object> extra, long tracker, Object[] temp) throws IOException  {
        	
        	while (CodegenAccess.nextToken(iter) == ',') {
                
                if (binding == null) {
                    extra = onUnknownProperty(iter, fieldName, extra);
                } else {
                    if (binding.isAsMissingWhenNotPresent()) {
                        tracker |= binding.getMask();
                    }
                    temp[binding.getIdx()] = decodeBinding(iter, binding);
                }
            }
        }
        
        private Object createNewObject(JsonIterator iter, Object[] temp) {
            if (iter.tempObjects == null) {
                iter.tempObjects = new HashMap<String, Object>();
            }
            Object[] ctorArgs = (Object[]) iter.tempObjects.get(ctorArgsCacheKey);
            if (ctorArgs == null) {

                ctorArgs = new Object[desc.getCtor().getParameters().size()];

                iter.tempObjects.put(ctorArgsCacheKey, ctorArgs);
            }
            Arrays.fill(ctorArgs, null);

            for (int i = 0; i < desc.getCtor().getParameters().size(); i++) {
                Object arg = temp[desc.getCtor().getParameters().get(i).getIdx()];

                if (arg != notset) {
                    ctorArgs[i] = arg;
                }
            }
            return ctorArgs;
        }
        
    }

    public class WithWrapper implements Decoder {

        @Override
        public Object decode(JsonIterator iter) throws IOException {
            try {
                return decod(iter);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e1) {
                throw new JsonException(e1);
            }
        }

        private Object decod(JsonIterator iter) throws IllegalAccessException, InvocationTargetException, InstantiationException, IOException  {
            if (iter.readNull()) {
                CodegenAccess.resetExistingObject(iter);
                return null;
            }
            Object obj = createNewObject();
            
            if (!CodegenAccess.readObjectStart(iter)) {
                deco3(obj, requiredIdx, desc, iter);
                return obj;
            }
            Map<String, Object> extra = null;
            long tracker = 0L;
            if (iter.tempObjects == null) {
                iter.tempObjects = new HashMap<String, Object>();
            }
            Object[] temp = (Object[]) iter.tempObjects.get(tempCacheKey);
            if (temp == null) {
                temp = new Object[tempCount];
                iter.tempObjects.put(tempCacheKey, temp);
            }
            Arrays.fill(temp, notset);
            Slice fieldName = CodegenAccess.readObjectFieldAsSlice(iter);
            Binding binding = allBindings.get(fieldName);
            if (binding == null) {
                extra = onUnknownProperty(iter, fieldName, extra);
            } else {
                if (binding.isAsMissingWhenNotPresent()) {
                    tracker |= binding.getMask();
                }
                if (canNotSetDirectly(binding)) {
                    temp[binding.getIdx()] = decodeBinding(iter, obj, binding);
                } else {
                    setToBinding(obj, binding, decodeBinding(iter, obj, binding));
                }
            }
            
            dec3(fieldName, binding, iter, extra, tracker, temp, obj);
            
            
            if (tracker != expectedTracker) {
                if (desc.getOnMissingProperties() == null) {
                    throw new JsonException(CON + collectMissingFields(tracker));
                } else {
                    setToBinding(obj, desc.getOnMissingProperties(), collectMissingFields(tracker));
                }
            }
            setExtra(obj, extra);
            applyWrappers(temp, obj);
            return obj;
        }
        
        private Object deco3(Object obj, int requiredIdx, ClassDescriptor desc ,JsonIterator iter) throws IllegalAccessException, InvocationTargetException  {
        	
        	if (requiredIdx > 0) {
                if (desc.getOnMissingProperties() == null) {
                    throw new JsonException(CON + collectMissingFields(0));
                } else {
                    setToBinding(obj, desc.getOnMissingProperties(), collectMissingFields(0));
                }
            }
			return iter;
        }
         
        private void dec3(Slice fieldName, Binding binding, JsonIterator iter, Map<String, Object> extra, long tracker, Object[] temp, Object obj) throws IllegalAccessException, IOException, InvocationTargetException  {
        	
        	while (CodegenAccess.nextToken(iter) == ',') {
                
                if (binding == null) {
                    extra = onUnknownProperty(iter, fieldName, extra);
                } else {
                    if (binding.isAsMissingWhenNotPresent()) {
                        tracker |= binding.getMask();
                    }
                    if (canNotSetDirectly(binding)) {
                        temp[binding.getIdx()] = decodeBinding(iter, obj, binding);
                    } else {
                        setToBinding(obj, binding, decodeBinding(iter, obj, binding));
                    }
                }
            }
        }
        
        private boolean canNotSetDirectly(Binding binding) {
            return binding.getField() == null && binding.getMethod() == null;
        }
    }

    private void setToBinding(Object obj, Binding binding, Object value) throws IllegalAccessException, InvocationTargetException  {
        if (binding.getField() != null) {
            binding.getField().set(obj, value);
        } else {
            binding.getMethod().invoke(obj, value);
        }
    }

    private void setExtra(Object obj, Map<String, Object> extra) throws IllegalAccessException, InvocationTargetException  {
        if (extra == null) {
            return;
        }
        if (desc.isAsExtraForUnknownProperties()) {
            if (desc.getOnExtraProperties() == null) {
                    throw new JsonException("unknown property: ");
                
            } else {
                setToBinding(obj, desc.getOnExtraProperties(), extra);
            }
        }
        for (Method wrapper : desc.getKeyValueTypeWrappers()) {
            for (Map.Entry<String, Object> entry : extra.entrySet()) {
                Any value = (Any) entry.getValue();
                wrapper.invoke(obj, entry.getKey(), value.object());
            }
        }
    }

    

    private Object decodeBinding(JsonIterator iter, Binding binding) throws IOException {
        Object value;
        value = binding.getDecoder().decode(iter);
        return value;
    }

    private Object decodeBinding(JsonIterator iter, Object obj, Binding binding) throws IOException, IllegalAccessException {
        if (binding.isValueCanReuse()) {
            CodegenAccess.setExistingObject(iter, binding.getField().get(obj));
        }
        return decodeBinding(iter, binding);
    }

    private Map<String, Object> onUnknownProperty(JsonIterator iter, Slice fieldName, Map<String, Object> extra) {
        boolean shouldReadValue = desc.isAsExtraForUnknownProperties() || !desc.getKeyValueTypeWrappers().isEmpty();
        if (shouldReadValue) {
            Any value = iter.readAny();
            if (extra == null) {
                extra = new HashMap<String, Object>();
            }
            extra.put(fieldName.toString(), value);
        } else {
            iter.skip();
        }
        return extra;
    }

    private List<String> collectMissingFields(long tracker) {
        List<String> missingFields = new ArrayList<String>();
        for (Binding binding : allBindings.values()) {
            if (binding.isAsMissingWhenNotPresent()) {
                long mask = binding.getMask();
                CodegenAccess.addMissingField(missingFields, tracker, mask, binding.getName());
            }
        }
        return missingFields;
    }

    private void applyWrappers(Object[] temp, Object obj) throws IllegalAccessException, InvocationTargetException {
        for (WrapperDescriptor wrapper : desc.getBindingTypeWrappers()) {
            Object[] args = new Object[wrapper.getparameters().size()];
            for (int i = 0; i < wrapper.getparameters().size(); i++) {
                Object arg = temp[wrapper.getparameters().get(i).getIdx()];
                if (arg != notset) {
                    args[i] = arg;
                }
            }
            wrapper.getMethod().invoke(obj, args);
        }
    }

    

    private Object createNewObject(Object... args) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        if (desc.getCtor().getObjectFactory() != null) {
            return desc.getCtor().getObjectFactory().create(desc.getClazz());
        }
        if (desc.getCtor().getStaticFactory() != null) {
            return desc.getCtor().getStaticFactory().invoke(null, args);
        } else {
            return desc.getCtor().getCtor().newInstance(args);
        }
    }
   
    
    
}
