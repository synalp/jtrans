package utils;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.rmi.server.UID;
import java.util.concurrent.ArrayBlockingQueue;

import javax.swing.JFrame;

/**
 * @author David Cheeseman
 *	DePauw '08
 *	Last revised 080505.
 */
public class WGETJava {

	/**
	 * Command line component.
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length != 1) {
			outputUsage();
		} else {
			try {
				DownloadFile(new URL(args[0]));
			} catch (MalformedURLException e) {
				e.printStackTrace();
				System.exit(1);
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(2);
			}
		}
	}

	/**
	 * Outputs the usage of the command line component.
	 */
	private static void outputUsage() {
		System.out.println("USAGE: ");
		System.out
		.println("java -jar PHPDownloader.jar %URL OF PHP DOWNLOAD WITH QUOTES%");
	}

	/**
	 * This function downloads the file specified in the URL to the
	 * current working directory.
	 * @param theURL
	 * The URL of the file to be downloaded.
	 * @return
	 * An integer result based on the WGETJavaResults Enumeration.
	 * Values Include:
	 * 	FAILED_IO_EXCEPTION - Could not open a connection to the URL.
	 * 	FAILED_UKNOWNTYPE - Could not determine the file type.
	 * 	COMPLETE - Downloaded completed sucessfully.
	 * @throws IOException
	 */
	public static WGETJavaResults DownloadFile(final URL theURL) {

		final utils.ProgressDialog waiting = new utils.ProgressDialog((JFrame)null,null,"please wait: downloading...");
		final ArrayBlockingQueue<WGETJavaResults> res = new ArrayBlockingQueue<WGETJavaResults>(1);
		Interruptable searchingproc = new Interruptable() {
			private boolean tostop=false;
			public void stopit() {
				tostop=true;
			}
			@Override
			public void run() {
				try {
					URLConnection con;
					UID uid = new UID();

					con = theURL.openConnection();
					con.connect();

					int len = con.getContentLength();
					int downloadedLen = 0;
					String type = con.getContentType();
					System.out.println("type "+type);

					if (type != null) {
						byte[] buffer = new byte[4 * 1024];
						int read;

						String theFile = theURL.getPath();
						theFile = theFile.substring(theFile.lastIndexOf('/')+1);
						System.out.println("the file "+theFile);

						FileOutputStream os = new FileOutputStream(theFile);
						InputStream in = con.getInputStream();

						while ((read = in.read(buffer)) > 0) {
							if (tostop) break;
							os.write(buffer, 0, read);
							downloadedLen+=read;
							float done=(float)downloadedLen/(float)len;
							waiting.setProgress(done);
						}

						os.close();
						in.close();

						res.put(WGETJavaResults.COMPLETE);
					} else {
						res.put(WGETJavaResults.FAILED_UKNOWNTYPE);
					}
					return;
				} catch (Exception e) {
					e.printStackTrace();
				}
				try {
					res.put(WGETJavaResults.FAILED_UKNOWNTYPE);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		waiting.setRunnable(searchingproc);
		waiting.setVisible(true);
		WGETJavaResults r;
		try {
			r = res.take();
			return r;
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		}
	}
}
