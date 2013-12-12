package jtrans.gui;

import jtrans.facade.Project;
import jtrans.utils.CrossPlatformFixes;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Panel that allows hiding/showing specific speaker tracks in a MultiTrackView.
 * @see jtrans.gui.MultiTrackView
 */
public class SpeakerVisibilityControl extends JPanel {
	public SpeakerVisibilityControl(Project project, final MultiTrackView mtv) {
		super();
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		final int modifier = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
		final InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

		for (int i = 0; i < project.tracks.size(); i++) {
			final JCheckBox box =
					new JCheckBox(project.tracks.get(i).speakerName, true);
			add(box);

			final int finalIdx = i;

			box.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					// Prevent hiding everything
					if (!box.isSelected() && mtv.getVisibleCount() == 1) {
						box.setSelected(true);
					} else {
						mtv.setTrackVisible(finalIdx, box.isSelected());
					}
				}
			});

			// Create shortcut on number keys (1 through 9)
			if (i < 9) {
				int number = i + 1;

				Action action = new AbstractAction() {
					@Override
					public void actionPerformed(ActionEvent e) {
						box.doClick();
					}
				};

				KeyStroke stroke = KeyStroke.getKeyStroke(
						Integer.toString(number).charAt(0), modifier);

				inputMap.put(stroke, stroke);
				getActionMap().put(stroke, action);
				box.setText(String.format("%s (%s%s%d)",
						box.getText(),
						CrossPlatformFixes.getKeyModifiersText(modifier),
						CrossPlatformFixes.isOSX ? "" : " ",
						number));
			}
		}
	}
}
