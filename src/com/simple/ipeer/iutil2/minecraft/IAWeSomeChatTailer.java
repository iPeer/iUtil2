package com.simple.ipeer.iutil2.minecraft;

import java.io.File;

/**
 *
 * @author iPeer
 */
public interface IAWeSomeChatTailer {
    
    public File getFile();
    public File getInputFile();
    public File getOutput();
    public File getOutputFile();
    public String getName();
    public void setServerName(String name);
    public void start();
    public void startIfNotRunning();
    public void stop();
    
}
