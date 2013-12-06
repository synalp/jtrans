package jtrans.speechreco;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.sphinx.util.NISTAlign;

import jtrans.utils.SuiteDeMots;

public class RecoUtterance extends ArrayList<RecoWord> {
	private static final long serialVersionUID = 1L;
	public String defaultEncoding="UTF-8";
    
	/**
	 * ne clone pas les mots eux-memes !
	 */
	@Override
	public RecoUtterance clone() {
		RecoUtterance ru = new RecoUtterance();
		ru.addAll(this);
		return ru;
	}
	
	@Override
	public int hashCode() {
		int tot=0;
		for (RecoWord rw : this) tot+=rw.hashCode();
		return tot;
	}
	@Override
	public boolean equals(Object o) {
		RecoUtterance u = (RecoUtterance)o;
		if (size()!=u.size()) return false;
		for (int i=0;i<size();i++) {
			RecoWord rw = get(i);
			if (!rw.equals(u.get(i))) return false;
		}
		return true;
	}
	
	public String toString() {
		String s = "";
		for (RecoWord w : this) {
			s+=w.word+" ";
		}
		return s;
	}
	
    public void saveLab(String labFile) {
    	try {
    		PrintWriter f = new PrintWriter(new OutputStreamWriter(new FileOutputStream(labFile), Charset.forName(defaultEncoding)));
    		for (int i=0;i<size();i++) {
    			f.println(get(i).frameDeb+" "+get(i).frameEnd+" "+get(i).word);
    		}
    		f.close();
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
	}

    public void loadLab(String labfile) {
    	try {
			BufferedReader f = new BufferedReader(new InputStreamReader(new FileInputStream(labfile),Charset.forName(defaultEncoding)));
			for (;;) {
				String s=f.readLine();
				if (s==null) break;
				String[] ss = s.split(" ");
				RecoWord w = new RecoWord();
				if (ss.length==1) {
					w.word=ss[0];
					add(w);
				} else if (ss.length>=3) {
					w.frameDeb=Integer.parseInt(ss[0]);
					w.frameEnd=Integer.parseInt(ss[1]);
					w.word=ss[2];
					add(w);
				}
			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    public void calcWER(NISTAlign aligner, RecoUtterance ref) {
		String[] rectab = new String[size()];
		for (int i=0;i<rectab.length;i++) {
			rectab[i]=get(i).word;
		}
		String[] reftab = new String[ref.size()];
		for (int i=0;i<reftab.length;i++) {
			reftab[i]=ref.get(i).word;
		}
    	
		SuiteDeMots refs = new SuiteDeMots(reftab);
		SuiteDeMots recs = new SuiteDeMots(rectab);
		SuiteDeMots as = refs.normaliseForAccuracy();
		SuiteDeMots bs = recs.normaliseForAccuracy();
		aligner.align(as.toString(),bs.toString());
		aligner.printNISTSentenceSummary();
    }
    public void calcWER(NISTAlign aligner, String refString) {
		String[] rec = new String[size()];
		for (int i=0;i<rec.length;i++) {
			rec[i]=get(i).word;
		}

		SuiteDeMots refs = new SuiteDeMots(refString);
		SuiteDeMots recs = new SuiteDeMots(rec);
		SuiteDeMots as = refs.normaliseForAccuracy();
		SuiteDeMots bs = recs.normaliseForAccuracy();
		aligner.align(as.toString(),bs.toString());
		aligner.printNISTSentenceSummary();
    }
    public void cutAndAppendSegmentsRecouvrants(List<RecoWord> ws, boolean cutFirst, boolean cutSecond, boolean join) {
		if (ws.size()==0) return;
		if (size()==0) addAll(ws);
		else {
			int f0 = ws.get(0).frameDeb;
			int wordInConflict =  0;
			while (wordInConflict<size()&&get(wordInConflict).frameEnd<f0)
				wordInConflict++;
			if (wordInConflict<size()) {
				int middlefr = (get(wordInConflict).frameDeb+get(size()-1).frameEnd)/2;
				if (cutFirst) {
					// on supprime les anciens mots apres middlefr
					for (int i=size()-1;i>=0;i--) {
						if (get(i).frameEnd>middlefr) remove(i);
						else break;
					}
				}
				if (cutSecond) {
					// on supprime les nouveaux mots avant middlefr
					for (int i=ws.size()-1;i>=0;i--) {
						if (ws.get(i).frameEnd<middlefr) {
							for (int j=i;j>=0;j--) ws.remove(j);
							break;
						}
					}				
				}
			}
			if (join)
				addAll(ws);
		}
    }
}
