package plugins.text;

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
			doc.insertString(doc.getLength(), "\n"+res+"\n", null);
			edit.repaint();
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

}
