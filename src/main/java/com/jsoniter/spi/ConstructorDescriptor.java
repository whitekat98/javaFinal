package com.jsoniter.spi;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ConstructorDescriptor {
    /**
     * set to null if use constructor
     * otherwise use static method
     */
    private String staticMethodName;
    // option 1: use constructor
    private Constructor ctor;
    // option 2: use static method
    private Method staticFactory;
    // option 3: create by extension
    private Extension objectFactory;

    /**
     * the parameters to call constructor or static method
     */
    private List<Binding> parameters = new ArrayList<Binding>();

    @Override
    public String toString() {
        return "ConstructorDescriptor{" +
                "staticMethodName='" + getStaticMethodName() + '\'' +
                ", ctor=" + getCtor() +
                ", staticFactory=" + getStaticFactory() +
                ", parameters=" + getParameters() +
                '}';
    }

	public String getStaticMethodName() {
		return staticMethodName;
	}

	public void setStaticMethodName(String staticMethodName) {
		this.staticMethodName = staticMethodName;
	}

	public Method getStaticFactory() {
		return staticFactory;
	}

	public void setStaticFactory(Method staticFactory) {
		this.staticFactory = staticFactory;
	}

	public Constructor getCtor() {
		return ctor;
	}

	public void setCtor(Constructor ctor) {
		this.ctor = ctor;
	}

	public Extension getObjectFactory() {
		return objectFactory;
	}

	public void setObjectFactory(Extension objectFactory) {
		this.objectFactory = objectFactory;
	}

	public List<Binding> getParameters() {
		return parameters;
	}

	public void setParameters(List<Binding> parameters) {
		this.parameters = parameters;
	}
}
