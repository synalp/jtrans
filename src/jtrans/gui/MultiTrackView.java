package jtrans.gui;

import jtrans.facade.Project;
import jtrans.facade.Track;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Panel showing tracks side by side.
 * @see jtrans.gui.SpeakerVisibilityControl
 */
public class MultiTrackView extends JPanel {
	private List<TrackView> views;
	private List<JPanel> compoundViewPanes;
	private boolean[] visibility;
	private int visibleCount;

	public MultiTrackView(Project project, JTransGUI gui) {
		super();

		views = new ArrayList<TrackView>();
		compoundViewPanes = new ArrayList<JPanel>();
		visibility = new boolean[project.tracks.size()];

		for (Track t: project.tracks) {
			final TrackView area = new TrackView(gui, t);
			final JScrollPane scroll = new JScrollPane(area);
			final JLabel speakerLabel = new JLabel(t.speakerName,
					SwingConstants.CENTER);
			speakerLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 24));
			speakerLabel.setEnabled(false);

			views.add(area);

			final JPanel compound = new JPanel(new BorderLayout()) {{
				add(speakerLabel, BorderLayout.NORTH);
				add(scroll, BorderLayout.CENTER);
			}};

			compoundViewPanes.add(compound);
		}

		setAllVisible();
	}

	private void updateVisibleTracks() {
		assert visibility.length == compoundViewPanes.size();

		visibleCount = 0;
		for (Boolean b: visibility)
			visibleCount += b?1:0;

		removeAll();
		setLayout(new GridLayout(1, visibleCount));

		for (int i = 0; i < visibility.length; i++) {
			if (visibility[i])
				add(compoundViewPanes.get(i));
		}

		validate(); // force re-layout
	}

	public void setAllVisible() {
		Arrays.fill(visibility, true);
		updateVisibleTracks();
	}

	public void setTrackVisible(int index, boolean v) {
		assert index >= 0 && index < compoundViewPanes.size();
		visibility[index] = v;
		updateVisibleTracks();
	}

	public void setViewFont(Font font) {
		for (TrackView view: views)
			view.setFont(font);
	}

	public int getVisibleCount() {
		return visibleCount;
	}

	public TrackView getView(int index) {
		return views.get(index);
	}
}
