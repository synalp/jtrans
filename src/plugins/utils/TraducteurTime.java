/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

/*
Copyright Christophe Cerisara, Josselin Pierre (1er septembre 2008)

cerisara@loria.fr

Ce logiciel est un programme informatique servant à aligner un
corpus de parole avec sa transcription textuelle.

Ce logiciel est régi par la licence CeCILL-C soumise au droit français et
respectant les principes de diffusion des logiciels libres. Vous pouvez
utiliser, modifier et/ou redistribuer ce programme sous les conditions
de la licence CeCILL-C telle que diffusée par le CEA, le CNRS et l'INRIA 
sur le site "http://www.cecill.info".

En contrepartie de l'accessibilité au code source et des droits de copie,
de modification et de redistribution accordés par cette licence, il n'est
offert aux utilisateurs qu'une garantie limitée.  Pour les mêmes raisons,
seule une responsabilité restreinte pèse sur l'auteur du programme,  le
titulaire des droits patrimoniaux et les concédants successifs.

A cet égard  l'attention de l'utilisateur est attirée sur les risques
associés au chargement,  à l'utilisation,  à la modification et/ou au
développement et à la reproduction du logiciel par l'utilisateur étant 
donné sa spécificité de logiciel libre, qui peut le rendre complexe à 
manipuler et qui le réserve donc à des développeurs et des professionnels
avertis possédant  des  connaissances  informatiques approfondies.  Les
utilisateurs sont donc invités à charger  et  tester  l'adéquation  du
logiciel à leurs besoins dans des conditions permettant d'assurer la
sécurité de leurs systèmes et ou de leurs données et, plus généralement, 
à l'utiliser et l'exploiter dans les mêmes conditions de sécurité. 

Le fait que vous puissiez accéder à cet en-tête signifie que vous avez 
pris connaissance de la licence CeCILL-C, et que vous en avez accepté les
termes.
*/

package plugins.utils;

/**
 * Classe regrouppant les méthodes de traduction du temps en string
 *
 */
public abstract class TraducteurTime {

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
	
	
	
	
}//TraducteurTime
