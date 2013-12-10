package jtrans.utils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * Modal dialog that displays progress about a background task.
 * Provides an optional 'Cancel' button to abort the task.
 * The dialog is automatically closed when the task is complete.
 */
public class CancelableProgressDialog extends JDialog implements ProgressDisplay {
	private boolean cancelable;
	private JProgressBar progressBar;
	private JLabel infoLabel;
	private JButton cancelButton;
	private SwingWorker worker;

	public CancelableProgressDialog(String title, boolean cancelable) {
		super(null, title, ModalityType.APPLICATION_MODAL);

		cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				cancel();
			}
		});

		progressBar = new JProgressBar(0, 1000);
		progressBar.setValue(0);

		infoLabel = new JLabel("Please wait...");

		// Call setStringPainted now so that the progress bar height
		// stays the same whether or not the string is shown.
		progressBar.setStringPainted(true);
		progressBar.setPreferredSize(new Dimension(512, 32));

		JPanel jp = new JPanel() {{
			setLayout(new BorderLayout(20, 20));
			add(infoLabel, BorderLayout.NORTH);
			add(progressBar, BorderLayout.CENTER);
			add(cancelButton, BorderLayout.EAST);
			setBorder(new EmptyBorder(20, 20, 20, 20));
		}};
		setContentPane(jp);
		pack();

		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				cancel();
			}
		});

		setCancelable(cancelable);
	}

	@Override
	public void setIndeterminateProgress(final String message) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				infoLabel.setText(message);
				progressBar.setEnabled(true);
				progressBar.setIndeterminate(true);
			}
		});
	}

	@Override
	public void setProgress(final String message, final float f) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				infoLabel.setText(message);
				progressBar.setEnabled(true);
				progressBar.setIndeterminate(false);
				progressBar.setValue((int)(f*1000f));
			}
		});
	}

	@Override
	public void setProgressDone() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				dispose();
			}
		});
	}

	private boolean isTaskActive() {
		return worker != null && !worker.isDone();
	}

	public void setTask(final Callable task) {
		worker = new SwingWorker() {
			@Override
			protected Object doInBackground() throws Exception {
				return task.call();
			}

			@Override
			protected void done() {
				dispose();
			}
		};
	}

	/**
	 * May be invoked from any thread.
	 * @param c
	 */
	public void setCancelable(final boolean c) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				cancelable = c;
				cancelButton.setEnabled(c);
			}
		});
	}

	/**
	 * Should be invoked from the event dispatch thread
	 */
	public void executeInForeground() throws InterruptedException, ExecutionException {
		progressBar.setIndeterminate(true);
		worker.execute();
		setLocationByPlatform(true);
		setVisible(true);
		worker.get();
	}

	private void cancel() {
		if (cancelable && isTaskActive()) {
			worker.cancel(true);
			worker = null;
			dispose();
		}
	}
}
