/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package jtrans.speechreco.frontEnd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

public class HTKFile implements FrontEnd {
	public int onlyNframes = -1;
	public ByteOrder order = ByteOrder.LITTLE_ENDIAN;
//	public ByteOrder order = ByteOrder.BIG_ENDIAN;
	public int curt = -1;
	public int nFrames;
	
	private String nom = null;
	private ByteBuffer bf=null;
	private FileChannel fich=null;
	
	int ncoefs;
	float[] obs;
	
	public int getNcoefs() {return ncoefs;}
	
	public void close() {
		if (fich!=null) {
			try {
				fich.close();
				fich=null;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void save(String nom, FrontEnd obs, short parmKind, boolean isLittleEndian) {
		ByteOrder order;
		if (isLittleEndian)
			order = ByteOrder.LITTLE_ENDIAN;
		else
			order = ByteOrder.BIG_ENDIAN;
		int nt = 1;
		ArrayList<float[]> allobs = new ArrayList<float[]>();
		float[] v = obs.getOneVector();
		if (v==null) {
			System.err.println("warning: no obs to save !");
			return;
		} else {
			for (;;) {
				allobs.add(v);
				v=obs.getOneVector();
				if (v==null) break;
				nt++;
			}
		}
		try {
			FileOutputStream ftmp = new FileOutputStream(nom);
			{
				File ff = new File(nom);
				System.err.println("save in " + ff.getAbsolutePath());
			}
			FileChannel fichw = ftmp.getChannel();
			int sp = 100000; // sampling period
			int ncoefs = allobs.get(0).length;
			short s = (short) (ncoefs * 4);
			saveHeader(fichw, nt, sp, s, parmKind, order);

			// on enregistre toutes les obs
			ByteBuffer bf = ByteBuffer.allocate(ncoefs*4);
			bf.order(order);
			for (int i=0;i<allobs.size();i++) {
				v = allobs.get(i);
				saveObs(fichw, v, bf);
			}
			fichw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private static void saveHeader(FileChannel fw, int nFrames, int sampPeriod, short vecSize, short parmKind, ByteOrder order) {
		ByteBuffer bf = ByteBuffer.allocate(12);
		bf.order(order);
		bf.clear();
		bf.putInt(nFrames);
		bf.putInt(sampPeriod);
		bf.putShort(vecSize);
		bf.putShort(parmKind);
		bf.rewind();
		try {
			fw.write(bf);
		} catch (IOException e) {
			e.printStackTrace();
		}
		bf=null;
	}
	private static void saveObs(FileChannel fw, float [] o, ByteBuffer bf) {
		bf.clear();
		/*
		 * ne pas utiliser o.length garantit que on a le meme nb de coefs que declares dans le header
		 * et permet d'avoir un vecteur d'obs plus grand.
		 */
		for (int i=0;i<o.length;i++) {
		    bf.putFloat(o[i]);
        }
		bf.rewind();
		try {
			fw.write(bf);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public float[] getOneVector() {
		if (onlyNframes > 0 && curt >= onlyNframes-1) return null;
		try {
			bf.clear();
			if (fich==null) return null;
			int nread = fich.read(bf);
			if (nread<ncoefs) {
				fich.close();
				fich=null;
				return null;
			}
			for (int i=0;i<ncoefs;i++) {
				obs[i] = bf.getFloat(i*4);
			}
			curt++;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return obs;
	}
	
	public HTKFile(String n, boolean isLittleEndian) {
		if (isLittleEndian)
			order = ByteOrder.LITTLE_ENDIAN;
		else
			order = ByteOrder.BIG_ENDIAN;
		try {
			nom = ""+n;
			bf = ByteBuffer.allocate(12);
			bf.order(order);
			FileInputStream ftmp = new FileInputStream(nom);
			fich = ftmp.getChannel();
			bf.clear(); fich.read(bf);
			nFrames = bf.getInt(0);
			int sampPeriod = bf.getInt(4);
			short ncoefs = ((short)((int)bf.getShort(8) / 4));
			this.ncoefs=ncoefs;
			obs = new float[this.ncoefs];
			short parmKind = bf.getShort(10);
			bf = ByteBuffer.allocate(4*ncoefs);
			bf.order(order);
			curt=-1;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
