package main;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.net.URL;
import javax.swing.JApplet;
import javax.swing.SwingUtilities;

import plugins.applis.SimpleAligneur.Aligneur;
import plugins.utils.ErrorsReporting;
import plugins.utils.ErrorsReporting.reportingWay;

public class AppletGUI extends JApplet implements FocusListener {
	//Called when this applet is loaded into the browser.
	public void init() {
		//Execute a job on the event-dispatching thread:
		//creating this applet's GUI.
		try {
			ErrorsReporting.currentWay=reportingWay.frame;
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					createGUI();
				}
			});
		} catch (Exception e) {
			ErrorsReporting.report("AppletGUI Exception in createGUI");
			ErrorsReporting.report(e);
		}
	}
	public void start() {
		if (pan!=null) pan.requestFocusInWindow();
	}
    public String getAppletInfo() {
        return "Title: JSafran\n"
               + "Author: Christophe Cerisara\n"
               + "A lightweight version of the JSafran application designed to run in w browser.";
    }
    
    Aligneur pan=null;
    
	/**
     * Create the GUI. For thread safety, this method should
     * be invoked from the event-dispatching thread.
     */
    private void createGUI() {
    	System.out.println("creating GUI");
    	setFocusable(true);
    	pan = new Aligneur(false);
    	setJMenuBar(pan.createMenus());
//    	pan.setBackground(Color.white);
    	setContentPane(pan);
    	System.out.println("ok for now");
    	URL baseurl = getDocumentBase();

    	System.out.println("document base url "+baseurl);
    	String jtrfile = baseurl.toString();
    	int i=jtrfile.lastIndexOf('/');
    	jtrfile = jtrfile.substring(0,i+1)+"culture.jtr";
    	pan.friendlyLoadProject(new File(jtrfile));
    	System.out.println("GUI created !");
    	pan.inputControls();
    	
    	addFocusListener(this);

    	setVisible(true);
    }
    
	@Override
	public void focusGained(FocusEvent e) {
		System.out.println("gained focus");
	}
	@Override
	public void focusLost(FocusEvent e) {
		System.out.println("lost focus");
	}
}
