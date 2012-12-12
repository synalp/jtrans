package utils.installer;
/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/



/*
This library is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public
License as published by the Free Software Foundation; either
version 2 of the license, or (at your option) any later version.
 */


import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
Dialog which displays indeterminate progress.

@author <a href="mailto:jclasslib@ej-technologies.com">Ingo Kegel</a>
@version $Revision: 1.1 $ $Date: 2003/08/18 07:46:43 $
 */
public class ProgressDialog extends JDialog {

	private static final int PROGRESS_BAR_WIDTH = 200;

	private Runnable runnable;

	private JProgressBar progressBar;
	private JLabel lblMessage;

	public boolean isDeterminate = false;
	
	public void setProgress(float v) {
		progressBar.setValue((int)(v*100f));
		progressBar.setIndeterminate(false);
		repaint();
	}
	
	/**
	 * Constructor.
	 * @param parent the parent frame.
	 * @param runnable the <tt>Runnable</tt> to be started on <tt>setVisible</tt>.
	 * @param message the initial status message.
	 */
	public ProgressDialog(JFrame parent, Runnable runnable, String message) {
		super(parent);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		init(runnable, message);
	}

	/**
	 * Constructor.
	 * @param parent the parent dialog.
	 * @param runnable the <tt>Runnable</tt> to be started on <tt>setVisible</tt>.
	 * @param message the initial status message.
	 */
	public ProgressDialog(JDialog parent, Runnable runnable, String message) {
		super(parent);
		init(runnable, message);
	}

	/**
	 * Set the current status message.
	 * @param message the message.
	 */
	public void setMessage(String message) {
		lblMessage.setText(message);
	}

	/**
	 * Set the  <tt>Runnable</tt> to be started on <tt>setVisible</tt>.
	 * @param runnable the <tt>Runnable</tt>.
	 */
	public void setRunnable(Runnable runnable) {
		this.runnable = runnable;
	}

	public void setVisible(boolean visible) {
		if (visible && !isDeterminate) {
			progressBar.setIndeterminate(true);
		} else {
			progressBar.setIndeterminate(false);
		}
		super.setVisible(visible);
	}

	private void init(Runnable runnable, String message) {
		setupControls();
		setMessage(message);
		setupComponent();
		setupEventHandlers();
		setRunnable(runnable);
	}

	private void setupControls() {

		progressBar = new JProgressBar();
		Dimension preferredSize = progressBar.getPreferredSize();
		preferredSize.width = PROGRESS_BAR_WIDTH;
		progressBar.setPreferredSize(preferredSize);
		lblMessage = new JLabel(" ");
	}

	private void setupComponent() {

		JPanel contentPane = (JPanel)getContentPane();
		contentPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new GridBagLayout());
		GridBagConstraints gc = new GridBagConstraints();
		gc.gridx = 0;
		gc.gridy = GridBagConstraints.RELATIVE;
		gc.anchor = GridBagConstraints.NORTHWEST;
		contentPane.add(lblMessage, gc);
		gc.weightx = 1;
		gc.fill = GridBagConstraints.HORIZONTAL;
		contentPane.add(progressBar, gc);

		setTitle("");
		setModal(true);
		pack();

		addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
			}
			@Override
			public void keyReleased(KeyEvent e) {
			}
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode()==KeyEvent.VK_ESCAPE)
					dispose();
			}
		});
	}

	private void setupEventHandlers() {

		addComponentListener(new ComponentAdapter() {
			public void componentShown(ComponentEvent event) {
				final Thread task = new Thread(runnable);
				task.start();
				new Thread() {
					public void run() {
						try {
							task.join();
						} catch (InterruptedException e) {
						}
						dispose();
//						SwingUtilities.invokeLater(new Runnable() {
//							public void run() {
//								setVisible(false);
//							}
//						});
					}
				}.start();
			}
		});
	}

	public static void main(String args[]) {
		ProgressDialog m = new ProgressDialog((JFrame)null, new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(4000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}, "please wait");
		m.setVisible(true);
	}
}
