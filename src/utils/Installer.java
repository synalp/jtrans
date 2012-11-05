package utils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JOptionPane;

import utils.FileUtils;
import utils.WGETJava;

public class Installer {
	
	public static void main(String args[]) {
		if ((new File("culture.jtr")).exists())
			launchJSafran(args);
		else {
			try {
				WGETJava.DownloadFile(new URL("http://talc1.loria.fr/users/cerisara/jtrans/culture.wav"));
				WGETJava.DownloadFile(new URL("http://talc1.loria.fr/users/cerisara/jtrans/culture.txt"));
				WGETJava.DownloadFile(new URL("http://talc1.loria.fr/users/cerisara/jtrans/culture.jtr"));
				String[] ar = {"culture.jtr"};
				launchJSafran(ar);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void launchJSafran(final String[] args) {
		if (!isAlreadyInstalled()) install();
		try {
			plugins.applis.SimpleAligneur.Aligneur.main(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void install() {
		File tf = new File(".");
		int rep = JOptionPane.showConfirmDialog(null, "First time run detected. OK for downloading all resources (400Mb) in "+tf.getAbsolutePath()+"?");
		if (rep==JOptionPane.OK_OPTION) {
			try {
				URL resurl = new URL("http://talc1.loria.fr/users/cerisara/jtrans/jtransres.zip");
				WGETJava.DownloadFile(resurl);
				FileUtils.unzip("jtransres.zip");
				System.out.println("installation successful");
				File f = new File("jtransres.zip");
				f.delete();
			} catch (Exception e) {
				JOptionPane.showMessageDialog(null, "error installing: "+e.toString());
			}
		}
	}
	public static boolean isAlreadyInstalled() {
		File f = new File("./res/acmod/ESTER2_Train_373f_a01_s01.f04.lexV02_alg01_ter.cd_2500.mdef");
		if (!f.exists()) return false;
		return true;
	}
}

