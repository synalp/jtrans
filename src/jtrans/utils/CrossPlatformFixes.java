package jtrans.utils;

import javax.swing.*;
import java.awt.event.KeyEvent;

public class CrossPlatformFixes {
	public static final String
			OSX_SHIFT_CHAR = "\u21e7",
			OSX_CLOVER_CHAR = "\u2318",
			OSX_OPTION_CHAR = "\u2325",
			OSX_CONTROL_CHAR = "\u2303";

	public static final boolean isOSX =
			System.getProperty("os.name").contains("OS X");

	public static String getKeyModifiersText(int modifiers) {
		String str = KeyEvent.getKeyModifiersText(modifiers);
		if (isOSX) {
			str = str
					.replace("Meta", OSX_CLOVER_CHAR)
					.replace("Ctrl", OSX_CONTROL_CHAR)
					.replace("Shift", OSX_SHIFT_CHAR)
					.replace("Alt", OSX_OPTION_CHAR)
					.replace("+", "");
			str = new StringBuilder(str).reverse().toString();
			System.out.println(KeyEvent.getKeyText(modifiers));
		}
		return str;
	}

	public static void setNativeLookAndFeel() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		if (isOSX)
			System.setProperty("apple.laf.useScreenMenuBar", "true");
	}
}
