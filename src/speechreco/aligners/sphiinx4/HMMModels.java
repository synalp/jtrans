/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
 */

package speechreco.aligners.sphiinx4;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import edu.cmu.sphinx.linguist.acoustic.AcousticModel;
import edu.cmu.sphinx.linguist.acoustic.UnitManager;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Sphinx3Loader;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.TiedStateAcousticModel;
import edu.cmu.sphinx.util.LogMath;

public abstract class HMMModels {
	private static AcousticModel mods = null;
	private static LogMath logMath = null;
	private static UnitManager unitManager = null;

	// le resource doit etre en dernier !
//	public static String pathHMMs[] = {"res/acmod","../jtrans/res/acmod","resource:/res/acmod"};
	public static String modelDef = "ESTER2_Train_373f_a01_s01.f04.lexV02_alg01_ter.cd_2500.mdef";
	public static String datapath = "ESTER2_Train_373f_a01_s01.f04.lexV02_alg01_ter.cd_2500.params.064g/";

	public static LogMath getLogMath() {
		if (logMath==null) logMath = new LogMath(1.0001f,true);
		return logMath;
	}

	public static UnitManager getUnitManager() {
		if (unitManager==null) unitManager = new UnitManager();
		return unitManager;
	}

	public static AcousticModel getAcousticModels() {
		if (mods==null) {
			try {
				UnitManager um = getUnitManager();
				Sphinx3Loader loader=null;
				LogMath logm = getLogMath();
				
				// TODO: get the HMM path from a configuration file
				String path = "res/acmod";
				URL modurl;
				if (path.startsWith("http")||path.startsWith("file://")) {
					modurl = new URL(path);
				} else {
					modurl = (new File(path)).toURI().toURL();
				}
//				loader = new Sphinx3Loader(modurl, modelDef, datapath, logm, um, true, false, 39, 0f, 1e-7f, 0.0001f, false);
				// ancienne version de S4
				loader = new Sphinx3Loader(modurl,modelDef,datapath,logm,um,0f,1e-7f,0.0001f,false);
				
/*				
				for (int pathidx=0;pathidx<pathHMMs.length;pathidx++) {
					String u = FileUtils.getRessource("plugins.speechreco.aligners.sphiinx4.HMMModels", pathHMMs[pathidx]);
System.out.println("acmod trying path "+pathHMMs[pathidx]+" "+u);
					if (u!=null) {
						File f = new File(u);
						if (f.exists()) {
							URL tt = f.toURI().toURL();
							System.out.println("acoustic models url "+tt);
							loader = new Sphinx3Loader(tt, modelDef, datapath, logm, um, 0, 1e-7f, 0.0001f, false);
							break;
						}
					}
				}
				*/
				
				mods = new TiedStateAcousticModel(loader, um, true);
				mods.allocate();
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return mods;
	}

	public static void main(String args[]) {
		AcousticModel mods = getAcousticModels();
	}
}
