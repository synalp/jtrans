/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.phonetiseur.bazar;

import java.io.*;
import java.util.ArrayList;
import java.util.TreeSet;

import plugins.phonetiseur.Configuration;
import plugins.utils.FileUtils;

public class MorphalouListeUnique {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Erreur :\n-arg1 : source\n-arg2 : cible");
        } else {
            BufferedReader r = FileUtils.openFileUTF(args[0]);
            PrintWriter w = FileUtils.writeFileUTF(args[1]);
            String l;
            TreeSet<String> al = new TreeSet<String>();

            while ((l = r.readLine()) != null) {
                String[] leReste = l.substring(0, l.indexOf(" ___")).split(" ");

                // Le mot
                String mot = leReste[1];
                for (int j = 2; j < (leReste.length - 2); j++) {
                    mot += " " + leReste[j];
                }
                mot = mot.replace('\'', Configuration.CHAR_DE_REMPLACEMENT_APPOSTROPHE);
                if (!al.contains(mot)) {
                    al.add(mot);
                }
            }

            String s;
            while ((s = al.pollFirst()) != null) {
                w.println(s);
            }

            r.close();
            w.close();
        }
    }
}
