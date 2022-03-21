package com.mojin.encry.maven.plugin;

import java.io.File;

/**
 * 
 * @say little Boy, don't be sad.
 * @name Rezar
 * @time 2022-03-07 08:04:45
 * @Desc 些年若许,不负芳华.
 *
 */
public class EncryItem {
	
	private String packageName;
	private String javaFile;
	private String normalFile;
	
	public File parseToFilePath(
			File baseDir) {
		if (this.packageName != null 
				&& !this.packageName.trim().isEmpty()) {
			return parseToPackageDir(baseDir);
		} else if (this.javaFile != null 
				&& !this.javaFile.trim().isEmpty()) {
			return paseToJavaFile(baseDir);
		} else if (this.normalFile != null 
				&& !this.normalFile.trim().isEmpty()) {
			return parseToNormalFile(baseDir);
		}
		throw new IllegalArgumentException("EncryItem必须配置属性");
	}
	
	private File parseToNormalFile(File baseDir) {
		return new File(
				baseDir,
				this.normalFile.trim().replace("/", File.separator));
	}

	private File paseToJavaFile(File baseDir) {
		return new File(
				baseDir,
				this.javaFile.trim().replace(".", File.separator) + ".class");
	}

	private File parseToPackageDir(
			File baseDir) {
		return new File(
				baseDir,
				this.packageName.trim().replace(".", File.separator));
	}

	public String getPackageName() {
		return packageName;
	}
	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}
	public String getJavaFile() {
		return javaFile;
	}
	public String getNormalFile() {
		return normalFile;
	}
	public void setJavaFile(String javaFile) {
		this.javaFile = javaFile;
	}
	public void setNormalFile(String normalFile) {
		this.normalFile = normalFile;
	}
	@Override
	public String toString() {
		return "EncryItem [packageName=" + packageName + ", javaFile=" + javaFile + ", normalFile=" + normalFile + "]";
	}
	
}

