package fr.loria.synalp.jtrans.gui;

import fr.loria.synalp.jtrans.facade.Project;
import fr.loria.synalp.jtrans.gui.trackview.MultiTrackTable;
import fr.loria.synalp.jtrans.markup.MarkupLoader;
import fr.loria.synalp.jtrans.markup.preprocessors.TCOFWhoifier;
import pro.ddopson.ClassEnumerator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;

public class LoaderChooser extends JDialog {

	private File file;
	private MarkupLoader markupLoader;
	private JScrollPane previewPane;
	private JTextArea messageArea;
	private JButton loadButton;

	private static final String WELCOME_MESSAGE =
			"Please select the adequate markup loader on the left.";

	private static final String FAILURE_MESSAGE =
			"Couldn't parse file with markup loader '%s'.\n\n" +
			"This most likely means that the format you chose is incorrect. " +
			"Please try another format from the list to the left.\n\n" +
			"Exception: %s\n(full stack trace on stderr)";


	private void populateLoaderButtons(
			ArrayList<Class<?>> loaders,
			ButtonGroup buttonGroup,
			JComponent addTo,
			Package pkg)
	{
		for (final Class clazz: loaders) {
			if (clazz.isInterface() || !clazz.getPackage().equals(pkg)) {
				continue;
			}

			final Class<? extends MarkupLoader> mlClass;

			try {
				mlClass = clazz.asSubclass(MarkupLoader.class);
			} catch (ClassCastException ex) {
				continue;
			}

			JRadioButton jrb = new JRadioButton(clazz.getSimpleName());
			buttonGroup.add(jrb);
			addTo.add(jrb);

			jrb.addActionListener(new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					tryLoader(mlClass);
				}
			});
		}
	}


	private void failureScreen(Class clazz, Exception ex) {
		messageArea.setText(String.format(FAILURE_MESSAGE, clazz, ex.toString()));
		previewPane.setViewportView(messageArea);
	}


	private void tryLoader(Class<? extends MarkupLoader> clazz) {
		Project project;

		loadButton.setEnabled(false);
		loadButton.setText("Load with " + clazz.getSimpleName());

		try {
			markupLoader = clazz.getConstructor().newInstance();
			project = markupLoader.parse(file);
		} catch (Exception ex) {
			markupLoader = null;
			ex.printStackTrace();
			failureScreen(clazz, ex);
			return;
		}

		previewPane.setViewportView(new MultiTrackTable(project, null, false));
		loadButton.setEnabled(true);
	}


	public LoaderChooser(File file) {
		super((Frame)null, "Format preview for " + file, true);

		this.file = file;

		//----------------------------------------------------------------------
		// Loader buttons

		ArrayList<Class<?>> loaders = ClassEnumerator.getClassesForPackage(
				MarkupLoader.class.getPackage());

		ButtonGroup loaderBG = new ButtonGroup();

		Box vanillaBox = new Box(BoxLayout.Y_AXIS);
		vanillaBox.setBorder(BorderFactory.createTitledBorder(
				"Vanilla parsers"));
		populateLoaderButtons(loaders, loaderBG, vanillaBox,
				MarkupLoader.class.getPackage());

		Box preprocessorBox = new Box(BoxLayout.Y_AXIS);
		preprocessorBox.setBorder(BorderFactory.createTitledBorder(
				"Preprocessors"));
		populateLoaderButtons(loaders, loaderBG, preprocessorBox,
				TCOFWhoifier.class.getPackage());

		JPanel chooserPane = new JPanel(new GridLayout(2, 1));
		chooserPane.add(vanillaBox);
		chooserPane.add(preprocessorBox);

		//----------------------------------------------------------------------
		// Center area

		messageArea = new JTextArea(WELCOME_MESSAGE);
		messageArea.setLineWrap(true);
		messageArea.setWrapStyleWord(true);
		messageArea.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));

		previewPane = new JScrollPane();
		previewPane.setViewportView(messageArea);

		JPanel previewPaneWrapper = new JPanel(new GridLayout(1,1)) {{
			setBorder(BorderFactory.createTitledBorder("Live preview"));
			add(previewPane);
			setPreferredSize(new Dimension(512, 512));
		}};

		//----------------------------------------------------------------------
		// OK/Cancel buttons

		JPanel bottomPane = new JPanel();

		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				markupLoader = null;
				LoaderChooser.this.dispose();
			}
		});

		loadButton = new JButton("Load");
		loadButton.setDefaultCapable(true);
		loadButton.setEnabled(false);
		loadButton.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (markupLoader == null)
					return;
				LoaderChooser.this.dispose();
			}
		});

		getRootPane().setDefaultButton(loadButton);

		bottomPane.add(cancelButton);
		bottomPane.add(loadButton);

		//----------------------------------------------------------------------
		// Add everything

		JPanel bigPane = new JPanel(new BorderLayout(10, 10));
		bigPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		bigPane.add(previewPaneWrapper, BorderLayout.CENTER);
		bigPane.add(chooserPane, BorderLayout.LINE_START);
		bigPane.add(bottomPane, BorderLayout.PAGE_END);

		setContentPane(bigPane);
		pack();
	}


	/**
	 * Returns the chosen valid markup loader, or null if the markup loader is
	 * invalid.
	 */
	public MarkupLoader getMarkupLoader() {
		return markupLoader;
	}

}
