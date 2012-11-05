package plugins.text;

import java.util.StringTokenizer;

import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import edu.cmu.sphinx.result.Result;
import plugins.speechreco.RecoListener;

public class TextEditorRecoListener implements RecoListener {
	JTextPane edit;
	Document doc;
	public TextEditorRecoListener(JTextPane editor) {
		edit=editor;
		doc = edit.getDocument();
	}
	
	@Override
	public void recoEnCours(Result tmpres) {
		try {
			doc.insertString(doc.getLength(), ".", null);
			edit.repaint();
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void recoFinie(Result finalres, String res) {
		try {
			StringTokenizer st = new StringTokenizer(res, ":\n");
			StringBuilder sb = new StringBuilder();
			sb.append('\n');
			for (;;) {
				if (!st.hasMoreTokens()) break;
				st.nextToken(); st.nextToken();
				String s = st.nextToken();
				if (!s.startsWith("SIL")) {
					sb.append(s); sb.append(' ');
				}
			}
			sb.append('\n');
			doc.insertString(doc.getLength(), sb.toString(), null);
			edit.repaint();
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

}
