package fr.loria.synalp.jtrans.utils;

import org.fuin.utils4j.Utils4J;

import javax.swing.*;
import java.io.File;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;

public class ResourceInstaller {

	public static final String CURRENT_VERSION = "20140416";

	public static final String BASE_URL =
			"http://talc1.loria.fr/users/cerisara/jtrans/";

	public static final ResourceInstaller standardResourceInstaller =
			new ResourceInstaller(
					Paths.RES_DIR,
					BASE_URL + "jtrans_res_" + CURRENT_VERSION + ".zip",
					"JTrans Base Resources",
					55, 120);

	public static final ResourceInstaller asrResourceInstaller =
			new ResourceInstaller(
					new File(Paths.RES_DIR, "LM_africain_3g.sorted.arpa.utf8.dmp"),
					BASE_URL + "jtrans_asr_" + CURRENT_VERSION + ".zip",
					"ASR Resources (Fat Trigram)",
					335, 707);

	private File expectedResourceFile;
	private String zipUrl;
	private String caption;
	private int zippedMB;
	private int unzippedMB;

	public ResourceInstaller(
			File expectedResourceFile,
			String zipUrl,
			String caption,
			int zippedMB,
			int unzippedMB)
	{
		this.expectedResourceFile = expectedResourceFile;
		this.zipUrl = zipUrl;
		this.caption = caption;
		this.zippedMB = zippedMB;
		this.unzippedMB = unzippedMB;
	}

	public boolean isInstalled() {
		return expectedResourceFile.exists();
	}

	/**
	 * Installs the resources
	 * @return true
	 */
	public boolean check() {
		return isInstalled() || install();
	}

	/**
	 * Suggests downloading and installing the resources automatically.
	 * If the user declines, the program is aborted.
	 */
	public boolean install() {
		final File extractIn = expectedResourceFile.getParentFile();

		String message = caption + " missing. " +
				"Would you like to install this now?\n\n" +
				"Warning: this will trigger a " +
				zippedMB + " MB download.\n" +
				"Once installed, the resources take up about " +
				unzippedMB + " MB of disk space.\n\n" +
				"Alternatively, you may download the following file:\n" +
				zipUrl + "\n" +
				"and unzip it in \"" + extractIn + "\".";

		System.out.println(message);

		int rc = JOptionPane.showConfirmDialog(null,
				message, "Install resources", JOptionPane.YES_NO_OPTION);

		if (rc != JOptionPane.YES_OPTION) {
			return false;
		}

		final CancelableProgressDialog progress = new CancelableProgressDialog(
				"Installing " + caption, true);

		progress.setTask(new Callable() {
			@Override
			public Object call() throws Exception {
				URL url = new URL(zipUrl);
				File zip = FileUtils.createVanishingTempFile("jtrans-res-", ".zip");
				FileUtils.downloadFile(url, zip, progress);
				progress.setCancelable(false);
				progress.setIndeterminateProgress("Decompressing...");
				extractIn.mkdirs();
				Utils4J.unzip(zip, extractIn);
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
			return false;
		}

		if (!Paths.RES_DIR.exists()) {
			JOptionPane.showMessageDialog(null,
					"Installed resources, yet still can't find " +
					"resource directory!\n" + Paths.RES_DIR,
					"Resources missing",
					JOptionPane.WARNING_MESSAGE);
		}

		return true;
	}

}
