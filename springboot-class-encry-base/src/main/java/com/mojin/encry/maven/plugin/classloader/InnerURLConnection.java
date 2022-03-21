package com.mojin.encry.maven.plugin.classloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 
 * @say little Boy, don't be sad.
 * @name Rezar
 * @time 2022-03-07 03:26:15
 * @Desc 些年若许,不负芳华.
 *
 */
public class InnerURLConnection extends HttpURLConnection {

    private InputStream inputStream;
    private DecryClassStarter classLoader;

    InnerURLConnection(URL url) throws IOException {
        super(url);
    }

    public InnerURLConnection(
    		URL u,
    		DecryClassStarter classLoader) {
    	super(u);
    	this.classLoader = classLoader;
	}

	@Override
    public void disconnect() {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
            	throw new IllegalStateException(e);
            }
        }
        connected = false;
    }

    @Override
    public boolean usingProxy() {
        return false;
    }

    @Override
    public void connect() throws IOException {
        if (connected) {
            return;
        }
        inputStream = 
        		loadData(
        				InnerURLConnection.class.getClassLoader());
        connected = true;
    }

    @Override
    public int getResponseCode() throws IOException {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public InputStream getInputStream() throws IOException {
    	if (!this.connected) {
    		this.connect();
    	}
        return inputStream;
    }

    private InputStream loadData(ClassLoader loader) {
        String file = url.getFile();
        return this.classLoader.loadOriginResource(file);
    }
}

