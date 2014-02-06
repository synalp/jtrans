package fr.loria.synalp.jtrans.utils;


/**
 * to be used with Progressdialog
 * 
 * @author xtof
 *
 */
public interface Interruptable extends Runnable {
	public void stopit();
}
