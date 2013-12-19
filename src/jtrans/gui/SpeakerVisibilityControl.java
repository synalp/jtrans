package jtrans.gui;

import jtrans.facade.Project;
import jtrans.gui.trackview.MultiTrackTable;
import jtrans.utils.CrossPlatformFixes;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel that allows hiding/showing specific speaker tracks in a MultiTrackView.
 * When a speaker is talking, their corresponding label in this panel pulses in
 * an attention-grabbing color.
 * @see jtrans.gui.trackview.MultiTrackView
 */
public class SpeakerVisibilityControl extends JPanel {
	private List<PulsatingJCheckBox> boxes = new ArrayList<PulsatingJCheckBox>();

	private static final Color IDLE_BG = UIManager.getColor("CheckBox.background");
	private static final Color IDLE_FG = UIManager.getColor("CheckBox.foreground");
	private static final Color PULSE_BG = new Color(0xF085B0);
	private static final Color PULSE_FG = Color.BLACK;


	// Cached gradient to avoid creating tons of Color objects during playback
	private static final Color[] GRADIENT_BG = linearGradient(12, IDLE_BG, PULSE_BG);
	private static final Color[] GRADIENT_FG = linearGradient(12, IDLE_FG, PULSE_FG);

	private static Color[] linearGradient(int steps, Color c1, Color c2) {
		Color[] fade = new Color[steps];
		for (int i = 0; i < steps; i++) {
			float t1 = (float)i/(float)steps;
			float t2 = 1f - t1;
			fade[i] = new Color(
					(int)(t1*c1.getRed()   + t2*c2.getRed()),
					(int)(t1*c1.getGreen() + t2*c2.getGreen()),
					(int)(t1*c1.getBlue()  + t2*c2.getBlue()));
		}
		return fade;
	}

	/**
	 * JCheckBox whose label can temporarily "pulse" in an
	 * attention-grabbing color.
	 */
	class PulsatingJCheckBox extends JCheckBox {
		int fadeStep;

		public PulsatingJCheckBox(String label, boolean checked) {
			super(label, checked);
			setOpaque(true);
			timer.setRepeats(false);
			fadeOut.setRepeats(true);
		}

		private Timer timer = new Timer(250, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				fadeStep = 0;
				fadeOut.restart();
			}
		});

		private Timer fadeOut = new Timer(50, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (fadeStep >= GRADIENT_FG.length) {
					setBackground(IDLE_BG);
					setForeground(IDLE_FG);
					fadeOut.stop();
				} else {
					setBackground(GRADIENT_BG[fadeStep]);
					setForeground(GRADIENT_FG[fadeStep]);
					fadeStep++;
				}
			}
		});

		public void pulse() {
			setOpaque(true);
			fadeOut.stop();
			timer.restart();
			setBackground(PULSE_BG);
			setForeground(PULSE_FG);
		}
	}

	public SpeakerVisibilityControl(Project project, final MultiTrackTable mtt) {
		super();
		setLayout(new GridLayout(project.tracks.size(), 1, 0, 5));

		final int modifier = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
		final InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

		for (int i = 0; i < project.tracks.size(); i++) {
			final PulsatingJCheckBox box =
					new PulsatingJCheckBox(project.tracks.get(i).speakerName, true);
			add(box);
			boxes.add(box);

			final int finalIdx = i;

			box.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					// Prevent hiding everything
					if (!box.isSelected() && mtt.getVisibleCount() == 1) {
						box.setSelected(true);
					} else {
						mtt.setTrackVisible(finalIdx, box.isSelected());
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

	public void pulse(final int trackIndex) {
		boxes.get(trackIndex).pulse();
	}
}
