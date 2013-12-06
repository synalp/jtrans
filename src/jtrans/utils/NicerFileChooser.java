package jtrans.utils;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * File chooser that lets the user change their mind before overwriting an
 * existing file.
 *
 * Also remembers the last chosen location during the program's lifespan.
 */
public class NicerFileChooser extends JFileChooser {
	// TODO: make this permanent instead of static
	private static File lastLocation = null;

	@Override
	public int showDialog(Component parent, String approveButtonText) {
		setCurrentDirectory(lastLocation);
		return super.showDialog(parent, approveButtonText);
	}

	@Override
	public void approveSelection() {
		File f = getSelectedFile();
		lastLocation = f.getParentFile();
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
