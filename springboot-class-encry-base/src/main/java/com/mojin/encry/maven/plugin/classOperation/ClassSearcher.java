package com.mojin.encry.maven.plugin.classOperation;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;

/**
 * 
 * @say little Boy, don't be sad.
 * @name Rezar
 * @time 2022-03-04 03:57:20
 * @Desc 些年若许,不负芳华.
 *
 */
public class ClassSearcher {

	private static final FileFilter DEFAULT_FILE_NAME_FILTER = 
			new FileFilter() {
		@Override
		public boolean accept(File pathname) {
			return pathname.getName().endsWith(".class");
		}
	};
	
	private static final FileFilter DEFAULT_FILE_FILTER = 
			new FileFilter() {
		@Override
		public boolean accept(File pathname) {
			return pathname.isDirectory();
		}
	};

	public static File searchMainClass(
			String basicDir) {
		return loopAndSearchMainClassFile(
				new File(basicDir));
	}
	
	public static File loopAndSearchMainClassFile(
			File rootDir) {
		for (File file : rootDir.listFiles(DEFAULT_FILE_NAME_FILTER)) {
			if (!file.isDirectory()
					&& isSpringBootMainClass(file)) {
				return file;
			}
		}
		for (File fileItem : rootDir.listFiles(DEFAULT_FILE_FILTER)) {
			File springbootMainClassFile = 
					loopAndSearchMainClassFile(fileItem);
			if (springbootMainClassFile != null) {
				return springbootMainClassFile;
			}
		}
		return null;
	}

	private static boolean isSpringBootMainClass(
			File file) {
		try (FileInputStream fileInputStream = new FileInputStream(file)) {
			return MainClassFinder.isSpringMainClass(fileInputStream);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
}

