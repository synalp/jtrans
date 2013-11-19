package utils;

import javax.swing.*;
import java.io.File;

/**
 * File chooser that lets the user change their mind before overwriting an
 * existing file.
 */
public class NicerFileChooser extends JFileChooser {
	@Override
	public void approveSelection() {
		File f = getSelectedFile();
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
