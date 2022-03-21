package com.mojin.encry.maven.plugin.classOperation;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.mojin.encry.maven.plugin.classloader.DecryClassStarter;
import com.mojin.encry.maven.plugin.util.Pair;

/**
 * 
 * @say little Boy, don't be sad.
 * @name Rezar
 * @time 2022-03-04 04:41:39
 * @Desc 些年若许,不负芳华.
 *
 */
public class MainClassRewriter {
	
	public static void rewriteSpringBootMainClass(
			File dir,
			String mainClassPackage,
			List<String> startupWithPackages,
			File mainClassFile, 
			boolean autoDeletePasswordFile) {
		
		String mainClassName = 
				mainClassFile.getName().substring(0, mainClassFile.getName().lastIndexOf("."));
		File curDir = mainClassFile.getParentFile();
		
		// 复制类加载器&反射启动类
		List<Pair<String, byte[]>> copyClassStarter = 
				ClassBuilder.copyClassStarter();
		String startClass = DecryClassStarter.class.getName();
		File decryStartClassDir = 
				new File(
						dir, 
						startClass.substring(
								0, 
								startClass.lastIndexOf(".")).replace(".", File.separator));
		if (!decryStartClassDir.exists()) {
			decryStartClassDir.mkdirs();
		}
		for (Pair<String, byte[]> item : copyClassStarter) {
			File starterHelpClassFile = 
					new File(
							decryStartClassDir,
							item.first);
			writeFile(decryStartClassDir, item.first, item.second);
			System.out.println("已复制加载器相关类:\t" + starterHelpClassFile.getAbsolutePath());
		}
		
		// 修改原始启动类，去掉main方法替换为startup
		byte[] originMainClassDataBytes = 
				readFileBytes(mainClassFile);
		byte[] renameMainClassToStartup = 
				ClassBuilder.renameMainClassToStartup(originMainClassDataBytes);
		writeFile(curDir, mainClassName, renameMainClassToStartup);
		System.out.println("已覆盖原始启动类:\t" + mainClassFile.getAbsolutePath());
		
		String startupWithPackagesStr = null;
		// 创建新的启动类，带main方法
		if (startupWithPackages == null
				|| startupWithPackages.isEmpty()) {
			startupWithPackagesStr = mainClassPackage;
		} else {
			StringBuilder sb = new StringBuilder();
			for (String str : startupWithPackages) {
				sb.append(str).append(";");
			}
			startupWithPackagesStr = sb.toString();
		}
		System.out.println("自定义类加载器加载的包:\t" + startupWithPackagesStr);
		Pair<String, byte[]> createStartClass = 
				ClassBuilder.createStartClass(
						mainClassPackage + "." + mainClassName, 
						startupWithPackagesStr,
						autoDeletePasswordFile);
		writeFile(curDir, createStartClass.first, createStartClass.second);
	}

	private static byte[] readFileBytes(File mainClassFile) {
		try {
			return FileUtils.readFileToByteArray(mainClassFile);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private static void writeFile(
			File curDir,
			String classFileName,
			byte[] classDatas) {
		try {
			FileUtils.writeByteArrayToFile(
					new File(curDir, 
							classFileName + ".class"), 
					classDatas);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

}

