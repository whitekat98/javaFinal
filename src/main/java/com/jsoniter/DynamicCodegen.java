package com.jsoniter;

import com.jsoniter.spi.Decoder;
import javassist.*;

class DynamicCodegen {
	
	private DynamicCodegen() {
	    throw new IllegalStateException("Utility class");
	}

    static ClassPool pool = ClassPool.getDefault();

    static {
        pool.insertClassPath(new ClassClassPath(Decoder.class));
    }

  

}
