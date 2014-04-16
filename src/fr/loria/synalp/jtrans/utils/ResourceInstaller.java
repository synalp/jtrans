package fr.loria.synalp.jtrans.utils;

import org.fuin.utils4j.Utils4J;

import javax.swing.*;
import java.io.File;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;

public class ResourceInstaller {

	public static final String CURRENT_VERSION = "20140416";

	public static final String APPX_ZIP_SIZE = "55 MB";

	public static final String APPX_UNPACKED_SIZE = "120 MB";

	public static final String ZIP_URL =
			"http://talc1.loria.fr/users/cerisara/jtrans/jtrans_res_"
					+ CURRENT_VERSION + ".zip";


	public static boolean shouldReinstallResources() {
		return !Paths.RES_DIR.exists();
	}


	/**
	 * Suggests downloading and installing the resources automatically.
	 * If the user declines, the program is aborted.
	 */
	public static void installResources() {
		String message = "JTrans resources are missing. " +
				"Would you like to install them now?\n\n" +
				"Warning: this will trigger a " +
				APPX_ZIP_SIZE + " download.\n" +
				"Once installed, the resources take up about " +
				APPX_UNPACKED_SIZE + " of disk space.\n\n" +
				"Alternatively, you may download the following file:\n" +
				ZIP_URL + "\n" +
				"and unzip it in \"" + Paths.BASE_DIR + "\".";

		System.out.println(message);

		int rc = JOptionPane.showConfirmDialog(null,
				message, "Install resources", JOptionPane.YES_NO_OPTION);

		if (rc != JOptionPane.YES_OPTION) {
			System.exit(1);
		}

		final CancelableProgressDialog progress = new CancelableProgressDialog(
				"Installing JTrans resources", true);

		progress.setTask(new Callable() {
			@Override
			public Object call() throws Exception {
				URL url = new URL(ZIP_URL);
				File zip = FileUtils.createVanishingTempFile("jtrans-res-", ".zip");
				FileUtils.downloadFile(url, zip, progress);
				progress.setCancelable(false);
				progress.setIndeterminateProgress("Decompressing...");
				Paths.BASE_DIR.mkdirs();
				Utils4J.unzip(zip, Paths.BASE_DIR);
				zip.delete();
				progress.setProgressDone();
				return null;
			}
		});

		try {
			progress.executeInForeground();
		} catch (CancellationException ignore) {
			System.out.println("Cancelled");
			System.exit(1);
		} catch (Exception ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(null,
					"Error while installing the resources.\n\n" + ex,
					"Error",
					JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}

		if (!Paths.RES_DIR.exists()) {
			JOptionPane.showMessageDialog(null,
					"Installed resources, yet still can't find " +
					"resource directory!\n" + Paths.RES_DIR,
					"Resources missing",
					JOptionPane.WARNING_MESSAGE);
		}
	}

}
