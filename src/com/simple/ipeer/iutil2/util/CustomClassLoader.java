package com.simple.ipeer.iutil2.util;

import java.io.FileInputStream;

/**
 *
 * @author iPeer
 */
public class CustomClassLoader extends ClassLoader {

    final String basePath = "./addons/";

    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        String fullName = name.replace('.', '/');
        fullName += ".class";

        String path = basePath + fullName ;
        try {
            FileInputStream fis = new FileInputStream(path);
            byte[] data = new byte[fis.available()];
            fis.read(data);
            Class<?> res = defineClass(name, data, 0, data.length);
            fis.close();

            return res;
        } catch(Exception e) {
            return super.findClass(name);
        }
    }
}
