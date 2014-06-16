/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
 */

package fr.loria.synalp.jtrans.speechreco.s4;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import edu.cmu.sphinx.linguist.acoustic.AcousticModel;
import edu.cmu.sphinx.linguist.acoustic.UnitManager;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Sphinx3Loader;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.TiedStateAcousticModel;
import edu.cmu.sphinx.util.LogMath;

import static fr.loria.synalp.jtrans.utils.Paths.RES_DIR;

public abstract class HMMModels {
	private static AcousticModel mods = null;
	private static LogMath logMath = null;
	private static UnitManager unitManager = null;

	public static String modelDef = "ESTER2_Train_373f_a01_s01.f04.lexV02_alg01_ter.cd_2500.mdef";
	public static String datapath = "ESTER2_Train_373f_a01_s01.f04.lexV02_alg01_ter.cd_2500.params.064g/";

	public static LogMath getLogMath() {
		// Sphinx's default log base = 1.0001 (cf. LogMath.java)
		if (logMath==null) logMath = new LogMath(2,true);
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
				URL modurl = (new File(RES_DIR, "acmod")).toURI().toURL();
//				loader = new Sphinx3Loader(modurl, modelDef, datapath, logm, um, true, false, 39, 0f, 1e-7f, 0.0001f, false);
				// ancienne version de S4
				loader = new Sphinx3Loader(modurl,modelDef,datapath,logm,um,0f,1e-7f,0.0001f,false);
				
				mods = new TiedStateAcousticModel(loader, um, true);
				mods.allocate();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return mods;
	}
}
