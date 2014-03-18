/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package fr.loria.synalp.jtrans.phonetiseur;

import fr.loria.synalp.jtrans.utils.PronunciationsLexicon;

public class PhonetiseurFacade {

    private Phonetiseur phonetiseur;
    private String cheminFichierMatriceProbaEtLexique, cheminRepArff, cheminRepModel;

    /**
     * Constructeur...
     * Les 3 chemins demandes en parametre seront utilises, suivant les methodes appelees, pour creer les fichiers necessaires ou les charger
     * @param cheminFichierMatriceProbaEtLexique chemin vers le fichier contenant (ou qui contiendra) le lexique et la matrice de proba
     * @param cheminRepArff chemin vers le repertoire contenant (ou qui contiendra) les fichiers ARFF crees
     * @param cheminRepModel chemin vers le repertoire contenant (ou qui contiendra) la sauvegarde des classifieurs
     */
    public PhonetiseurFacade(String cheminFichierMatriceProbaEtLexique, String cheminRepArff, String cheminRepModel) {
        if (cheminRepArff.charAt(cheminRepArff.length() - 1) != System.getProperty("file.separator").charAt(0)) {
            // S'il manque un separateur a la fin du chemin passe en parametre : on le rajoute
//            cheminRepArff += System.getProperty("file.separator");
        }
        if (cheminRepModel.charAt(cheminRepModel.length() - 1) != System.getProperty("file.separator").charAt(0)) {
            // S'il manque un separateur a la fin du chemin passe en parametre : on le rajoute
//            cheminRepModel += System.getProperty("file.separator");
        }


        this.cheminFichierMatriceProbaEtLexique = cheminFichierMatriceProbaEtLexique;
        this.cheminRepArff = cheminRepArff;
        this.cheminRepModel = cheminRepModel;
    }

    /**
     * Permet de lancer l'apprentissage
     * @param pronuncitationsLexicon une instance d'une des classes de JHTK permettant de lire le dictionnaire
     * @param cheminFichierMotsAApprendre chemin vers le fichier contenant la liste des mots que l'on ira chercher dans le dictionnaire pour l'apprentissage
     * @throws java.lang.Exception
     */
    public void lancerApprentissage(PronunciationsLexicon pronuncitationsLexicon, String cheminFichierMotsAApprendre) throws Exception {
        phonetiseur = new Phonetiseur(pronuncitationsLexicon, cheminFichierMotsAApprendre);
        phonetiseur.genererMatricesProbabilites();
        phonetiseur.enregistrerMatriceProbaEtLexique(cheminFichierMatriceProbaEtLexique);
        phonetiseur.genererFichiersVecteursARFF(cheminRepArff);
        phonetiseur.lancerApprentissage(cheminRepArff);
        phonetiseur.sauvegarderClassifieurs(cheminRepModel);
    }

    /**
     * Permet charger les classifieurs
     * @throws java.lang.Exception
     */
    public void chargerClassifieurs() throws Exception {
        phonetiseur = new Phonetiseur();
        phonetiseur.chargerMatriceProbaEtLexique(cheminFichierMatriceProbaEtLexique);
        phonetiseur.chargerClassifieurs(cheminRepArff, cheminRepModel);
    }

    /**
     * Phonetise un mot
     * @param mot le mot a phonetiser
     * @param posTag le posTag (il est possible de passer null a ce parametre si on ne souhaite pas le specifier)
     * @return le mot phonetise (les phonemes sont separes par des espaces)
	 * or an empty string if phonetization failed
     * @throws java.lang.Exception
     */
    public String phonetiser(String mot, String posTag) throws Exception {
        mot = mot.toLowerCase();
        if (phonetiseur == null) {
            return null;
        } else {
            String[] t;
            StringBuffer s = new StringBuffer();
            t = phonetiseur.phonetiser(Utils.stringToArrayString(mot.replace('\'', Configuration.CHAR_DE_REMPLACEMENT_APPOSTROPHE)), posTag);
			if (0 == t.length) {
				return "";
			}
            for (int i = 0; i < t.length - 1; i++) {
                s.append(t[i] + " ");
            }
            return s + t[t.length - 1];
        }
    }
}


