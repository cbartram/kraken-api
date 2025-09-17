package com.kraken.api.core.loader;

import lombok.Getter;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;

/**
 * Custom ClassLoader that loads classes from byte arrays stored in memory
 */
@Getter
public class ByteArrayClassLoader extends URLClassLoader {
    private final Map<String, byte[]> classData;

    public ByteArrayClassLoader(Map<String, byte[]> classData) {
        super(new URL[0], ByteArrayClassLoader.class.getClassLoader());
        this.classData = classData;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] bytes = classData.get(name);
        if (bytes != null) {
            return defineClass(name, bytes, 0, bytes.length);
        }
        return super.findClass(name);
    }
}

