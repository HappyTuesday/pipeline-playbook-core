package com.yit.deploy.core.utils;

import hudson.FilePath;
import org.codehaus.groovy.runtime.IOGroovyMethods;
import org.codehaus.groovy.runtime.ResourceGroovyMethods;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Properties;

public class IO {
    public static byte[] readBytes(FilePath path) {
        try {
            return IOGroovyMethods.getBytes(path.read());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeBytes(FilePath path, byte[] bytes) {
        try {
            IOGroovyMethods.setBytes(path.write(), bytes);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static String readToString(FilePath path) {
        try {
            return path.readToString();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeStringTo(FilePath path, String text) {
        try {
            path.write(text, Utils.DefaultCharset.name());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static String readToString(File file) {
        try {
            return ResourceGroovyMethods.getText(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getText(InputStream is) {
        try {
            return IOGroovyMethods.getText(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Properties loadProperties(String text) {
        Properties ps = new Properties();
        try (StringReader sr = new StringReader(text)) {
            try {
                ps.load(sr);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return ps;
    }
}
