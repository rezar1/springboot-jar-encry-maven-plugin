package com.mojin.encry.maven.plugin.util;


/**
 * 
 * @say little Boy, don't be sad.
 * @name Rezar
 * @time 2022-03-04 06:59:33
 * @Desc 些年若许,不负芳华.
 *
 */
public class Pair<F, S> {
	
	public final F first;
	public final S second;
	public Pair(F first, S second) {
		super();
		this.first = first;
		this.second = second;
	}
	
	public static <F,S> Pair<F,S> of(F first, S second) {
		return new Pair<F, S>(first, second);
	}
}

