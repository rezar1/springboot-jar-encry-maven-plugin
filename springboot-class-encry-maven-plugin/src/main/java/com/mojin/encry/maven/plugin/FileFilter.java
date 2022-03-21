package com.mojin.encry.maven.plugin;

import java.io.File;
import java.io.FilenameFilter;

public class FileFilter implements FilenameFilter {
	
    String extension;
    
    public FileFilter(String extension) {
        this.extension = extension;
    }

    @Override
    public boolean accept(File dir, String name) {
    	if (extension != null
    			|| extension.trim().isEmpty()) {
    		return name.endsWith(extension);
    	}
    	return true;
    }
    
}
