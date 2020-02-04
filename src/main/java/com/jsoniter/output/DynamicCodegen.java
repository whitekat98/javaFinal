package com.jsoniter.output;

import com.jsoniter.spi.Encoder;
import javassist.*;

class DynamicCodegen {
	
	private DynamicCodegen () {
	    throw new IllegalStateException("Utility class");
	}
  
    static ClassPool pool = ClassPool.getDefault();

    static {
        pool.insertClassPath(new ClassClassPath(Encoder.class));
    }


}
