package jtrans.gui;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.fuin.utils4j.Utils4J;
import jtrans.utils.WGETJava;


public class JTransInstall {
	
	final static String baseurl = "http://talc1.loria.fr/users/cerisara/jtrans/";
	
	public static void main(String args[]) {
		System.out.println("starting installer...");
		File flibs = new File("libs");
		if (!flibs.exists()) downloadApp();
		
		runApp(args);
	}
	
	private static void runApp(String args[]) {
		File d = new File("libs");
		File[] jars = d.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File arg0, String arg1) {
				return arg1.endsWith(".jar");
			}
		});
		try {
			for (File jar : jars)
				Utils4J.addToClasspath(jar.toURI().toURL());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		JTransGUI.main(args);
	}
	
	private static void downloadApp() {
		File dir = new File(".");
		System.out.println("Downloading jars in dir "+dir);
		try {
//			WGETJava.DownloadFile(new URL(baseurl+"counts.php"));
			WGETJava.DownloadFile(new URL(baseurl+"res.jar"));
			// res.jar doit contenir tous les jars dans libs/*.jar et toutes les autres resources
			File zipfile = new File("res.jar");
			Utils4J.unzip(zipfile, dir);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
