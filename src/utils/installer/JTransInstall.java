package utils.installer;
import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;

import org.fuin.utils4j.Utils4J;

public class JTransInstall {
	
	final static String baseurl = "http://talc1.loria.fr/users/cerisara/jtrans/";
	
	public static void main(String args[]) {
		System.out.println("starting installer...");
		File fappdesc = new File("app.desc");
		if (!fappdesc.exists()) downloadApp();
		
		runApp();
	}
	
	private static void runApp() {
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
		
		main.Main.main();
	}
	
	private static void downloadApp() {
		File dir = new File(".");
		System.out.println("Downloading application description file in dir "+dir);
		try {
			WGETJava.DownloadFile(new URL(baseurl+"app.desc"));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}
}
