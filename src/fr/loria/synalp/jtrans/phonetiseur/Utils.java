/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package fr.loria.synalp.jtrans.phonetiseur;

import java.util.*;

/**
 * Un classe contenant divers methodes utilisees un peu partout
 */
public class Utils {

    /**
     * Cette methode tranforme une String en un tableau dont chaque case
     * contient un caractere sous forme de String.
     * Ex : "abc" => ["a", "b", "c"]
     * @param string la String a transformer
     * @return un tableau de String.
     */
    public static String[] stringToArrayString(String string) {
		// Don't use split("")! Its behavior changed in Java 8!
		String[] str = new String[string.length()];
		for (int i = 0; i < string.length(); i++) {
			str[i] = String.valueOf(string.charAt(i));
		}
		return str;
    }

    /**
     * Cette methode tranforme une ArrayList de String en un tableau de String
     * @param alString l'arrayList a transformer
     * @return un tableau de String.
     */
    public static String[] arrayListStringToArrayString(ArrayList<String> alString) {
        String[] str = new String[alString.size()];
        for (int i = 0; i < alString.size(); i++) {
            str[i] = alString.get(i);
        }
        return str;
    }

    /**
     * Cette methode prend 2 matrice de cumul et retourne les 2 matrices de
     * probabilites.
     * Attention : la somme des valeurs d'une meme ligne egale 1 - proba
     * (proba : valeur passee en parametre).
     * Note : il est possible de passer la valeur null au parametre matrice2
     * Important : les 2 matrices doivent avoir le meme nombre de lignes
     */
    public static LinkedList<double[][]> convertirMatricesCumulsEnMatricesProbas(int[][] matrice1, int[][] matrice2, double proba) {
        double[][] matrice1D = new double[matrice1.length][matrice1[0].length];
        double[][] matrice2D = null;
        if (matrice2 != null) {
            matrice2D = new double[matrice2.length][matrice2[0].length];
        }
        double total;
        // Pour chaque ligne
        for (int l = 0; l < matrice1.length; l++) {
            total = 0;
            // Calcul de la somme des valeurs d'une meme ligne
            for (int c = 0; c < matrice1[0].length; c++) {
                total += matrice1[l][c];
            }
            if (matrice2 != null) {
                for (int c = 0; c < matrice2[0].length; c++) {
                    total += matrice2[l][c];
                }
            }
            // On calcule les nouvelles probas (sans tenir compte du parametre "proba")
            for (int c = 0; c < matrice1[0].length; c++) {
                matrice1D[l][c] = ((double) matrice1[l][c]) / total * ((double) (1 - proba));
            }
            if (matrice2 != null) {
                for (int c = 0; c < matrice2[0].length; c++) {
                    matrice2D[l][c] = ((double) matrice2[l][c]) / total * ((double) (1 - proba));
                }
            }
        }

        LinkedList<double[][]> r = new LinkedList<double[][]>();
        r.addFirst(matrice1D);
        r.addLast(matrice2D);

        return r;
    }

    /**
     * Converti un tableau d'ArrayList d'entier en tableau de tableau d'entier.
     * Hypothese : toutes les ArrayLists ont la meme taille
     * @param matriceEnConstruction
     * @return
     */
    public static int[][] convertirTableauArrayList_En_TableauTableau(ArrayList<Integer>[] matrice) {
        int nbLignes = matrice.length;
        int nbColonnes = matrice[0].size();
        int[][] t = new int[nbLignes][nbColonnes];

        for (int l = 0; l < nbLignes; l++) {
            for (int c = 0; c < nbColonnes; c++) {
                t[l][c] = matrice[l].get(c);
            }
        }
        return t;
    }

    public static String convertirMatriceDouble_String(double[][] m) {
        StringBuffer str = new StringBuffer();
        for (int l = m.length - 1; l >= 0; l--) {
            for (int c = 0; c < m[0].length - 1; c++) {
                str.append(m[l][c] + "\t");
            }
            str.append(m[l][m[0].length - 1] + "\n");
        }
        return str.toString();
    }

    public static String convertirMatriceInt_String(int[][] m) {
        StringBuffer str = new StringBuffer();
        for (int l = m.length - 1; l >= 0; l--) {
            for (int c = 0; c < m[0].length - 1; c++) {
                str.append(m[l][c] + "\t");
            }
            str.append(m[l][m[0].length - 1] + "\n");
        }
        return str.toString();
    }

    public static String convertirTableauString_String(String[] t) {
        StringBuffer str = new StringBuffer();
        for (int i = 0; i < t.length; i++) {
            str.append(t[i]);
        }

        return str.toString();
    }
}
