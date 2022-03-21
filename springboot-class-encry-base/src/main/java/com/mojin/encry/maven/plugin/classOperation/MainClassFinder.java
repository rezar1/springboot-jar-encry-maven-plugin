package com.mojin.encry.maven.plugin.classOperation;

import java.io.IOException;
import java.io.InputStream;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;


/**
 * 
 * @say little Boy, don't be sad.
 * @name Rezar
 * @time 2022-03-04 04:19:10
 * @Desc 些年若许,不负芳华.
 *
 */
public class MainClassFinder {
	
	private static final String SPRING_BOOT_ANNOTATION = "Lorg/springframework/boot/autoconfigure/SpringBootApplication;";

	public static boolean isSpringMainClass(
			InputStream is) throws IOException {
		final TwoFlag flag = new TwoFlag();
		ClassReader reader = new ClassReader(is);
		ClassVisitor visitor = new ClassVisitor(Opcodes.ASM5, null) {
			@Override
			public MethodVisitor visitMethod(
					int access, 
					String name, 
					String desc,
					String signature,
					String[] exceptions) {
				if (access == 9 
						&& name.contentEquals("main") 
						&& desc.contentEquals("([Ljava/lang/String;)V")) {
					flag.flag2 = true;
				}
				return null;
			}
			@Override
			public AnnotationVisitor visitAnnotation(
					String desc, 
					boolean visible) {
				if (desc.contentEquals(SPRING_BOOT_ANNOTATION)) {
					flag.flag1 = true;
				}
				return null;
			}
		};
		reader.accept(visitor, 0);
		return flag.isAllOk();
	}
	
	private static class TwoFlag {
		boolean flag1;
		boolean flag2;
		public boolean isAllOk() {
			return this.flag1 && this.flag2;
		}
	}
	
}

