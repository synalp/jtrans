/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.phonetiseurNomPropre;

import java.util.*;
import java.util.ArrayList;

/**
 * Cette classe represente un alignement entre les graphemes et les phonemes.
 * Son interet est uniquement de faciliter l'affichage et le stockage des deux tableaux de String.
 * Exemple :
 * h   e   x       a
 *     E   g   z   a
 *
 * h est aligne avec le phoneme vide
 * g et z est un double phoneme => g est aligne avec x et z avec le grapheme vide
 */
public class AlignementGraphemesPhonemes {

    private String[] tGraphemesAlignes;
    private String[] tPhonemesAlignes;
    private double score;

    /**
     * Constructeur : les deux tableaux doivent etre "alignes", c'est-a-dire que
     * tGraphemesAlignes[i] est aligne avec tPhonemesAlignes[i].
     * @param tGraphemesAlignes le tableau des graphemes alignes
     * @param tPhonemesAlignes le tableau des phonemes alignes
     */
    public AlignementGraphemesPhonemes(String[] tGraphemesAlignes, String[] tPhonemesAlignes) {
        if (tGraphemesAlignes.length != tPhonemesAlignes.length) {
            System.err.println("Erreur classe AlignementGraphemesPhonemes : les 2 tableaux n'ont pas la meme taille...");
        }
        this.tGraphemesAlignes = tGraphemesAlignes;
        this.tPhonemesAlignes = tPhonemesAlignes;
        score = -1;
    }

    /**
     * Constructeur : les deux ArrayList doivent etre "alignes", c'est-a-dire que
     * alGraphemesAlignes[i] est aligne avec alPhonemesAlignes[i].
     * @param alGraphemesAlignes les graphemes alignes
     * @param alPhonemesAlignes les phonemes alignes
     */
    public AlignementGraphemesPhonemes(ArrayList<String> alGraphemesAlignes, ArrayList<String> alPhonemesAlignes) {
        if (alGraphemesAlignes.size() != alPhonemesAlignes.size()) {
            System.err.println("Erreur classe AlignementGraphemesPhonemes : les 2 ArrayList n'ont pas la meme taille...");
        }

        tGraphemesAlignes = new String[alGraphemesAlignes.size()];
        tPhonemesAlignes = new String[alPhonemesAlignes.size()];
        for (int i = 0; i < alGraphemesAlignes.size(); i++) {
            // Note : alGraphemesAlignesMeilleurSolution et alPhonemesAlignesMeilleurSolution
            // doivent avoir la meme taille
            tGraphemesAlignes[i] = alGraphemesAlignes.get(i);
            tPhonemesAlignes[i] = alPhonemesAlignes.get(i);
        }
        score = -1;
    }

    /**
     * Constructeur : les deux LinkedList doivent etre "alignes", c'est-a-dire que
     * lGraphemesAlignes[i] est aligne avec lPhonemesAlignes[i].
     * @param lGraphemesAlignes les graphemes alignes
     * @param lPhonemesAlignes les phonemes alignes
     */
    public AlignementGraphemesPhonemes(LinkedList<String> lGraphemesAlignes, LinkedList<String> lPhonemesAlignes) {
        if (lGraphemesAlignes.size() != lPhonemesAlignes.size()) {
            System.err.println("Erreur classe AlignementGraphemesPhonemes : les 2 LinkedList n'ont pas la meme taille...");
        }

        tGraphemesAlignes = new String[lGraphemesAlignes.size()];
        tPhonemesAlignes = new String[lPhonemesAlignes.size()];
        for (int i = 0; i < lGraphemesAlignes.size(); i++) {
            // Note : alGraphemesAlignesMeilleurSolution et alPhonemesAlignesMeilleurSolution
            // doivent avoir la meme taille
            tGraphemesAlignes[i] = lGraphemesAlignes.get(i);
            tPhonemesAlignes[i] = lPhonemesAlignes.get(i);
        }
        score = -1;
    }

    /**
     * Pout obtenir la taille de l'aligment
     * @return
     */
    public int getTailleAlignement() {
        return Math.max(tGraphemesAlignes.length, tPhonemesAlignes.length);
    }

    /**
     * Pour obtenir le score (proba) attribuer a cet alignement
     * @return
     */
    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    /**
     * Pour obtenir le ieme grapheme
     * @param i
     * @return
     */
    public String getGrapheme(int i) {
        return tGraphemesAlignes[i];
    }

    /**
     * Pour obtenir le ieme phoneme
     * @param i
     * @return
     */
    public String getPhoneme(int i) {
        return tPhonemesAlignes[i];
    }

    /**
     * Methode qui converti l'alignement autant de vecteur que necessaire, separes par des virgules
     * @param debutDeMot une string indiquant comment sera note, dans le CSV, le debut d'un mot
     * @param finDeMot une string indiquant comment sera note, dans le CSV, la fin d'un mot
     * @param posTag le posTag le postag
     * @param stringGraphemeVide le grapheme vide
     */
    public String getVecteursString(String debutDeMot, String finDeMot, String posTag, String stringGraphemeVide, String separateurDoublesPhonemes) {
        StringBuffer str = new StringBuffer();
        int j, nb;

        for (int i = 0; i < tGraphemesAlignes.length; i++) {
            // On ne traite que les graphemes non vides
            if (!tGraphemesAlignes[i].equals(stringGraphemeVide)) {
                // Le grapheme courant
                str.append(tGraphemesAlignes[i] + ",");

                // recherche des 4 graphemes non vides de gauche
                j = i - 1;
                nb = 1;
                while ((j >= 0) && (nb <= 4)) {
                    if (!tGraphemesAlignes[j].equals(stringGraphemeVide)) {
                        str.append(tGraphemesAlignes[j] + ",");
                        nb++;
                    }
                    j--;
                }
                while (nb <= 4) {
                    str.append(debutDeMot + ",");
                    nb++;
                }

                // recherche des 4 graphemes non vides de droite
                j = i + 1;
                nb = 1;
                while ((j < tGraphemesAlignes.length) && (nb <= 4)) {
                    if (!tGraphemesAlignes[j].equals(stringGraphemeVide)) {
                        str.append(tGraphemesAlignes[j] + ",");
                        nb++;
                    }
                    j++;
                }
                while (nb <= 4) {
                    str.append(finDeMot + ",");
                    nb++;
                }

                // PosTag
                str.append(posTag + ",");

                // Phoneme
                if ((i < tGraphemesAlignes.length - 1) && (tGraphemesAlignes[i + 1].equals(stringGraphemeVide))) {
                    // Cas d'un double phoneme
                    str.append(tPhonemesAlignes[i] + separateurDoublesPhonemes + tPhonemesAlignes[i + 1]);
                } else {
                    str.append(tPhonemesAlignes[i]);
                }
                if (i < tGraphemesAlignes.length - 1) {
                    str.append("\n");
                }
            }
        }
        return str.toString();
    }

    /**
     * Methode qui retourne un tableau contenant les vecteurs concernant l'alignement pour la
     * methode utilisant les 4 classifieurs
     * @param debutDeMot une string indiquant comment sera note, dans le CSV, le debut d'un mot
     * @param finDeMot une string indiquant comment sera note, dans le CSV, la fin d'un mot
     * @param posTag le posTag le postag
     * @param stringGraphemeVide le grapheme vide
     * @param probasLangues les probas concernant l'appartenance du mot aux differentes langues
     */
    public String[] getVecteur_Methode_4Classifieurs(String debutDeMot, String finDeMot, String posTag, String stringGraphemeVide, float[] probasLangues) {
        String[] tResultat = new String[4];
        int j, nb;
        boolean estUnDoublePhoneme;
        boolean estSurGraphemeVide;

        tResultat[0] = ""; // Simple ou double phoneme
        tResultat[1] = ""; // Simples phonemes
        tResultat[2] = ""; // 1er double phoneme
        tResultat[3] = ""; // 2eme double phoneme

        for (int i = 0; i < tGraphemesAlignes.length; i++) {
            estUnDoublePhoneme = (i < tGraphemesAlignes.length - 1) && (tGraphemesAlignes[i + 1].equals(stringGraphemeVide));
            estSurGraphemeVide = tGraphemesAlignes[i].equals(stringGraphemeVide);

            // Le grapheme courant
            if (!estSurGraphemeVide) {
                tResultat[0] += tGraphemesAlignes[i] + ",";
                if (!estUnDoublePhoneme) {
                    tResultat[1] += tGraphemesAlignes[i] + ",";
                } else {
                    tResultat[2] += tGraphemesAlignes[i] + ",";
                }
            } else {
                tResultat[3] += tGraphemesAlignes[i - 1] + ",";
            }

            // On place le phoneme precedent dans le cas des 2eme double phonemes
            if (estSurGraphemeVide) {
                tResultat[3] += tPhonemesAlignes[i - 1] + ",";
            }

            // recherche des 4 graphemes non vides de gauche
            j = !estSurGraphemeVide ? i - 1 : i - 2;
            nb = 1;
            while ((j >= 0) && (nb <= 4)) {
                if (!estSurGraphemeVide) {
                    if (!tGraphemesAlignes[j].equals(stringGraphemeVide)) {
                        tResultat[0] += tGraphemesAlignes[j] + ",";
                        if (!estUnDoublePhoneme) {
                            tResultat[1] += tGraphemesAlignes[j] + ",";
                        } else {
                            tResultat[2] += tGraphemesAlignes[j] + ",";
                        }
                        nb++;
                    }
                } else {
                    if (!tGraphemesAlignes[j].equals(stringGraphemeVide)) {
                        tResultat[3] += tGraphemesAlignes[j] + ",";
                        nb++;
                    }
                }
                j--;
            }

            // On finit de remplir avec le caractere de debut de mot
            while (nb <= 4) {
                if (!estSurGraphemeVide) {
                    tResultat[0] += debutDeMot + ",";
                    if (!estUnDoublePhoneme) {
                        tResultat[1] += debutDeMot + ",";
                    } else {
                        tResultat[2] += debutDeMot + ",";
                    }
                } else {
                    tResultat[3] += debutDeMot + ",";
                }
                nb++;
            }


            // recherche des 4 graphemes non vides de droite
            j = i + 1;
            nb = 1;
            while ((j < tGraphemesAlignes.length) && (nb <= 4)) {
                if (!estSurGraphemeVide) {
                    if (!tGraphemesAlignes[j].equals(stringGraphemeVide)) {
                        tResultat[0] += tGraphemesAlignes[j] + ",";
                        if (!estUnDoublePhoneme) {
                            tResultat[1] += tGraphemesAlignes[j] + ",";
                        } else {
                            tResultat[2] += tGraphemesAlignes[j] + ",";
                        }
                        nb++;
                    }
                } else {
                    if (!tGraphemesAlignes[j].equals(stringGraphemeVide)) {
                        tResultat[3] += tGraphemesAlignes[j] + ",";
                        nb++;
                    }
                }
                j++;
            }

            // On finit de remplir avec le caractere de fin de mot
            while (nb <= 4) {
                if (!estSurGraphemeVide) {
                    tResultat[0] += finDeMot + ",";
                    if (!estUnDoublePhoneme) {
                        tResultat[1] += finDeMot + ",";
                    } else {
                        tResultat[2] += finDeMot + ",";
                    }
                } else {
                    tResultat[3] += finDeMot + ",";
                }
                nb++;
            }

            // PosTag
            if (!estSurGraphemeVide) {
                tResultat[0] += posTag + ",";
                if (!estUnDoublePhoneme) {
                    tResultat[1] += posTag + ",";
                } else {
                    tResultat[2] += posTag + ",";
                }
            } else {
                tResultat[3] += posTag + ",";
            }

            // Probas langues
            for (int n = 0; n < probasLangues.length; n++) {
                if (!estSurGraphemeVide) {
                    tResultat[0] += probasLangues[n] + ",";
                    if (!estUnDoublePhoneme) {
                        tResultat[1] += probasLangues[n] + ",";
                    } else {
                        tResultat[2] += probasLangues[n] + ",";
                    }
                } else {
                    tResultat[3] += probasLangues[n] + ",";
                }
            }

            // La sortie
            if (!estSurGraphemeVide) {
                tResultat[0] += estUnDoublePhoneme ? Configuration.VALEUR_SORTIE_VECTEUR_DOUBLE_PHONEME : Configuration.VALEUR_SORTIE_VECTEUR_SIMPLE_PHONEME;
                if (!estUnDoublePhoneme) {
                    tResultat[1] += tPhonemesAlignes[i];
                } else {
                    tResultat[2] += tPhonemesAlignes[i];
                }
            } else {
                tResultat[3] += tPhonemesAlignes[i];
            }

            // On met un retour a la ligne si on est pas sur le dernier grapheme
            if (!estSurGraphemeVide) {
                tResultat[0] += "\n";
                if (!estUnDoublePhoneme) {
                    tResultat[1] += "\n";
                } else {
                    tResultat[2] += "\n";
                }
            } else {
                tResultat[3] += "\n";
            }

        }

        for (int i = 0; i < tResultat.length; i++) {
            if (tResultat[i].length() > 0) {
                tResultat[i] = tResultat[i].substring(0, tResultat[i].length() - 1);
            }
        }

        return tResultat;
    }

    @Override
    public String toString() {
        StringBuffer str1 = new StringBuffer();
        StringBuffer str2 = new StringBuffer();
        String g, p;
        for (int i = 0; i < tGraphemesAlignes.length; i++) {
            g = tGraphemesAlignes[i];
            p = tPhonemesAlignes[i];
            str1.append(g + "\t");
            str2.append(p + "\t");
        }
        String r = str1 + "\n" + str2;
        if (score >= 0) {
            r += "\n(Score : " + score + ")";
        }
        return r;
    }

    public int comparerScore(AlignementGraphemesPhonemes a) {
        if (getScore() < a.getScore()) {
            return -1;
        } else if (getScore() == a.getScore()) {
            return 0;
        } else {
            return 1;
        }
    }

    /**
     * Retourne le tableau des phonemes
     */
    public String[] getPhonemes() {
        ArrayList<String> al = new ArrayList<String>();
        for (int i = 0; i < tPhonemesAlignes.length; i++) {
            if (!tPhonemesAlignes[i].equals(Configuration.STRING_DE_REMPLACEMENT_PHONEME_VIDE)) {
                al.add(tPhonemesAlignes[i]);
            }
        }

        return Utils.arrayListStringToArrayString(al);
    }
}
