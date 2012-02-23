package plugins.utils;

import java.util.Arrays;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

public abstract class ErrorsReporting {
	public static enum reportingWay {none, console, file, frame};
	
	public static reportingWay currentWay = reportingWay.console;
	
	public static void report(Exception e) {
		final StringBuffer sbuf = new StringBuffer();
		sbuf.append(e.toString());
		sbuf.append(e.getMessage());
		sbuf.append(Arrays.toString(e.getStackTrace())+"\n");
		Throwable t = e.getCause();
		while (t!=null)
		{
			sbuf.append("CAUSE: "+t);
			sbuf.append(t.toString());
			sbuf.append(t.getMessage());
			sbuf.append(Arrays.toString(t.getStackTrace())+"\n");
			t=t.getCause();
		}
		report(sbuf.toString());
	}
	
	public static void report(String x) {
		switch (currentWay) {
		case none: break;
		case console: System.err.println(x); break;
		case file: // TODO break;
		case frame:
			JTextPane txtp = new JTextPane();
			JScrollPane sp = new JScrollPane(txtp);
			txtp.setText(x);
			JOptionPane.showMessageDialog(null, sp);
			break;
		}
	}
}
