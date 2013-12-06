/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package jtrans.phonetiseurNomPropre;

public class Configuration {

    public static final double EPSILON_ALIGNEMENT_GRAPHEME_PHONEME = 0.000000001;
    public static final String REPERTOIRE_CIBLE_FICHIERS_DONNEES = System.getProperty("user.dir") + System.getProperty("file.separator");
    public static final String REPERTOIRE_FICHIERS_POUR_LES_TESTS_PHONETISEUR = System.getProperty("user.dir") + System.getProperty("file.separator") + "jtrans/test" + System.getProperty("file.separator") + "jtrans/plugins" + System.getProperty("file.separator") + "phonetiseur" + System.getProperty("file.separator");
    //*** La matrice de probas ***//
    public static final double SEUIL_SCORE_ALIGNEMENT_POUR_TENIR_COMPTE_CALCUL_MATRICE_PROBA = .01;
    public static final double PROBA_CREER_DOUBLE_PHONEME = 0.000001;
    public static final double PROBA_PHONEME_VIDE_INIT = 0.5;
    public static final String GRAPHEME_DEBUT_DE_MOT_VECTEUR = "$$$$";
    public static final String GRAPHEME_FIN_DE_MOT_VECTEUR = "££££";
    public static final int NB_DECALAGES_HEURISTIQUE_DAELEMANS = 3;
    public static final double EPSILON_CONVERGENCE_MATRICES_PROBAS = 0.001;
    public static final int NB_ITERATIONS_MAX_CALCUL_MATRICES_PROBAS = 200;
    //*** Remplacement de caracteres ***//
    public static final char CHAR_DE_REMPLACEMENT_APPOSTROPHE = '`';
    public static final String STRING_DE_REMPLACEMENT_PHONEME_VIDE = "_"; // cas d'un grapheme non aligne
    public static final String STRING_DE_REMPLACEMENT_GRAPHEME_VIDE = "_"; // cas d'un double phoneme (concerne le deuxieme phoneme qui n'est aligne avec rien)
    //*** Les fichiers ARFF ***//
    public static final String NOM_FICHIER_ARFF_SIMPLE_OU_DOUBLE_PHONEME = "vecteursSimpleOuDoublePhoneme";
    public static final String NOM_FICHIER_ARFF_SIMPLE_PHONEME = "vecteursSimplePhoneme";
    public static final String NOM_FICHIER_ARFF_1er_DOUBLE_PHONEME = "vecteursDoublePhoneme1er";
    public static final String NOM_FICHIER_ARFF_2eme_DOUBLE_PHONEME = "vecteursDoublePhoneme2eme";
    public static final String VALEUR_SORTIE_VECTEUR_SIMPLE_PHONEME = "simple";
    public static final String VALEUR_SORTIE_VECTEUR_DOUBLE_PHONEME = "double";
    //*** Les models (fichiers de sauvegarde des classifieurs) ***//
    public static final String NOM_FICHIER_MODEL_SIMPLE_OU_DOUBLE_PHONEME = "vecteursSimpleOuDoublePhoneme";
    public static final String NOM_FICHIER_MODEL_SIMPLE_PHONEME = "vecteursSimplePhoneme";
    public static final String NOM_FICHIER_MODEL_1er_DOUBLE_PHONEME = "vecteursDoublePhoneme1er";
    public static final String NOM_FICHIER_MODEL_2eme_DOUBLE_PHONEME = "vecteursDoublePhoneme2eme";
}
