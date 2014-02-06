package fr.loria.synalp.jtrans.utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

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

		if (isOSX) {
			System.setProperty("apple.laf.useScreenMenuBar", "true");
		}

		// Prevent triggering accidental events when clicking outside a popup menu
		UIManager.put("PopupMenu.consumeEventOnClose", Boolean.TRUE);
	}

	/**
	 * Reliably returns whether a mouse event is a popup trigger, regardless of
	 * the MouseListener method that intercepted the event.
	 *
	 * Normally, popup triggers should be checked in both mousePressed and
	 * mouseReleased because there is no standard way to do it across different
	 * platforms. However, this method makes checking for popup triggers
	 * possible in either callback method (at the cost of a slightly lesser
	 * "native" feel on some systems).
	 */
	public static boolean isPopupTrigger(MouseEvent e) {
		return (e.getModifiers() & Event.META_MASK) != 0;
	}
}
