/*
 * Copyright 2012, by Yet another Protobuf Maven Plugin Developers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mojin.encry.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.Key;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import com.mojin.encry.maven.plugin.classOperation.ClassBuilder;
import com.mojin.encry.maven.plugin.classOperation.ClassSearcher;
import com.mojin.encry.maven.plugin.classOperation.MainClassRewriter;
import com.mojin.encry.maven.plugin.util.RandomPasswordUtils;

/**
 * @goal encry
 * @phase test
 * @requiresDependencyResolution
 */
public class EncryMojo extends AbstractMojo {

	private static final String DEFAULT_PASSWORD_CHARES = "abcdefghijklmnopqrstuvwxyz0123456789#@!%$^&*()_+|'/.,l";

	/**
	 * The Maven project.
	 *
	 * @parameter expression="${project}"
	 * @required
	 * @readonly
	 */
	private MavenProject project;
	
	/**
	 * springBoot???????????????????????????????????????????????????main????????????
	 * 
	 * @parameter expression="${mainClass}"
	 */
	private String mainClass;

	/**
	 * ???????????????????????????????????????(????????????????????????????????????)
	 * 
	 * @parameter expression="${customeClassloaderScanPackages}"
	 */
	private String[] customeClassloaderScanPackages;
	
	/**
	 * ??????????????????
	 * 
	 * @parameter expression="${encryItemes}"
	 */
	private EncryItem[] encryItemes;
	
	/**
	 * ?????????????????????????????????
	 * 
	 * @parameter expression="${excludeEncryPackagesOrClasses}"
	 */
	private String[] excludeEncryPackagesOrClasses;
	
	/**
	 * ?????????????????????
	 * 
	 * @parameter expression="${encryPasswordChar}"
	 */
	private String encryPasswordChar;
	
	/**
	 * ???????????????????????????????????????
	 * 
	 * @parameter expression="${autoDeletePassword}"
	 */
	private String autoDeletePassword;
	
	private Set<Pattern> excludeEncryPattern = new HashSet<>();
	
	private String basicDir;
	private File basicDirFile;
	private String encryPassword;
	private Set<File> needEncryFiles = new HashSet<>();

	public void execute() throws MojoExecutionException {
		if (project.getPackaging() != null 
				&& "pom".equals(project.getPackaging().toLowerCase())) {
			getLog().info("Skipping 'pom' packaged project");
			return;
		}
		this.basicDir = 
				project.getBuild().getOutputDirectory() + File.separatorChar;
		this.basicDirFile =
				new File(this.basicDir);
		this.encryPassword = 
				initEncryPasswordChar();
		writePasswordToCurDir();
		if (excludeEncryPackagesOrClasses != null
				&& excludeEncryPackagesOrClasses.length > 0) {
			for (String excludeItem : excludeEncryPackagesOrClasses) {
				this.excludeEncryPattern.add(
						Pattern.compile(
								excludeItem));
				getLog().info("??????[?????????????????????]??????????????????:\t" + excludeItem);
			}
		}
		this.encryClassFile();
		getLog().info("?????????????????????");
		this.resetMainClass();
		getLog().info("?????????????????????");
	}
	
	private void writePasswordToCurDir() {
		File curDir = new File(".");
		try {
			FileUtils.write(
					new File(curDir, "decry.psd"), 
					this.encryPassword);
			getLog().warn("????????????????????????????????????[decry.psd]?????????,???????????????");
		} catch (IOException e) {
			getLog().warn("???????????????:" + this.encryPassword);
			getLog().error("writePasswordToCurDir error:" + e);
		}
	}

	/**
	 * ?????????springboot????????????????????????????????????????????????
	 */
	private void resetMainClass() {
		File mainClassFile = null;
		if (this.mainClass != null) {
			try {
				URL resource = 
						getClassLoader().getResource(this.mainClass.replace(".", "/") + ".class");
				if (resource != null) {
					mainClassFile = 
							new File(resource.toURI());
				}
			} catch (Exception e) {
				// ignore
			}
		} else {
			mainClassFile =
					ClassSearcher.searchMainClass(this.basicDir);
		}
		if (mainClassFile == null
				|| !mainClassFile.exists()) {
			if (this.mainClass != null) {
				getLog().warn(this.mainClass + " ??????????????????!!!");
			} else {
				getLog().error("????????????SpringBoot?????????, ????????????????????????SpringBootApplication???????????????????????????????????????????????????????????????");
			}
			throw new IllegalStateException();
		}
		System.out.println("??? ??? ??? ??? ??? ??? :\t" + mainClassFile.getAbsolutePath());
		rewriteSpringMainClass(mainClassFile);
	}

	private void rewriteSpringMainClass(
			File mainClassFile) {
		List<String> startupWithPackages = null;
		if (this.customeClassloaderScanPackages != null
				&& this.customeClassloaderScanPackages.length != 0) {
			startupWithPackages = 
					Arrays.asList(this.customeClassloaderScanPackages);
		}
		String mainClassPackage = 
				mainClassFile.getAbsolutePath().replace(this.basicDir, "")
				.replace(File.separator + mainClassFile.getName(), "")
				.replace(File.separatorChar, '.');
		MainClassRewriter.rewriteSpringBootMainClass(
				new File(this.basicDir),
				mainClassPackage, 
				startupWithPackages,
				mainClassFile,
				(autoDeletePassword == null || autoDeletePassword.trim().isEmpty()) ? true : Boolean.parseBoolean(autoDeletePassword.trim()));
	}

	private String initEncryPasswordChar() {
		return RandomPasswordUtils.getRandomStringByLength(
				encryPasswordChar == null ? DEFAULT_PASSWORD_CHARES : encryPasswordChar,
						16);
	}

	private FileFilter CLASS_FILTER = new FileFilter("");
	
	private void encryClassFile() {
		if (this.encryItemes == null) {
			return;
		}
		File baseDir = new File(this.basicDir);
		for (EncryItem encryItem : this.encryItemes) {
			File encryFile = 
					encryItem.parseToFilePath(baseDir);
			getLog().info("????????????????????????:\t" + encryFile.getAbsolutePath() + "\t???????????????:" + encryFile.isFile());
			encryResource(encryFile);
		}
		
		if (this.needEncryFiles.isEmpty()) {
			getLog().info("Empty files need encry");
			return;
		}
		final File encryDataDir = 
				new File(
						this.basicDir, 
						"configDir");
		encryDataDir.mkdir();
		
		final ExecutorService newFixedThreadPool =
				Executors.newFixedThreadPool(30);
		
		final ConcurrentHashMap<String, String> cipherDatas = 
				new ConcurrentHashMap<String, String>();
		
		final CountDownLatch latch = 
				new CountDownLatch(needEncryFiles.size());
		
		for (File file : needEncryFiles) {
			final File tmpFile = file;
			newFixedThreadPool.execute(new Runnable() {
				@Override
				public void run() {
					try {
						// ?????????????????????????????????????????????
						String resourceKey = null;
						if (tmpFile.getParentFile().equals(basicDirFile)) {
							resourceKey = 
									tmpFile.getName();
						} else {
							resourceKey = 
									new StringBuilder()
									.append(
											tmpFile.getParent().replace(basicDir, ""))
									.append(File.separator)
									.append(tmpFile.getName())
									.toString();
						}
						byte[] readFileToByteArray = 
								FileUtils.readFileToByteArray(tmpFile);
						byte[] cipher = 
								cipher(
										encryPassword,
										readFileToByteArray);
						File saveCipherFile = 
								new File(
										encryDataDir, 
										UUID.randomUUID().toString());
						FileUtils.writeByteArrayToFile(
								saveCipherFile, 
								cipher);
						cipherDatas.put(
								resourceKey, 
								saveCipherFile.getName());
						// ??????????????????
						overlapWithTmpData(
								tmpFile, 
								readFileToByteArray);
						getLog().info("????????????:\t" + resourceKey);
						latch.countDown();
					} catch (IOException e) {
						getLog().error(e);
						System.exit(-1);
					}
				}
			});
		}
		try {
			latch.await();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		newFixedThreadPool.shutdown();
		
		getLog().info("?????????????????????????????????:\t" + encryDataDir.getAbsolutePath());
		
		StringBuilder sb = new StringBuilder();
		for (Entry<String, String> entry : cipherDatas.entrySet()) {
			sb.append(entry.getKey()).append(":").append(entry.getValue()).append(";");
		}
		
		File encryDataFile = 
				new File(
						encryDataDir, 
						"map.data");
		try {
			encryDataFile.createNewFile();
			FileUtils.writeByteArrayToFile(
					encryDataFile,
					cipher(
							encryPassword,
							sb.toString().getBytes()));
			getLog().info("????????????????????????:\t" + encryDataFile.getAbsolutePath());
		} catch (IOException e) {
			getLog().error(e);
			System.exit(-1);
		}
	}
	
	private static final byte[] NORMAL_FILE_TEMP_DATAS = 
			"The current file is encrypted and cannot be viewed".getBytes();

	protected void overlapWithTmpData(
			File tmpFile, 
			byte[] sourceData) throws IOException {
		byte[] tmpDatas = null;
		if (tmpFile.getName().endsWith(".class")) {
			// ?????????????????????????????????????????????????????????
			tmpDatas = 
					ClassBuilder.buildClass(sourceData);
		} else {
			// ???????????????????????????????????????
			tmpDatas = NORMAL_FILE_TEMP_DATAS;
		}
		try {
			FileUtils.writeByteArrayToFile(
					tmpFile,
					tmpDatas);
		} catch (Exception e) {
			getLog().warn("wrong file:" + tmpFile.getName());
		}
	}

	private void encryResource(File itemDir) {
		if (itemDir.isDirectory()) {
			for (File file : itemDir.listFiles(CLASS_FILTER)) {
				if (!excludItem(file)) {
					encryResource(file);
				}
			}
		} else {
			needEncryFiles.add(itemDir);
		}
	}

	private boolean excludItem(File tmpFile) {
		try {
			if (excludeEncryPattern.isEmpty()) {
				return false;
			}
			String resourceKey = 
					new StringBuilder()
					.append(
							tmpFile.getParent().replace(basicDir, "").replace(File.separator, "/"))
					.append("/")
					.append(tmpFile.getName())
					.toString();
			for (Pattern pattern : this.excludeEncryPattern) {
				if (pattern.matcher(resourceKey).matches()) {
					return true;
				}
			}
		} catch (Exception e) {
			getLog().info("tmpFile:" + tmpFile.getAbsolutePath());
			e.printStackTrace();
		}
		return false;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
	private ClassLoader getClassLoader()
    {
        try
        {
        	// ????????????????????????????????????????????? compilePath
            List classpathElements = project.getCompileClasspathElements();
            classpathElements.add( project.getBuild().getOutputDirectory() );
            // ?????? URL ??????
            URL urls[] = new URL[classpathElements.size()];
            for ( int i = 0; i < classpathElements.size(); ++i )
            {
                urls[i] = new File( (String) classpathElements.get( i ) ).toURL();
            }
            // ?????????????????????
            return new URLClassLoader( urls, this.getClass().getClassLoader() );
        }
        catch ( Exception e )
        {
            getLog().debug( "Couldn't get the classloader." );
            return this.getClass().getClassLoader();
        }
    }
	
	private static final String UTF_8 = "utf-8";

	private static final String KEY_ALGORITHM = "AES";

	private static final String DEFAULT_CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding";


	public byte[] cipher(String password, byte[] cipherDatas) {
		// AES??????
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
	
}


