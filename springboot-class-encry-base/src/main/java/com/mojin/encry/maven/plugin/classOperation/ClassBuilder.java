package com.mojin.encry.maven.plugin.classOperation;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.CheckClassAdapter;

import com.mojin.encry.maven.plugin.classloader.DecryClassStarter;
import com.mojin.encry.maven.plugin.classloader.InnerURLConnection;
import com.mojin.encry.maven.plugin.classloader.InnerURLStreamHandler;
import com.mojin.encry.maven.plugin.util.Pair;



/**
 * 
 * @say little Boy, don't be sad.
 * @name Rezar
 * @time 2022-03-02 05:46:46
 * @Desc 些年若许,不负芳华.
 *
 */
public class ClassBuilder implements Opcodes {
	
	
	private static List<Pair<String, byte[]>> allStarterClassByteDatas = new ArrayList<>();
	
	static {
		allStarterClassByteDatas.add(loadClassByteCodes(DecryClassStarter.class));
		allStarterClassByteDatas.add(loadClassByteCodes(InnerURLConnection.class));
		allStarterClassByteDatas.add(loadClassByteCodes(InnerURLStreamHandler.class));
	}
	
	public static List<Pair<String, byte[]>> copyClassStarter() {
		return Collections.unmodifiableList(allStarterClassByteDatas);
	}
	
	/**
	 * 复制原始类，只包含类声明
	 * 
	 * @param sourceDatas
	 * @return
	 * @throws IOException
	 */
	public static byte[] buildClass(byte[] sourceDatas) throws IOException {
		ClassReader reader = new ClassReader(sourceDatas);
		final ClassWriter cw = new ClassWriter(0);
		ClassVisitor visitor = new ClassVisitor(Opcodes.ASM5, cw) {
			@Override
			public void visit(int version, int access, String name, String signature, String superName,
					String[] interfaces) {
				this.cv.visit(version, access, name, signature, superName, null);
			}
			@Override
			public void visitSource(String arg0, String arg1) {
			}
			
			@Override
			public MethodVisitor visitMethod(int arg0, String arg1, String arg2, String arg3, String[] arg4) {
				return null;
			}
			
			@Override
			public FieldVisitor visitField(int arg0, String arg1, String arg2, String arg3, Object arg4) {
				return null;
			}
		};
		reader.accept(visitor, 0);
		return cw.toByteArray();
	}
	
	/**
	 * 修改springboot启动类，将main方法改为startup方法
	 * 
	 * @param sourceDatas
	 * @return
	 */
	public static byte[] renameMainClassToStartup(
			byte[] sourceDatas) {
		ClassReader reader = new ClassReader(sourceDatas);
		final ClassWriter cw = new ClassWriter(0);
		ClassVisitor visitor = new ClassVisitor(Opcodes.ASM5, cw) {
			@Override
			public MethodVisitor visitMethod(
					int access,
					String name, 
					String desc,
					String arg3, 
					String[] arg4) {
				// 将原始启动类的main方法替换为startup方法名
				if (access == 9 
						&& name.contentEquals("main") 
						&& desc.contentEquals("([Ljava/lang/String;)V")) {
					return super.visitMethod(access, "startup", desc, arg3, arg4);
				}
				return super.visitMethod(access, name, desc, arg3, arg4);
			}
		};
		reader.accept(visitor, 0);
		return cw.toByteArray();
	}
	
	/**
	 * 创建新的springboot启动类，改类将调用 MainClassInvoker， MainClassInvoker然后反射调用原始springboot启动类的startup方法
	 * 
	 * @param originStartClass
	 * @param decryIncludePackages
	 * @param startupWithPackagesStr 
	 * @return
	 */
	public static Pair<String,byte[]> createStartClass(
			final String originStartClass,
			final String decryIncludePackages,
			final boolean autoDeletePasswordFile) {
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		CheckClassAdapter cv = new CheckClassAdapter(cw);
		
		String originStartClassPackage = 
				originStartClass.substring(
						0,
						originStartClass.lastIndexOf(".")).replace(".", "/") + "/";
		
		String tmpClassName = "StartClass_" + System.currentTimeMillis();
		
		String className =
				originStartClassPackage + tmpClassName;

		cv.visit(V1_7, ACC_PUBLIC, className, null, Type.getInternalName(Object.class), null);

		MethodVisitor mv;

		{
			mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
			mv.visitInsn(RETURN);
			Label l1 = new Label();
			mv.visitLabel(l1);
			mv.visitLocalVariable("this", "L" + className + ";", null, l0, l1, 0);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}

		{
			mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC + ACC_FINAL, "main", "([Ljava/lang/String;)V", null, null);
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitLdcInsn(originStartClass);
			mv.visitLdcInsn(DecryClassStarter.class.getName());
			mv.visitLdcInsn(decryIncludePackages);
			mv.visitLdcInsn(String.valueOf(autoDeletePasswordFile));
			mv.visitMethodInsn(
					INVOKESTATIC,
					DecryClassStarter.class.getName().replace(".", "/"),
					"invoke",
					"([Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", false);
			mv.visitInsn(RETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}
		cv.visitEnd();
		return Pair.of(tmpClassName, cw.toByteArray());
	}
	
	private static Pair<String, byte[]> loadClassByteCodes(Class<?> clazz){
		try {
			InputStream resourceAsStream = 
					ClassBuilder.class.getClassLoader().getResourceAsStream(
							clazz.getName().replace(".", "/") + ".class");
			byte[] byteCodeDatas = new byte[resourceAsStream.available()];
			IOUtils.readFully(resourceAsStream, byteCodeDatas);
			return Pair.of(clazz.getSimpleName(), byteCodeDatas);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
	
}
