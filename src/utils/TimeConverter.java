/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

/*
Copyright Christophe Cerisara, Josselin Pierre (1er septembre 2008)

cerisara@loria.fr

Ce logiciel est un programme informatique servant e aligner un
corpus de parole avec sa transcription textuelle.

Ce logiciel est regi par la licence CeCILL-C soumise au droit franeais et
respectant les principes de diffusion des logiciels libres. Vous pouvez
utiliser, modifier et/ou redistribuer ce programme sous les conditions
de la licence CeCILL-C telle que diffusee par le CEA, le CNRS et l'INRIA 
sur le site "http://www.cecill.info".

En contrepartie de l'accessibilite au code source et des droits de copie,
de modification et de redistribution accordes par cette licence, il n'est
offert aux utilisateurs qu'une garantie limitee.  Pour les memes raisons,
seule une responsabilite restreinte pese sur l'auteur du programme,  le
titulaire des droits patrimoniaux et les concedants successifs.

A cet egard  l'attention de l'utilisateur est attiree sur les risques
associes au chargement,  e l'utilisation,  e la modification et/ou au
developpement et e la reproduction du logiciel par l'utilisateur etant 
donne sa specificite de logiciel libre, qui peut le rendre complexe e 
manipuler et qui le reserve donc e des developpeurs et des professionnels
avertis possedant  des  connaissances  informatiques approfondies.  Les
utilisateurs sont donc invites e charger  et  tester  l'adequation  du
logiciel e leurs besoins dans des conditions permettant d'assurer la
securite de leurs systemes et ou de leurs donnees et, plus generalement, 
e l'utiliser et l'exploiter dans les memes conditions de securite. 

Le fait que vous puissiez acceder e cet en-tete signifie que vous avez 
pris connaissance de la licence CeCILL-C, et que vous en avez accepte les
termes.
*/

package utils;

public abstract class TimeConverter {

	public static String HEURES_STR = "h";
	public static String MINUTES_STR = "min";
	public static String SECONDES_STR = "s";
	public static String MILLISECONDES_STR = "ms";
	
	public static String getTimeHMinSFromSeconds(double time){
		StringBuilder timeStr = new StringBuilder();
		int temp;
		
		temp = (int)(time/3600.0);
		if(temp > 0){
			timeStr.append(temp);
			timeStr.append(HEURES_STR);
			time = time - temp*3600.0;
		}
		
		temp = (int)(time/60.0);
		if(temp > 0){
			timeStr.append(temp);
			timeStr.append(MINUTES_STR);
			time = time - temp*60.0;
		}
		
		temp = (int)time;
		timeStr.append(temp);
		timeStr.append(SECONDES_STR);


		
		return timeStr.toString();
	}// getTimeMinSMSFromSeconds(double time)
	
	
	public static String getTimeMinSMSFromSeconds(double time){
		StringBuilder timeStr = new StringBuilder();
		int temp;
		
		temp = (int)(time/60.0);
		if(temp > 0){
			timeStr.append(temp);
			timeStr.append(MINUTES_STR);
			time = time - temp*60.0;
		}
		
		temp = (int)time;
		if(temp > 0){
			time = time - temp;
			timeStr.append(temp);
			timeStr.append(SECONDES_STR);
		}
		
		temp = (int)(time*1000.0);
		timeStr.append(temp);
		timeStr.append(MILLISECONDES_STR);
		
		
		return timeStr.toString();
	}// getTimeMinSMSFromSeconds(double time)
	
	public static String getTimeMinMSFromSecondes(double time){
		StringBuilder timeStr = new StringBuilder();
		int temp;
		
		temp = (int)(time/60.0);
		if(temp > 0){
			timeStr.append(temp);
			timeStr.append(MINUTES_STR);
			time = time - temp*60.0;
		}
		
		temp = (int)time;
		timeStr.append(temp);
		timeStr.append(SECONDES_STR);
	
		
		return timeStr.toString();
	}//getTimeMinMSFromSecondes(double time)


	public static float frame2sec(int fr) {
		return (float)frame2millisec(fr)/1000f;
	}

	public static long frame2millisec(int fr) {
		// window = 25ms, donc milieu = 12ms
		return fr*10+12;
	}

	public static int millisec2frame(long ms) {
		return (int)((ms-12)/10);
	}

	public static int second2frame(float sec) {
		return (int)(sec*100f);
	}

	//=======================================================================
	// TIME CONVERSION FUNCTIONS FROM OldAlignment

	public static long frame2sample(int frame) {
		long f=frame;
		f*=160;
		f+=205; // moitie d'une window
		return f;
	}

	public static int sample2frame(long sample) {
		sample-=205;
		if (sample<0) return 0;
		return (int)(sample/160);
	}

	public static float sample2second(long sample) {
		return (float)sample/16000f;
	}

	public static int second2sample(float sec) {
		return (int)(sec*16000f);
	}
}
