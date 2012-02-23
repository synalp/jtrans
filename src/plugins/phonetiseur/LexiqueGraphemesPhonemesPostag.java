/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.phonetiseur;

import java.util.ArrayList;

/**
 * Cette classe permet de stocker tous les graphemes, tous les phonemes, tous
 * les doubles phonemes et tous les posTag.
 * (un double phoneme correspond a deux phonemes "obtenu" a partir d'un seul grapheme.
 * L'interet de cette classe est de ne pas avoir a manipuler les graphemes
 * et les phonemes mais des indices (utile pour les matrices).
 *
 * Note 1 : les doubles phonemes temporaires permettent de creer une future liste de double phonemes.
 * Lors de l'appel de la methode "commitDoublesPhonemesTemp()", ces doubles phonemes temporaires
 * deviennent les "vrais" doubles phonemes. Les anciens sont perdus.
 */
public class LexiqueGraphemesPhonemesPostag {

    private ArrayList<String> alGraphemes,  alPhonemes,  alDoublesPhonemes,  alDoublesPhonemesTemp,  alPosTag;

    public LexiqueGraphemesPhonemesPostag() {
        alGraphemes = new ArrayList<String>();
        alPhonemes = new ArrayList<String>();
        alDoublesPhonemes = new ArrayList<String>();
        alDoublesPhonemesTemp = new ArrayList<String>();
        alPosTag = new ArrayList<String>();
    }

    /**
     * Cette methode permet d'obtenir un indice unique pour le grapheme passe
     * en parametre
     * @param grapheme grapheme dont on souhaite obtenir un indice
     * @return l'indice
     */
    public int getIndiceFromGrapheme(String grapheme) {
        return alGraphemes.indexOf(grapheme);
    }

    /**
     * Cette methode permet d'obtenir un indice unique pour le phoneme passe
     * en parametre
     * @param phoneme phoneme dont on souhaite obtenir un indice
     * @return l'indice
     */
    public int getIndiceFromPhoneme(String phoneme) {
        return alPhonemes.indexOf(phoneme);
    }

    /**
     * Cette methode permet d'obtenir un indice unique pour le double phoneme
     * passe en parametre
     * @param p1
     * @param p2
     * @return
     */
    public int getIndiceFromDoublePhoneme(String p1, String p2) {
        boolean trouve = false;
        int i = 0;
        while ((!trouve) && (i < alDoublesPhonemes.size())) {
            trouve = (alDoublesPhonemes.get(i).equals(p1) && alDoublesPhonemes.get(i + 1).equals(p2));
            //||(alDoublesPhonemes.get(i).equals(p2) && alDoublesPhonemes.get(i + 1).equals(p1));
            if (!trouve) {
                i += 2;
            }
        }
        return trouve ? i / 2 : -1;
    }

    /**
     * Cette methode permet d'obtenir un indice unique pour le double phoneme temporaire
     * passe en parametre
     * @param p1
     * @param p2
     * @return
     */
    public int getIndiceFromDoublePhonemeTemp(String p1, String p2) {
        boolean trouve = false;
        int i = 0;
        while ((!trouve) && (i < alDoublesPhonemesTemp.size())) {
            trouve = (alDoublesPhonemesTemp.get(i).equals(p1) && alDoublesPhonemesTemp.get(i + 1).equals(p2));
            //||(alDoublesPhonemesTemp.get(i).equals(p2) && alDoublesPhonemesTemp.get(i + 1).equals(p1));
            if (!trouve) {
                i += 2;
            }
        }
        return trouve ? i / 2 : -1;
    }

    /**
     * Cette methode permet d'obtenir un indice unique pour le posTag passe
     * en parametre
     * @param posTag posTag dont on souhaite obtenir un indice
     * @return l'indice
     */
    public int getIndiceFromPosTag(String posTag) {
        return alPosTag.indexOf(posTag);
    }

    /**
     * Pour obtenir le grapheme a partir de son indice
     * @param indice
     * @return
     */
    public String getGraphemeFromIndice(int indice) {
        return alGraphemes.get(indice);
    }

    /**
     * Pour obtenir le phoneme a partir de son indice
     * @param indice
     * @return
     */
    public String getPhonemeFromIndice(int indice) {
        return alPhonemes.get(indice);
    }

    /**
     * Pour obtenir les 2 phonemes a partir de l'indice
     * @param indice
     * @return
     */
    public String[] getDoublePhonemeFromIndice(int indice) {
        String[] r = new String[2];
        r[0] = alDoublesPhonemes.get(indice * 2);
        r[1] = alDoublesPhonemes.get(indice * 2 + 1);
        return r;
    }

    /**
     * Pour obtenir le posTag a partir de son indice
     * @param indice
     * @return
     */
    public String getPosTagFromIndice(int indice) {
        return alPosTag.get(indice);
    }

    /**
     * Pour obtenir le nombre de graphemes contenu dans le lexique
     * @return
     */
    public int getNbGraphemes() {
        return alGraphemes.size();
    }

    /**
     * Pour obtenir le nombre de phonemes contenu dans le lexique
     * @return
     */
    public int getNbPhonemes() {
        return alPhonemes.size();
    }

    /**
     * Pour obtenir le nombre de posTag contenu dans le lexique
     * @return
     */
    public int getNbPosTag() {
        return alPosTag.size();
    }

    /**
     * Pour obtenir le nombre de doubles phonemes
     * @return
     */
    public int getNbDoublesPhonemes() {
        return alDoublesPhonemes.size() / 2;
    }

    /**
     * Pour obtenir le nombre de doubles phonemes
     * @return
     */
    public int getNbDoublesPhonemesTemp() {
        return alDoublesPhonemesTemp.size() / 2;
    }

    /**
     * Indique si le grapheme passe en parametre est contenu dans le lexique
     * @param grapheme
     * @return
     */
    public boolean contientGrapheme(String grapheme) {
        return alGraphemes.contains(grapheme);
    }

    /**
     * Indique si le phoneme passe en parametre est contenu dans le lexique
     * @param phoneme
     * @return
     */
    public boolean contientPhoneme(String phoneme) {
        return alPhonemes.contains(phoneme);
    }

    /**
     * Indique si le double phoneme passe en parametre est contenu dans le lexique
     * @param p1
     * @param p2
     * @return
     */
    public boolean contientDoublePhoneme(String p1, String p2) {
        return getIndiceFromDoublePhoneme(p1, p2) != -1;
    }

    /**
     * Indique si le double phoneme temporaire passe en parametre est contenu dans le lexique
     * @param p1
     * @param p2
     * @return
     */
    public boolean contientDoublePhonemeTemp(String p1, String p2) {
        return getIndiceFromDoublePhonemeTemp(p1, p2) != -1;
    }

    /**
     * Indique si le posTag passe en parametre est contenu dans le lexique
     * @param posTag
     * @return
     */
    public boolean contientPosTag(String posTag) {
        return alPosTag.contains(posTag);
    }

    /**
     * Pour ajouter un grapheme au lexique (aucun effet s'il existe deja)
     * @param grapheme
     * @return true si l'element n'etait pas present
     */
    public boolean ajouterGrapheme(String grapheme) {
        if (!contientGrapheme(grapheme)) {
            alGraphemes.add(grapheme);
            return true;
        }
        return false;
    }

    /**
     * Pour ajouter un phoneme au lexique (aucun effet s'il existe deja)
     * @param phoneme
     * @return true si l'element n'etait pas present
     */
    public boolean ajouterPhoneme(String phoneme) {
        if (!contientPhoneme(phoneme)) {
            alPhonemes.add(phoneme);
            return true;
        }
        return false;
    }

    /**
     * Pour ajouter un double phoneme au lexique (aucun effet s'il existe deja)
     * @param p1
     * @param p2
     * @return true si l'element n'etait pas present
     */
    public boolean ajouterDoublePhoneme(String p1, String p2) {
        if (!contientDoublePhoneme(p1, p2)) {
            alDoublesPhonemes.add(p1);
            alDoublesPhonemes.add(p2);
            return true;
        }
        return false;
    }

    /**
     * Pour ajouter un double phoneme temporaire au lexique (aucun effet s'il existe deja).
     * @param p1
     * @param p2
     * @return true si l'element n'etait pas present
     */
    public boolean ajouterDoublePhonemeTemp(String p1, String p2) {
        if (!contientDoublePhonemeTemp(p1, p2)) {
            alDoublesPhonemesTemp.add(p1);
            alDoublesPhonemesTemp.add(p2);
            return true;
        }
        return false;
    }

    /**
     * Pour ajouter un posTag au lexique (aucun effet s'il existe deja)
     * @param posTag
     * @return true si l'element n'etait pas present
     */
    public boolean ajouterPosTag(String posTag) {
        if (!contientPosTag(posTag)) {
            alPosTag.add(posTag);
            return true;
        }
        return false;
    }

    public String toStringPourEnregistrer() {
        StringBuffer str = new StringBuffer();

        for (int i = 0; i < alGraphemes.size(); i++) {
            str.append(alGraphemes.get(i));
            if (i != alGraphemes.size() - 1) {
                str.append("\t");
            }
        }
        str.append("\n");

        for (int i = 0; i < alPhonemes.size(); i++) {
            str.append(alPhonemes.get(i));
            if (i != alPhonemes.size() - 1) {
                str.append("\t");
            }
        }
        str.append("\n");

        for (int i = 0; i < alDoublesPhonemes.size(); i++) {
            str.append(alDoublesPhonemes.get(i));
            if (i != alDoublesPhonemes.size() - 1) {
                str.append("\t");
            }
        }
        str.append("\n");

        for (int i = 0; i < alPosTag.size(); i++) {
            str.append(alPosTag.get(i));
            if (i != alPosTag.size() - 1) {
                str.append("\t");
            }
        }

        return str.toString();
    }

    /**
     * Vide la structure de donnees
     */
    public void raz() {
        alGraphemes.clear();
        alPhonemes.clear();
        alDoublesPhonemes.clear();
        alDoublesPhonemesTemp.clear();
        alPosTag.clear();
    }

    /**
     * Les doubles phonemes dans la liste Temp deviennent les "vrais" doubles phonemes
     */
    public void commitDoublesPhonemesTemp() {
        alDoublesPhonemes = alDoublesPhonemesTemp;
        alDoublesPhonemesTemp = new ArrayList<String>();
    }

    /**
     * Cette methode compare la liste des doubles phonemes avec la liste Temp
     * est renvoie true si les 2 liste ont le meme contenu
     * @return
     */
    public boolean pasDeDifferenceDansLesDoublesPhonemes() {
        if (alDoublesPhonemes.size() != alDoublesPhonemesTemp.size()) {
            return false;
        }
        for (int i = 0; i < alDoublesPhonemes.size(); i += 2) {
            if (!contientDoublePhonemeTemp(alDoublesPhonemes.get(i), alDoublesPhonemes.get(i + 1))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Pour obtenir les graphemes sous forme de tableau de String
     */
    public String[] getArrayGraphemes() {
        return Utils.arrayListStringToArrayString(alGraphemes);
    }

    /**
     * Pour obtenir les phonemes sous forme de tableau de String
     */
    public String[] getArrayPhonemes() {
        return Utils.arrayListStringToArrayString(alPhonemes);
    }

    /**
     * Pour obtenir les doubles phonemes sous forme de tableau de String
     */
    public String[][] getArrayDoublesPhonemes() {
        String[][] r = new String[alDoublesPhonemes.size() / 2][2];
        for (int i = 0; i < r.length; i++) {
            r[i][0] = alDoublesPhonemes.get(i * 2);
            r[i][1] = alDoublesPhonemes.get(i * 2 + 1);
        }
        return r;
    }

    /**
     * Pour obtenir les posTag sous forme de tableau de String
     */
    public String[] getArrayPosTag() {
        return Utils.arrayListStringToArrayString(alPosTag);
    }

    /**
     * Pour obtenir les graphemes sous forme de String
     */
    public String getStringGraphemes(String separateur) {
        StringBuffer str = new StringBuffer();
        int taille = alGraphemes.size();

        for (int i = 0; i < taille - 1; i++) {
            str.append(alGraphemes.get(i) + separateur);
        }
        str.append(alGraphemes.get(taille - 1));
        return str.toString();
    }

    /**
     * Pour obtenir les phonemes simples sous forme de String
     */
    public String getStringPhonemesSimples(String separateur) {
        StringBuffer str = new StringBuffer();
        int taille = alPhonemes.size();

        for (int i = 0; i < taille - 1; i++) {
            str.append(alPhonemes.get(i) + separateur);
        }
        str.append(alPhonemes.get(taille - 1));
        return str.toString();
    }

    /**
     * Pour obtenir les doubles phonemes sous forme de String
     */
    public String getStringDoublesPhonemes(String separateur, String separateurDoublesPhonemes) {
        StringBuffer str = new StringBuffer();
        int taille = alDoublesPhonemes.size();

        for (int i = 0; i < taille - 3; i += 2) {
            str.append(alDoublesPhonemes.get(i) + separateurDoublesPhonemes + alDoublesPhonemes.get(i + 1) + separateur);
        }
        str.append(alDoublesPhonemes.get(taille - 2) + separateurDoublesPhonemes + alDoublesPhonemes.get(taille - 1));
        return str.toString();
    }

    public String getStringPhonemesEtDoublesPhonemes(String separateur, String separateurDoublesPhonemes) {
        return getStringPhonemesSimples(separateur) + separateur + getStringDoublesPhonemes(separateur, separateurDoublesPhonemes);
    }

    /**
     * Pour obtenir les posTag sous forme de String
     */
    public String getStringPosTag(String separateur) {
        StringBuffer str = new StringBuffer();
        int taille = alPosTag.size();

        for (int i = 0; i < taille - 1; i++) {
            str.append(alPosTag.get(i) + separateur);
        }
        str.append(alPosTag.get(taille - 1));
        return str.toString();
    }

    /**
     * Pour obtenir les 1ers phonemes des doubles phonemes sous forme de String
     */
    public String getString1erDoublesPhonemes(String separateur) {
        StringBuffer str = new StringBuffer();
        int taille = alDoublesPhonemes.size();

        for (int i = 0; i < taille; i += 2) {
            if (str.indexOf(alDoublesPhonemes.get(i) + separateur) == -1) {
                str.append(alDoublesPhonemes.get(i) + separateur);
            }
        }
        return (str.length() > 0) ? str.substring(0, str.length() - 1).toString() : str.toString();
    }

    /**
     * Pour obtenir les 2emes phonemes des doubles phonemes sous forme de String
     */
    public String getString2emeDoublesPhonemes(String separateur) {
        StringBuffer str = new StringBuffer();
        int taille = alDoublesPhonemes.size();

        for (int i = 1; i < taille - 1; i += 2) {
            if (str.indexOf(alDoublesPhonemes.get(i) + separateur) == -1) {
                str.append(alDoublesPhonemes.get(i) + separateur);
            }
        }
        return (str.length() > 0) ? str.substring(0, str.length() - 1).toString() : str.toString();
    }
}
