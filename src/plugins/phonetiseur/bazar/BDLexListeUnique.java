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

public class BDLexListeUnique {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Erreur :\n-arg1 : source\n-arg2 : cible");
        } else {
            BufferedReader r = FileUtils.openFileISO(args[0]);
            PrintWriter w = FileUtils.writeFileUTF(args[1]);
            String l;
            TreeSet<String> al = new TreeSet<String>();

            while ((l = r.readLine()) != null) {
                String[] t = l.split(";");

                // Le mot
                String mot = t[0];
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
