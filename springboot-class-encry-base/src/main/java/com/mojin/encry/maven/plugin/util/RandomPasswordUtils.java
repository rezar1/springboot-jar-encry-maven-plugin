package com.mojin.encry.maven.plugin.util;

import java.util.Random;

/**
 * 
 * @say little Boy, don't be sad.
 * @name Rezar
 * @time 2018年1月3日
 * @Desc this guy is to lazy , noting left.
 *
 */
public class RandomPasswordUtils {

	/**
	 * 获取一定长度的随机字符串
	 * 
	 * @param length
	 *            指定字符串长度
	 * @return 一定长度的字符串
	 */
	public static String getRandomStringByLength(String base, int length) {
		Random random = new Random();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < length; i++) {
			int number = random.nextInt(base.length());
			sb.append(base.charAt(number));
		}
		return sb.toString();
	}

}
