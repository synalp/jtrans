/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

/*
Copyright Christophe Cerisara, Josselin Pierre (1er septembre 2008)

cerisara@loria.fr

Ce logiciel est un programme informatique servant � aligner un
corpus de parole avec sa transcription textuelle.

Ce logiciel est r�gi par la licence CeCILL-C soumise au droit fran�ais et
respectant les principes de diffusion des logiciels libres. Vous pouvez
utiliser, modifier et/ou redistribuer ce programme sous les conditions
de la licence CeCILL-C telle que diffus�e par le CEA, le CNRS et l'INRIA 
sur le site "http://www.cecill.info".

En contrepartie de l'accessibilit� au code source et des droits de copie,
de modification et de redistribution accord�s par cette licence, il n'est
offert aux utilisateurs qu'une garantie limit�e.  Pour les m�mes raisons,
seule une responsabilit� restreinte p�se sur l'auteur du programme,  le
titulaire des droits patrimoniaux et les conc�dants successifs.

A cet �gard  l'attention de l'utilisateur est attir�e sur les risques
associ�s au chargement,  � l'utilisation,  � la modification et/ou au
d�veloppement et � la reproduction du logiciel par l'utilisateur �tant 
donn� sa sp�cificit� de logiciel libre, qui peut le rendre complexe � 
manipuler et qui le r�serve donc � des d�veloppeurs et des professionnels
avertis poss�dant  des  connaissances  informatiques approfondies.  Les
utilisateurs sont donc invit�s � charger  et  tester  l'ad�quation  du
logiciel � leurs besoins dans des conditions permettant d'assurer la
s�curit� de leurs syst�mes et ou de leurs donn�es et, plus g�n�ralement, 
� l'utiliser et l'exploiter dans les m�mes conditions de s�curit�. 

Le fait que vous puissiez acc�der � cet en-t�te signifie que vous avez 
pris connaissance de la licence CeCILL-C, et que vous en avez accept� les
termes.
*/

package plugins.text.elements;

import java.io.Serializable;

/** Classe regroupant les informations sur les locuteurs */
public class Locuteur_Info implements Serializable {
	//id="spk1" name="Herv� Gardette" check="yes" type="male" dialect="native" accent="" scope="global"
	
	public static final long serialVersionUID = 1;
	
	//--------- Champs priv�s ------------
	private byte id;
	private String name;
	private boolean check;
	//True si c'est une femme, false sinon
	private boolean type;
	private String dialect;
	private String accent;
	private String scope;
	
	
	
	//--------------- Constructeur -----------
	public Locuteur_Info(byte id, String name, boolean check, boolean type, String dialect, String accent, String scope) {
		super();
		this.id = id;
		this.name = name;
		this.check = check;
		this.type = type;
		this.dialect = dialect;
		this.accent = accent;
		this.scope = scope;
	}

	public Locuteur_Info(byte id, String name) {
		super();
		this.id = id;
		this.name = name;
		this.check = false;
		this.type = false;
		this.dialect = "";
		this.accent = "";
		this.scope = "";
	}

	//--------- Getters & Setters --------
	public String getAccent() {
		return accent;
	}

	public void setAccent(String accent) {
		this.accent = accent;
	}

	public boolean isCheck() {
		return check;
	}

	public void setCheck(boolean check) {
		this.check = check;
	}

	public String getDialect() {
		return dialect;
	}

	public void setDialect(String dialect) {
		this.dialect = dialect;
	}

	public byte getId() {
		return id;
	}

	public void setId(byte id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getScope() {
		return scope;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

	public boolean isType() {
		return type;
	}

	public void setType(boolean type) {
		this.type = type;
	}
	
}//class Locuteur_Info
