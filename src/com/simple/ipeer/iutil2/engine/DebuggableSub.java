package com.simple.ipeer.iutil2.engine;

/**
 *
 * @author iPeer
 */
public interface DebuggableSub {

    public Throwable getLastExeption();
    public long getLastExceptionTime();
    public long getLastUpdateTime();
    
}
