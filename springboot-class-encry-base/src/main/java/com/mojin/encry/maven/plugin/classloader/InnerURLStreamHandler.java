package com.mojin.encry.maven.plugin.classloader;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;


/**
 * 
 * @say little Boy, don't be sad.
 * @name Rezar
 * @time 2022-03-07 03:25:42
 * @Desc 些年若许,不负芳华.
 *
 */
public class InnerURLStreamHandler extends URLStreamHandler {

	private DecryClassStarter classLoader;
	
    public InnerURLStreamHandler(
    		DecryClassStarter classLoader) {
    	this.classLoader = classLoader;
	}

	@Override
    protected URLConnection openConnection(URL u) throws IOException {
        return new InnerURLConnection(u, this.classLoader);
    }
    
}

