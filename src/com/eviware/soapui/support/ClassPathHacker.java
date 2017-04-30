package com.eviware.soapui.support;

/*
 *  soapUI, copyright (C) 2004-2007 eviware.com
 *
 *  soapUI is free software; you can redistribute it and/or modify it under the
 *  terms of version 2.1 of the GNU Lesser General Public License as published by
 *  the Free Software Foundation.
 *
 *  soapUI is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Lesser General Public License for more details at gnu.org.
 */

// Source: http://www.java2s.com/Open-Source/Java-Document/Web-Services/soapui-1.7.5/com/eviware/soapui/support/ClasspathHacker.java.htm

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;


public class ClassPathHacker {

    private static final Class[] parameters = new Class[] { URL.class };

    public static void addFile(String s) throws IOException {
        File f = new File(s);
        addFile(f);
    }//end method

    public static void addFile(File f) throws IOException {
        addURL(f.toURL());
    }//end method

    public static void addURL(URL u) throws IOException {

        try {
            ClassLoader classLoader = (URLClassLoader)ClassLoader.getSystemClassLoader(); //SoapUI.class.getClassLoader();
            if (!(classLoader instanceof  URLClassLoader)) {
                //SoapUI.log
                //        .error("SoapUI classloader is not an URLClassLoader, failed to add external library");
                return;
            }

            URLClassLoader sysloader = (URLClassLoader) classLoader;
            Class sysclass = URLClassLoader.class;
            Method method = sysclass.getDeclaredMethod("addURL",
                    parameters);
            method.setAccessible(true);
            method.invoke(sysloader, new Object[] { u });

            //SoapUI.log
            //        .info("Added [" + u.toString() + "] to classpath");

        } catch (Throwable t) {
            //SoapUI.logError(t);
            throw new IOException(
                    "Error, could not add URL to system classloader");
        }//end try catch

    }//end method

}//end class

// old code

/*import java.lang.reflect.*;
import java.io.*;
import java.net.*;

public class ClassPathHacker {
	
	private static final Class[] parameters = new Class[]{URL.class};
	
	public static void addFile(String s) throws IOException {
		File f = new File(s);
		addFile(f);
	}//end method
	
	public static void addFile(File f) throws IOException {
		addURL(f.toURL());
	}//end method
	
	
	public static void addURL(URL u) throws IOException {
		
		URLClassLoader sysloader = (URLClassLoader)ClassLoader.getSystemClassLoader();
		Class sysclass = URLClassLoader.class;
		
		try {
			Method method = sysclass.getDeclaredMethod("addURL",parameters);
			method.setAccessible(true);
			method.invoke(sysloader,new Object[]{ u });
		} catch (Throwable t) {
			t.printStackTrace();
			throw new IOException("Error, could not add URL to system classloader");
		}//end try catch
		
	}//end method
	
}//end class
 */
