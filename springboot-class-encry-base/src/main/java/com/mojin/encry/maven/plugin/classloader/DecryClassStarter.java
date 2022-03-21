package com.mojin.encry.maven.plugin.classloader;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.net.URLStreamHandler;
import java.security.Key;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * 
 * @say little Boy, don't be sad.
 * @name Rezar
 * @time 2022-03-02 03:58:25
 * @Desc 些年若许,不负芳华.
 *
 */
public class DecryClassStarter extends ClassLoader {
	
	private char classNameReplacementChar;
	private Set<String> includePackages = new HashSet<String>();
	private Map<String, String> mapResources = new HashMap<>();
	
	private String decryPassword;
	
	private URLStreamHandler urlStreamHandler;

	public DecryClassStarter(
			ClassLoader parentClassLoader,
			String startLoaderIncludePackages) {
		super(parentClassLoader);
		this.checkJvmConfigParameters();
		if (startLoaderIncludePackages == null
				|| startLoaderIncludePackages.isEmpty()) {
			throw new IllegalArgumentException("未指定启动类包含的包");
		}
		for (String str : startLoaderIncludePackages.split(";")) {
			if (str.isEmpty()) {
				continue;
			}
			this.includePackages.add(str);
		}
		this.initDecryPassword();
		this.loadExternalData();
		this.configUrlStreamHandler();
	}
	
	private void checkJvmConfigParameters() {
		// -XX:+DisableAttachMechanism ,禁止内存嗅探
		boolean hasFind = false;
		List<String> inputArgs = 
				ManagementFactory.getRuntimeMXBean().getInputArguments();
		for (int i = 0 ;i < inputArgs.size() ;i ++) {
			if (inputArgs.get(i).equals("-XX:+DisableAttachMechanism")) {
				hasFind = true;
			}
		}
		if (!hasFind) {
			System.out.println(
					"\n#####################未设置的JVM参数[-XX:+DisableAttachMechanism],存在class被dump的风险#######################");
		}
	}

	private void configUrlStreamHandler() {
		StringBuilder newConfig = new StringBuilder();
		String property = 
				System.getProperty("java.protocol.handler.pkgs");
		if (property != null 
				&& !property.trim().isEmpty()) {
			newConfig.append(property).append("|com.mojin.encry.maven.plugin.classloader");
		} else {
			newConfig.append("com.mojin.encry.maven.plugin.classloader");
		}
		System.setProperty(
				"java.protocol.handler.pkgs", 
				newConfig.toString());
		this.urlStreamHandler = 
				new InnerURLStreamHandler(this);
	}

	protected void loadExternalData() {
		try (InputStream resourceAsStream = 
				this.getParent().getResourceAsStream("configDir/map.data")) {
			byte[] buff = 
					new byte[resourceAsStream.available()];
			readFully(resourceAsStream, buff);
			byte[] decrypt = 
					decrypt(decryPassword, buff);
			String mapData = new String(decrypt);
			for (String entry : mapData.split(";")) {
				if (entry.trim().isEmpty()) {
					continue;
				}
				String[] resourceKeyWithDataFileName = entry.split(":");
				mapResources.put(
						resourceKeyWithDataFileName[0], 
						resourceKeyWithDataFileName[1]);
			}
		} catch(IllegalStateException ex) {
			throw new IllegalStateException(
					"密码非法，无法启动",
					ex);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		};
	}
	
	private boolean checkResourceIsEncryFile(
			String resource,
			boolean isClass) {
		return this.mapResources.containsKey(
				isClass ? (resource.replace(".", "/") + ".class") : resource);
	}

	/**
	 * 存在文件夹目录监控扫描的可能 TODO
	 */
	protected void initDecryPassword() {
		File file = 
				new File("." + File.separator + "decry.psd");
		if (!file.exists()) {
			throw new IllegalStateException(
					"密码文件(decry.psd)不存在，无法启动");
		}
		try {
			this.decryPassword = 
					readFileToString(file).trim();
			if (decryPassword == null
					|| decryPassword.isEmpty()) {
				throw new IllegalStateException(
						"密码文件(decry.psd)内容为空，无法启动");
			}
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
	
	private String readFileToString(File file) throws IOException {
		try (FileInputStream fileInputStream = new FileInputStream(file)) {
			byte[] buff = new byte[fileInputStream.available()];
			readFully(new FileInputStream(file), buff);
			return new String(buff).trim();
		} catch (IOException e) {
			throw e;
		}
	}

	public static void invoke(
			String[] args, 
			String mainClass,
			String decryClass,
			String startLoaderIncludePackages,
			String autoDeletePasswordFile) {
		ClassLoader contextClassLoader = 
				Thread.currentThread().getContextClassLoader();
		try {
			Class<?> classLoaderClass = 
					contextClassLoader.loadClass(decryClass);
			ClassLoader decryClassLoader = 
					(ClassLoader)(classLoaderClass.getConstructor(
							ClassLoader.class,
							String.class)
					.newInstance(
							contextClassLoader, 
							startLoaderIncludePackages));
			Thread.currentThread().setContextClassLoader(decryClassLoader);
			Class<?> loadClass = 
					decryClassLoader.loadClass(mainClass);
			Object argObj = args;
			loadClass.getMethod(
					"startup",
					args.getClass())
			.invoke(null, argObj);
			if (Boolean.valueOf(autoDeletePasswordFile)) {
				File file = 
						new File("." + File.separator + "decry.psd");
				if (file.exists()) {
					file.delete();
				}
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
	
	
	@Override
	public synchronized Class<?> loadClass(
			String className, 
			boolean resolveIt) throws ClassNotFoundException {
		if (!checkInIncludePackages(className)) {
			return super.loadClass(
					className,
					resolveIt);
		}
		Class<?> clz = 
				findLoadedClass(className);
        if (clz != null) {
            return clz;
        }
    	byte[] classBytes = null;
    	if (this.checkResourceIsEncryFile(className, true)) {
    		classBytes = 
    				this.loadResourceByteDatas(
    						className,
    						true);
    	} else {
    		classBytes = 
    				this.loadClassBytesFromParent(
    						className);
    	}
    	if (classBytes != null) {
    		Class<?> defineClass = 
    				defineClass(
    						className, 
    						classBytes,
    						0,
    						classBytes.length );
    		if (resolveIt) {
    			this.resolveClass(defineClass);
    		}
    		if (defineClass.getPackage() == null) {
                int lastDotIndex = className.lastIndexOf( '.' );
                String packageName = 
                		(lastDotIndex >= 0) ? className.substring( 0, lastDotIndex) : "";
				definePackage(packageName, null, null, null, null, null, null, null);
            }
    		return defineClass;
    	}
    	return super.loadClass(className, resolveIt);
	}

	private boolean checkInIncludePackages(
			String className) {
		for (String include : this.includePackages) {
			if (className.startsWith(include)) {
				return true;
			}
		}
		return false;
	}

	private byte[] loadClassBytesFromParent(String className) {
		try {
			InputStream resourceAsStream = 
					super.getParent().getResourceAsStream(formatClassName(className));
			if (resourceAsStream == null) {
				return null;
			}
			byte[] buff = new byte[resourceAsStream.available()];
			readFully(resourceAsStream, buff);
			return buff;
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private byte[] loadResourceByteDatas(
			String resource, 
			boolean isLoadClassFile) {
		String decryResourceFileName = 
				isLoadClassFile ? this.mapResources.get(
						resource.replace(".", "/") + ".class")
						: resource;
		try {
			URL url =
					this.getParent().getResource(
							"configDir/" + decryResourceFileName);
			InputStream stream = url.openStream();
			byte[] buff = new byte[stream.available()];
			readFully(url.openStream(), buff);
			return decrypt(decryPassword, buff);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
	
	protected String formatClassName(String className) {
        className = className.replace( '/', '~' );
        if (classNameReplacementChar == '\u0000') {
            // '/' is used to map the package to the path
            className = className.replace( '.', '/' ) + ".class";
        } else {
            // Replace '.' with custom char, such as '_'
            className = className.replace( '.', classNameReplacementChar ) + ".class";
        }
        className = className.replace( '~', '/' );
        return className;
    }
	


	private static final String UTF_8 = "utf-8";

	private static final String KEY_ALGORITHM = "AES";

	private static final String DEFAULT_CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding";


	public byte[] cipher(String password, byte[] cipherDatas) {
		// AES加密
		Cipher cipher;
		try {
			cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);
			Key key = 
					new SecretKeySpec(
							password.getBytes(UTF_8),
							KEY_ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, key);
			byte[] aesData = cipher.doFinal(cipherDatas);
			return aesData;
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	public byte[] decrypt(String password, byte[] datas) {
		try {
			Cipher cipher = 
					Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);
			Key key = new SecretKeySpec(password.getBytes(UTF_8), KEY_ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, key);
			return cipher.doFinal(datas);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
	
	private static final int EOF = -1;
	
	public int readFully(
			InputStream input, 
			byte[] buffer) throws IOException {
		int offset = 0;
		int length = input.available();
        if (length < 0) {
            throw new IllegalArgumentException(
            		"Length must not be negative: " + length);
        }
        int remaining = length;
        while (remaining > 0) {
            int location = length - remaining;
            int count = input.read(buffer, offset + location, remaining);
            if (EOF == count) { // EOF
                break;
            }
            remaining -= count;
        }
        return length - remaining;
    }
	
	@Override
	public URL getResource(String resource) {
		if (this.checkResourceIsEncryFile(resource, false)) {
			try {
				return new URL(
						"inner",
						null,
						-1,
						this.mapResources.get(resource),
						this.urlStreamHandler);
			} catch (Exception e) {
	            throw new IllegalArgumentException(
	            		"getResource error: " + resource,
	            		e);
			}
		}
		return super.getResource(resource);
	}
	
	

	@Override
	public InputStream getResourceAsStream(String resource) {
		InputStream resourceAsStream = 
				super.getResourceAsStream(resource);
		return resourceAsStream;
	}

	@Override
	public Enumeration<URL> getResources(
			String resource) throws IOException {
		URL tmpResourceURL = null;
		URL replaceWithURL = null;
		if (this.checkResourceIsEncryFile(resource, false)) {
			tmpResourceURL = 
					super.getParent().getResource(resource);
			replaceWithURL = 
					this.getResource(resource);
		}
		Enumeration<URL> resources = 
				super.getResources(resource);
		Set<URL> sets = new HashSet<>();
		while (resources.hasMoreElements()) {
			URL nextElement = 
					resources.nextElement();
			// 进行替换
			if (tmpResourceURL != null
					&& nextElement.equals(tmpResourceURL)) {
				sets.add(replaceWithURL);
			} else {
				sets.add(nextElement);
			}
		}
		return Collections.enumeration(sets);
	}
	
	public InputStream loadOriginResource(
			String encryFileName) {
		ByteArrayInputStream inputStream = 
				new ByteArrayInputStream(
						this.loadResourceByteDatas(
								encryFileName,
								false));
		return inputStream;
	}
	
}

