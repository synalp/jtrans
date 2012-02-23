package main;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat.Encoding;

import plugins.applis.SimpleAligneur.Aligneur;
import plugins.speechreco.aligners.sphiinx4.AlignementEtat;
import plugins.speechreco.aligners.sphiinx4.AutoAligner;
import plugins.speechreco.aligners.sphiinx4.S4AlignOrder;
import plugins.speechreco.aligners.sphiinx4.S4ForceAlignBlocViterbi;
import plugins.speechreco.grammaire.Grammatiseur;
import plugins.text.ListeElement;
import plugins.text.PonctParser;
import plugins.text.TextSegments;
import plugins.text.elements.Element_Mot;
import plugins.utils.FileUtils;

/**
 * 
 * TODO: methode pour tuer l'autoaligner proprement
 * 
 * @author cerisara
 *
 */
public class JTrans {

	public static AlignementEtat[] forceAlign(String[] mots, String wavfile) throws Exception {
		if (jtransnogui==null) {
			jtransnogui = new JTrans();
		}
		return jtransnogui.batchForceAlign(mots, wavfile);
	}

	public static void runGUI() {
		new Aligneur();
	}

	public static float frame2sec(int fr) {
		long ms = frame2millisec(fr);
		float sec = (float)ms/1000f;
		return sec;
	}
	public static long frame2millisec(int fr) {
		// window = 25ms, donc milieu = 12ms
		return fr*10+12;
	}
	public static int millisec2frame(long ms) {
		return (int)((ms-12)/10);
	}

	public static void savePraat(String textgridout, AlignementEtat al) {
		try {
			PrintWriter f = FileUtils.writeFileISO(textgridout);
			f.println("File type = \"ooTextFile\"");
			f.println("Object class = \"TextGrid\"");
			f.println();
			f.println("xmin = 0");
			f.println("xmax = "+frame2sec(al.getSegmentEndFrame(al.getNbSegments()-1)));
			f.println("tiers? <exists>");
			f.println("size = 1");
			f.println("item []:");
			f.println("\titem [1]:");
			f.println("\t\tclass = \"IntervalTier\"");
			f.println("\t\tname = \"mots\"");
			f.println("\t\txmin = 0");
			f.println("\t\txmax = "+frame2sec(al.getSegmentEndFrame(al.getNbSegments()-1)));
			f.println("\t\tintervals: size = "+al.getNbSegments());
			for (int i=0;i<al.getNbSegments();i++) {
				f.println("\t\tintervals ["+(i+1)+"]:");
				f.println("\t\t\txmin = "+frame2sec(al.getSegmentDebFrame(i)));
				f.println("\t\t\txmax = "+frame2sec(al.getSegmentEndFrame(i)));
				f.println("\t\t\ttext = \""+al.getSegmentLabel(i)+"\"");
			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Il vaut mieux utiliser une tokenization exterieure, car celle realisee ici est basique !
	 */
	public static AlignementEtat[] forceAlign(String phrase, String wavfile) throws Exception {
		String[] mots = phrase.split(" ");
		return forceAlign(mots, wavfile);
	}

	public static void align(String wavfile, String txtfile) {
		// conversion du fichier WAV si besoin
		try {
			AudioFormat fileAudioFormat = AudioSystem.getAudioFileFormat(new File(wavfile)).getFormat();
			if (fileAudioFormat.getSampleRate()==16000 && fileAudioFormat.getChannels()==1 && fileAudioFormat.getEncoding()==Encoding.PCM_SIGNED && fileAudioFormat.getSampleSizeInBits()==16) {
				System.out.println("wave file format OK : pas besoin de conversion !");
			} else {
				System.out.println("WARNING: wav format not in PMC signed 16kHz 16 bits mono ! Converting...");
				AudioInputStream sonFormatOrigine = AudioSystem.getAudioInputStream(new File(wavfile));
				AudioInputStream son = AudioSystem.getAudioInputStream(new AudioFormat(16000,16,1,true,false),sonFormatOrigine);
				wavfile = "tmptcwav.wav";
				AudioSystem.write(son, Type.WAVE, new File(wavfile));
			}
		} catch (UnsupportedAudioFileException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		PonctParser parser = new PonctParser(null);
		ListeElement listeElts = parser.parseimmutable(txtfile);
		List<Element_Mot> lmots = listeElts.getMots();
		String[] mots = new String[lmots.size()];
		for (int i=0;i<lmots.size();i++) {
			mots[i] = lmots.get(i).getWordString();
			System.out.println("debug mot "+i+" ["+mots[i]+"]");
		}
		s4aligner = S4ForceAlignBlocViterbi.getS4Aligner(wavfile);
		s4aligner.setMots(mots);

		S4AlignOrder order = new S4AlignOrder(0, 0, mots.length, -1);
		AlignementEtat alignement=null;
		try {
			s4aligner.input2process.put(order);
			synchronized (order) {
				order.wait();
			}
			alignement = order.alignWords;
			if (alignement==null) {
				System.err.println("WARNING: qualité audio insuffisante !");
			}
		} catch (Exception e) {e.printStackTrace();}

		// on peut l'appeler plusieurs fois ensuite
		if (alignement!=null) {
			savePraat("out.TextGrid", alignement);
			System.err.println("Alignment saved in Praat format in out.TextGrid");
			Aligneur.saveProject(listeElts, "out.jtr", txtfile, wavfile, order.alignWords, order.alignPhones, "");
			System.err.println("Alignment saved in JTrans format in out.jtr");
		}
	}

	static S4ForceAlignBlocViterbi s4aligner = null;

	public static void terminate() {
		if (s4aligner!=null)
			try {
				s4aligner.input2process.put(S4AlignOrder.terminationOrder);
				synchronized (S4AlignOrder.terminationOrder) {
					S4AlignOrder.terminationOrder.wait();
				}
				System.exit(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	}

	public static void main(String args[]) throws Exception {
		boolean isiso=false;
		String txtfich = null, wavfich=null;
		for (int i=0;i<args.length;i++) {
			if (args[i].equals("-txt")) {
				txtfich = args[++i];
			}
			if (args[i].equals("-wav")) {
				wavfich = args[++i];
			}
			if (args[i].equals("-iso")) isiso=true;
		}
		if (txtfich==null||wavfich==null) {
			System.out.println("usage: java main.JTrans [-iso] -txt <textfileutf8> -wav <wavfile>");
			return;
		}

		if (isiso) TextSegments.isISO=true;
		align(wavfich, txtfich);
		terminate();

		/*
		String txtfich = args[0];
		String wavfich = args[1];
		BufferedReader f = new BufferedReader(new FileReader(txtfich));
		String s = f.readLine();
		f.close();
		AlignementEtat al = forceAlign(s, wavfich)[0];
		System.out.println(al);
		 */
	}

	// =======================================
	private static JTrans jtransnogui = null;

	private Grammatiseur grammatiseur;

	private void initGrammaire() {
		System.err.println("init grammar");
		try {
			ObjectInputStream f = new ObjectInputStream(getClass()
					.getResourceAsStream("/ressources/grammatiseur"));
			grammatiseur = (Grammatiseur) f.readObject();
			f.close();
			System.out.println("grammatiseur loaded");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			// cela arrive lorsqu'on charge un grammatiseur qui a ete enregistre
			// avec une autre version de java
			// Il faut regenrer le grammatiseur binaire !
			System.err.println("invalid class: reload lexicon");
			grammatiseur = Grammatiseur.getGrammatiseur();
			System.err.println("save grammatiseur to grammatiseur; you should copy it into ressources/ and rebuild the jar !");
			grammatiseur.serialize();
		}
		grammatiseur.initPhonetiseur();
	}// initGrammaire

	private JTrans() {
		jtransnogui=this;
		//        initGrammaire();
	}


	private AlignementEtat[] batchForceAlign(String[] mots, String wavfile) throws Exception {
		/*
        // determine la taille du buffer MFCC
		int nframes=0;
		{
			TemporalSigFromWavFile wavForMFCC = new TemporalSigFromWavFile();
			wavForMFCC.openWavFile(new File(wavfile));
	        MFCC mfcc = new MFCC(wavForMFCC);
			RoundBufferFrontEnd mfccbuf = new RoundBufferFrontEnd(null, 10000, mfcc.getNcoefs());
	        mfccbuf.setSource(mfcc);
	        while (mfccbuf.getOneVector()!=null) nframes++;
	        wavForMFCC.close();
		}

		// Les MFCC
		TemporalSigFromWavFile wavForMFCC = new TemporalSigFromWavFile();
		wavForMFCC.openWavFile(new File(wavfile));
        MFCC mfcc = new MFCC(wavForMFCC);
		RoundBufferFrontEnd mfccbuf = new RoundBufferFrontEnd(null, 10000, mfcc.getNcoefs());
        mfccbuf.setSource(mfcc);

        // La grammaire
		StringBuilder gramstring = new StringBuilder("[ sil ] ");
        for (int w=0;w<mots.length;w++) {
    		String rule = grammatiseur.getGrammar(mots[w]);
    		// on recupere toujours un silence optionnel au debut, que je supprime:
    		rule = rule.substring(4).trim();
    		if (rule.charAt(0)=='#') {
    			// le phonetiseur n'a pas marche: on suppose que c'est un bruit
    			rule = "xx "+rule;
    		}
    		gramstring.append(rule+" ");
        }
		 */

		AlignementEtat alignementMots = new AlignementEtat();
		AlignementEtat alignementPhones = new AlignementEtat();
		//		alignement.setSegments(mots);

		AutoAligner.batch=true;
		autoaligner = AutoAligner.getAutoAligner(wavfile, mots, null, alignementMots,alignementPhones);

		synchronized (alignementMots) {
			alignementMots.wait();
		}

		// attend la fin de l'alignement


		// et enfin: l'aligneur !
		//        SimpleForceAlign batch = new SimpleForceAlign(hmms, mfccbuf, null, null);
		//        batch.pruning = 3000;
		//        Alignement al = batch.align(gramstring.toString(), nframes);
		//        return al;

		AlignementEtat[] res = {alignementMots,alignementPhones};
		return res;
	}
	public static AutoAligner autoaligner=null;

}
