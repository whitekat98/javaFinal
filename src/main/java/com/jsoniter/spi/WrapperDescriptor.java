package com.jsoniter.spi;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class WrapperDescriptor {

    /**
     * the parameters to bind
     */
    private List<Binding> parameters = new ArrayList<Binding>();

    public List<Binding> getparameters() {
      return parameters;
    }

    public void setparameters(Binding parameters) {
      this.parameters.add(parameters);
    }

    public Method getMethod() {
		return method;
	}

	public void setMethod(Method method) {
		this.method = method;
	}

	private Method method;
}
