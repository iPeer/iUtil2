package com.simple.ipeer.iutil2.engine;

/**
 *
 * @author iPeer
 */
public enum LogLevel {
    NONE(0), NEVER(0), LOG_ONLY(1), LOG_AND_DEBUG(2), DEBUG_ONLY(3), ALL(4);
    
    private final int level;
    
    private LogLevel(int level) { this.level = level; }
    
    @Override
    public String toString() {
	return Integer.toString(level);
    }
}
