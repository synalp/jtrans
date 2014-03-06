package fr.loria.synalp.jtrans.utils;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.prefs.Preferences;

/**
 * File chooser that lets the user change their mind before overwriting an
 * existing file.
 *
 * Also remembers the last chosen location during the program's lifespan.
 */
public class NicerFileChooser extends JFileChooser {

	private final String prefKey;
	private final Preferences prefs;


	/**
	 * @param prefID ID of the file chooser so that each kind of chooser may
	 *               have its own last location
	 */
	public NicerFileChooser(String prefID) {
		prefKey = "NicerFileChooser last location " + prefID;
		prefs = Preferences.userRoot().node("fr.loria.synalp.jtrans");
	}


	@Override
	public int showDialog(Component parent, String approveButtonText) {
		String lastLocation = prefs.get(prefKey, null);

		if (lastLocation != null) {
			setCurrentDirectory(new File(lastLocation));
		}

		return super.showDialog(parent, approveButtonText);
	}


	@Override
	public void approveSelection() {
		File f = getSelectedFile();
		prefs.put(prefKey, f.getParentFile().getAbsolutePath());

		if (SAVE_DIALOG != getDialogType() || !f.exists()) {
			super.approveSelection();
			return;
		}

		int rc = JOptionPane.showConfirmDialog(getParent(),
				"The file \"" + f.getName() + "\" already exists.\n" +
						"Do you want to replace it?\n\n" +
						"Its previous contents will be lost.",
				"Existing file",
				JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.WARNING_MESSAGE);

		if (rc == JOptionPane.CANCEL_OPTION) {
			super.cancelSelection();
		} else if (rc == JOptionPane.YES_OPTION) {
			super.approveSelection();
		}
	}
}
