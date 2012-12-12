package utils.installer;
import java.net.MalformedURLException;
import java.net.URL;

public class JTransInstall {
	public static void main(String args[]) {
		System.out.println("ok");
		try {
			WGETJava.DownloadFile(new URL("http://talc1.loria.fr/users/cerisara/jtrans/culture.wav"));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}
}
