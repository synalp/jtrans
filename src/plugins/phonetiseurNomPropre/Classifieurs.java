/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.phonetiseurNomPropre;

import java.io.*;
import java.util.ArrayList;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.AODEsr;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.J48;
import weka.core.*;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

/**
 * Les reseaux neuronaux du phonetiseur
 */
public class Classifieurs {

    private LexiqueGraphemesPhonemesPostag lexique;
    private String repertoireFichiersARFF;
    private J48 tClassifieurSimplePhoneme[];
    private J48 classifieurSimpleOuDoublePhoneme, classifieurDoublePhoneme1er, classifieurDoublePhoneme2eme;
    private Remove filtreSimpleOuDoublePhoneme, filtreDoublePhoneme1er, filtreDoublePhoneme2eme, filtreSimplePhoneme;
    // Les "types" des fichiers ARFF
    private Instances instancesSimpleOuDoublePhoneme, instancesDoublePhoneme1er, instancesDoublePhoneme2eme;
    private Instances[] tInstancesSimplePhoneme;

    public Classifieurs(LexiqueGraphemesPhonemesPostag lexique, String repertoireFichiersARFF) throws Exception {
        this.lexique = lexique;
        this.repertoireFichiersARFF = repertoireFichiersARFF;
        initFiltres();
        initInstances();
    }

    private void initFiltres() {
        /*
        filtreSimpleOuDoublePhoneme = new Remove();
        filtreSimpleOuDoublePhoneme.setAttributeIndices("1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,last");
        filtreSimpleOuDoublePhoneme.setInvertSelection(true);

        filtreDoublePhoneme1er = new Remove();
        filtreDoublePhoneme1er.setAttributeIndices("1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,last");
        filtreDoublePhoneme1er.setInvertSelection(true);

        filtreDoublePhoneme2eme = new Remove();
        filtreDoublePhoneme2eme.setAttributeIndices("1,2,12,13,14,15,16,last");
        filtreDoublePhoneme2eme.setInvertSelection(true);

        filtreSimplePhoneme = new Remove();
        //filtreSimplePhoneme.setAttributeIndices("2,3,6,7,8,10,last");
        filtreSimplePhoneme.setAttributeIndices("2,3,4,5,6,7,8,9,10,11,12,13,14,15,last");
        filtreSimplePhoneme.setInvertSelection(true);
         * */

        filtreSimpleOuDoublePhoneme = new Remove();
        filtreSimpleOuDoublePhoneme.setAttributeIndices("1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,last");
        filtreSimpleOuDoublePhoneme.setInvertSelection(true);

        filtreDoublePhoneme1er = new Remove();
        filtreDoublePhoneme1er.setAttributeIndices("1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,last");
        filtreDoublePhoneme1er.setInvertSelection(true);

        filtreDoublePhoneme2eme = new Remove();
        filtreDoublePhoneme2eme.setAttributeIndices("1,2,12,13,14,15,16,last");
        filtreDoublePhoneme2eme.setInvertSelection(true);

        filtreSimplePhoneme = new Remove();
        filtreSimplePhoneme.setAttributeIndices("2,3,4,5,6,7,8,9,10,11,12,13,14,15,last");
        filtreSimplePhoneme.setInvertSelection(true);
    }

    private void initInstances() throws Exception {
        // On "charge" les types des fichiers ARFF
        DataSource source;

        source = new DataSource(repertoireFichiersARFF + Configuration.NOM_FICHIER_ARFF_SIMPLE_OU_DOUBLE_PHONEME + ".arff");
        instancesSimpleOuDoublePhoneme = source.getStructure();
        instancesSimpleOuDoublePhoneme.setClassIndex(instancesSimpleOuDoublePhoneme.numAttributes() - 1);
        //instancesSimpleOuDoublePhoneme = appliquerFiltre(filtreSimpleOuDoublePhoneme, instancesSimpleOuDoublePhoneme);

        source = new DataSource(repertoireFichiersARFF + Configuration.NOM_FICHIER_ARFF_1er_DOUBLE_PHONEME + ".arff");
        instancesDoublePhoneme1er = source.getStructure();
        instancesDoublePhoneme1er.setClassIndex(instancesDoublePhoneme1er.numAttributes() - 1);
        //instancesDoublePhoneme1er = appliquerFiltre(filtreDoublePhoneme1er, instancesDoublePhoneme1er);

        source = new DataSource(repertoireFichiersARFF + Configuration.NOM_FICHIER_ARFF_2eme_DOUBLE_PHONEME + ".arff");
        instancesDoublePhoneme2eme = source.getStructure();
        instancesDoublePhoneme2eme.setClassIndex(instancesDoublePhoneme2eme.numAttributes() - 1);
        //instancesDoublePhoneme2eme = appliquerFiltre(filtreDoublePhoneme2eme, instancesDoublePhoneme2eme);

        tInstancesSimplePhoneme = new Instances[lexique.getNbGraphemes()];
        for (int i = 0; i < lexique.getNbGraphemes(); i++) {
            String graphemeCourant = lexique.getGraphemeFromIndice(i);
            source = new DataSource(repertoireFichiersARFF + Configuration.NOM_FICHIER_ARFF_SIMPLE_PHONEME + "_" + graphemeCourant + ".arff");
            tInstancesSimplePhoneme[i] = source.getStructure();
            tInstancesSimplePhoneme[i].setClassIndex(tInstancesSimplePhoneme[i].numAttributes() - 1);
            //tInstancesSimplePhoneme[i] = appliquerFiltre(filtreSimplePhoneme, tInstancesSimplePhoneme[i]);
        }
    }

    public void lancerApprentissage(String repertoireFichiersARFF) throws Exception {
        // Classifieur simpleOuDoublePhoneme
        System.out.print("    - Classifieur simpleOuDoublePhoneme...");
        entrainerClassifieurSimpleOuDoublePhoneme(repertoireFichiersARFF);
        System.gc();
        System.out.println(" Ok");

        // Classifieur doublePhoneme1er
        System.out.print("    - Classifieur doublePhoneme1er...");
        entrainerClassifieurDoublePhoneme1er(repertoireFichiersARFF);
        System.gc();
        System.out.println(" Ok");

        // Classifieur doublePhoneme2eme
        System.out.print("    - Classifieur doublePhoneme2eme...");
        entrainerClassifieurDoublePhoneme2eme(repertoireFichiersARFF);
        System.gc();
        System.out.println(" Ok");

        // Classifieur simplesPhonemes
        System.out.println("    - Classifieur simplesPhonemes...");
        entrainerClassifieurSimplesPhonemes(repertoireFichiersARFF);
        System.gc();
        System.out.println("      => Ok");
    }

    private void entrainerClassifieurSimpleOuDoublePhoneme(String repertoireFichiersARFF) throws Exception {
        Instances instances;

        DataSource source = new DataSource(repertoireFichiersARFF + Configuration.NOM_FICHIER_ARFF_SIMPLE_OU_DOUBLE_PHONEME + ".arff");
        instances = source.getDataSet();

        // On definit la sortie (dernier attibut)
        instances.setClassIndex(instances.numAttributes() - 1);

        // On ne garde certains attributs
        instances = appliquerFiltre(filtreSimpleOuDoublePhoneme, instances);

        // On lance l'apprentissage
        classifieurSimpleOuDoublePhoneme = new J48();
        classifieurSimpleOuDoublePhoneme.buildClassifier(instances);
    }

    private void entrainerClassifieurDoublePhoneme1er(String repertoireFichiersARFF) throws Exception {
        Instances instances;

        DataSource source = new DataSource(repertoireFichiersARFF + Configuration.NOM_FICHIER_ARFF_1er_DOUBLE_PHONEME + ".arff");
        instances = source.getDataSet();

        // On definit la sortie (dernier attibut)
        instances.setClassIndex(instances.numAttributes() - 1);

        // On ne garde certains attributs
        instances = appliquerFiltre(filtreDoublePhoneme1er, instances);

        // On lance l'apprentissage
        classifieurDoublePhoneme1er = new J48();
        classifieurDoublePhoneme1er.buildClassifier(instances);


    }

    private void entrainerClassifieurDoublePhoneme2eme(String repertoireFichiersARFF) throws Exception {
        Instances instances;

        DataSource source = new DataSource(repertoireFichiersARFF + Configuration.NOM_FICHIER_ARFF_2eme_DOUBLE_PHONEME + ".arff");
        instances = source.getDataSet();

        // On definit la sortie (dernier attibut)
        instances.setClassIndex(instances.numAttributes() - 1);

        // On ne garde certains attributs
        instances = appliquerFiltre(filtreDoublePhoneme2eme, instances);

        // On lance l'apprentissage
        classifieurDoublePhoneme2eme = new J48();
        classifieurDoublePhoneme2eme.buildClassifier(instances);
    }

    private void entrainerClassifieurSimplesPhonemes(String repertoireFichiersARFF) throws Exception {
        Instances instances;
        DataSource source = null;

        tClassifieurSimplePhoneme = new J48[lexique.getNbGraphemes()];

        for (int i = 0; i < lexique.getNbGraphemes(); i++) {
            String graphemeCourant = lexique.getGraphemeFromIndice(i);

            try {
                source = new DataSource(repertoireFichiersARFF + Configuration.NOM_FICHIER_ARFF_SIMPLE_PHONEME + "_" + graphemeCourant + ".arff");
            } catch (Exception e) {
                // Fichier introuvable
                System.out.println("Pas de fichier " + repertoireFichiersARFF + Configuration.NOM_FICHIER_ARFF_SIMPLE_PHONEME + "_" + graphemeCourant + ".arff");
                source = null;
            }

            if (source != null) {
                System.out.println("        * " + graphemeCourant + " (" + (i + 1) + "/" + lexique.getNbGraphemes() + ")");
                instances = source.getDataSet();

                // On definit la sortie (dernier attibut)
                instances.setClassIndex(instances.numAttributes() - 1);

                // On ne garde certains attributs
                instances = appliquerFiltre(filtreSimplePhoneme, instances);

                // On lance l'apprentissage
                tClassifieurSimplePhoneme[i] = new J48();
                tClassifieurSimplePhoneme[i].buildClassifier(instances);
                System.gc();
            }

        }
    }

    private Instances appliquerFiltre(Filter filtre, Instances instances) throws Exception {
        Instances newInstances;
        Instance temp;

        filtre.setInputFormat(instances);
        for (int i = 0; i < instances.numInstances(); i++) {
            filtre.input(instances.instance(i));
        }

        filtre.batchFinished();
        newInstances = filtre.getOutputFormat();
        while ((temp = filtre.output()) != null) {
            newInstances.add(temp);
        }

        return newInstances;
    }

    private Instance appliquerFiltreAUneInstance(Filter filtre, Instance instance, Instances instances) throws Exception {
        filtre.setInputFormat(instances);
        filtre.input(instance);
        filtre.batchFinished();

        return filtre.output();
    }

    public void sauvegarderClassifieurs(String repertoireCible) throws IOException {
        ObjectOutputStream oos;

        oos = new ObjectOutputStream(new FileOutputStream(repertoireCible + Configuration.NOM_FICHIER_MODEL_SIMPLE_OU_DOUBLE_PHONEME + ".model"));
        oos.writeObject(classifieurSimpleOuDoublePhoneme);
        oos.flush();
        oos.close();

        oos = new ObjectOutputStream(new FileOutputStream(repertoireCible + Configuration.NOM_FICHIER_MODEL_1er_DOUBLE_PHONEME + ".model"));
        oos.writeObject(classifieurDoublePhoneme1er);
        oos.flush();
        oos.close();

        oos = new ObjectOutputStream(new FileOutputStream(repertoireCible + Configuration.NOM_FICHIER_MODEL_2eme_DOUBLE_PHONEME + ".model"));
        oos.writeObject(classifieurDoublePhoneme2eme);
        oos.flush();
        oos.close();

        for (int i = 0; i < lexique.getNbGraphemes(); i++) {
            String graphemeCourant = lexique.getGraphemeFromIndice(i);
            oos = new ObjectOutputStream(new FileOutputStream(repertoireCible + Configuration.NOM_FICHIER_MODEL_SIMPLE_PHONEME + "_" + graphemeCourant + ".model"));
            oos.writeObject(tClassifieurSimplePhoneme[i]);
            oos.flush();
            oos.close();
        }

    }

    public void chargerClassifieurs(String repertoireSource) throws Exception {
        ObjectInputStream oos;

        tClassifieurSimplePhoneme = new J48[lexique.getNbGraphemes()];

        oos = new ObjectInputStream(new FileInputStream(repertoireSource + Configuration.NOM_FICHIER_MODEL_SIMPLE_OU_DOUBLE_PHONEME + ".model"));
        classifieurSimpleOuDoublePhoneme = (J48) oos.readObject();
        oos.close();

        oos = new ObjectInputStream(new FileInputStream(repertoireSource + Configuration.NOM_FICHIER_MODEL_1er_DOUBLE_PHONEME + ".model"));
        classifieurDoublePhoneme1er = (J48) oos.readObject();
        oos.close();

        oos = new ObjectInputStream(new FileInputStream(repertoireSource + Configuration.NOM_FICHIER_MODEL_2eme_DOUBLE_PHONEME + ".model"));
        classifieurDoublePhoneme2eme = (J48) oos.readObject();
        oos.close();

        for (int i = 0; i < lexique.getNbGraphemes(); i++) {
            String graphemeCourant = lexique.getGraphemeFromIndice(i);
            try {
                oos = new ObjectInputStream(new FileInputStream(repertoireSource + Configuration.NOM_FICHIER_MODEL_SIMPLE_PHONEME + "_" + graphemeCourant + ".model"));
                tClassifieurSimplePhoneme[i] = (J48) oos.readObject();
                oos.close();
                System.gc();
            } catch (Exception e) {
                // Fichier inconnu
            }
        }

        initInstances();
    }

    /**
     * => on peut passer null au parametre Postag si on ne veut pas remplir le champ
     */
    public AlignementGraphemesPhonemes phonetiser(String[] tGraphemes, String Postag, float[] tProbasLangues) throws Exception {
        ArrayList<String> alGraphemes = new ArrayList<String>();
        ArrayList<String> alPhonemes = new ArrayList<String>();

        String graphemeCourant, doublePhoneme1er;
        Instance instance;
        int indiceGraphemeCourant;

        for (int i = 0; i < tGraphemes.length; i++) {
            // Est-ce-que le grapheme i est un simple ou double phoneme ?
            instance = new Instance(11 + tProbasLangues.length);
            instance.setDataset(instancesSimpleOuDoublePhoneme);
            instance.setValue(0, tGraphemes[i]);
            instance.setValue(1, (i >= 1) ? tGraphemes[i - 1] : Configuration.GRAPHEME_DEBUT_DE_MOT_VECTEUR);
            instance.setValue(2, (i >= 2) ? tGraphemes[i - 2] : Configuration.GRAPHEME_DEBUT_DE_MOT_VECTEUR);
            instance.setValue(3, (i >= 3) ? tGraphemes[i - 3] : Configuration.GRAPHEME_DEBUT_DE_MOT_VECTEUR);
            instance.setValue(4, (i >= 4) ? tGraphemes[i - 4] : Configuration.GRAPHEME_DEBUT_DE_MOT_VECTEUR);
            instance.setValue(5, (i < tGraphemes.length - 1) ? tGraphemes[i + 1] : Configuration.GRAPHEME_FIN_DE_MOT_VECTEUR);
            instance.setValue(6, (i < tGraphemes.length - 2) ? tGraphemes[i + 2] : Configuration.GRAPHEME_FIN_DE_MOT_VECTEUR);
            instance.setValue(7, (i < tGraphemes.length - 3) ? tGraphemes[i + 3] : Configuration.GRAPHEME_FIN_DE_MOT_VECTEUR);
            instance.setValue(8, (i < tGraphemes.length - 4) ? tGraphemes[i + 4] : Configuration.GRAPHEME_FIN_DE_MOT_VECTEUR);
            if (Postag != null) {
                instance.setValue(9, Postag);
            }
            for (int j = 0; j < tProbasLangues.length; j++) {
                instance.setValue(10 + j, tProbasLangues[j]);
            }
            instance = appliquerFiltreAUneInstance(filtreSimpleOuDoublePhoneme, instance, instancesSimpleOuDoublePhoneme);

            if (resultatClassifieur(instance, classifieurSimpleOuDoublePhoneme, instancesSimpleOuDoublePhoneme).equals(Configuration.VALEUR_SORTIE_VECTEUR_SIMPLE_PHONEME)) {
                // Cas d'un simple phoneme
                graphemeCourant = tGraphemes[i];
                indiceGraphemeCourant = lexique.getIndiceFromGrapheme(graphemeCourant);
                instance = new Instance(11 + tProbasLangues.length);
                instance.setDataset(tInstancesSimplePhoneme[indiceGraphemeCourant]);
                instance.setValue(0, tGraphemes[i]);
                instance.setValue(1, (i >= 1) ? tGraphemes[i - 1] : Configuration.GRAPHEME_DEBUT_DE_MOT_VECTEUR);
                instance.setValue(2, (i >= 2) ? tGraphemes[i - 2] : Configuration.GRAPHEME_DEBUT_DE_MOT_VECTEUR);
                instance.setValue(3, (i >= 3) ? tGraphemes[i - 3] : Configuration.GRAPHEME_DEBUT_DE_MOT_VECTEUR);
                instance.setValue(4, (i >= 4) ? tGraphemes[i - 4] : Configuration.GRAPHEME_DEBUT_DE_MOT_VECTEUR);
                instance.setValue(5, (i < tGraphemes.length - 1) ? tGraphemes[i + 1] : Configuration.GRAPHEME_FIN_DE_MOT_VECTEUR);
                instance.setValue(6, (i < tGraphemes.length - 2) ? tGraphemes[i + 2] : Configuration.GRAPHEME_FIN_DE_MOT_VECTEUR);
                instance.setValue(7, (i < tGraphemes.length - 3) ? tGraphemes[i + 3] : Configuration.GRAPHEME_FIN_DE_MOT_VECTEUR);
                instance.setValue(8, (i < tGraphemes.length - 4) ? tGraphemes[i + 4] : Configuration.GRAPHEME_FIN_DE_MOT_VECTEUR);
                if (Postag != null) {
                    instance.setValue(9, Postag);
                }
                for (int j = 0; j < tProbasLangues.length; j++) {
                    instance.setValue(10 + j, tProbasLangues[j]);
                }
                instance = appliquerFiltreAUneInstance(filtreSimplePhoneme, instance, tInstancesSimplePhoneme[indiceGraphemeCourant]);

                alGraphemes.add(graphemeCourant);
                alPhonemes.add(resultatClassifieur(instance, tClassifieurSimplePhoneme[indiceGraphemeCourant], tInstancesSimplePhoneme[indiceGraphemeCourant]));
            } else {
                // Cas d'un double phoneme
                // Double phoneme 1
                instance = new Instance(11 + tProbasLangues.length);
                instance.setDataset(instancesDoublePhoneme1er);
                instance.setValue(0, tGraphemes[i]);
                instance.setValue(1, (i >= 1) ? tGraphemes[i - 1] : Configuration.GRAPHEME_DEBUT_DE_MOT_VECTEUR);
                instance.setValue(2, (i >= 2) ? tGraphemes[i - 2] : Configuration.GRAPHEME_DEBUT_DE_MOT_VECTEUR);
                instance.setValue(3, (i >= 3) ? tGraphemes[i - 3] : Configuration.GRAPHEME_DEBUT_DE_MOT_VECTEUR);
                instance.setValue(4, (i >= 4) ? tGraphemes[i - 4] : Configuration.GRAPHEME_DEBUT_DE_MOT_VECTEUR);
                instance.setValue(5, (i < tGraphemes.length - 1) ? tGraphemes[i + 1] : Configuration.GRAPHEME_FIN_DE_MOT_VECTEUR);
                instance.setValue(6, (i < tGraphemes.length - 2) ? tGraphemes[i + 2] : Configuration.GRAPHEME_FIN_DE_MOT_VECTEUR);
                instance.setValue(7, (i < tGraphemes.length - 3) ? tGraphemes[i + 3] : Configuration.GRAPHEME_FIN_DE_MOT_VECTEUR);
                instance.setValue(8, (i < tGraphemes.length - 4) ? tGraphemes[i + 4] : Configuration.GRAPHEME_FIN_DE_MOT_VECTEUR);
                if (Postag != null) {
                    instance.setValue(9, Postag);
                }
                for (int j = 0; j < tProbasLangues.length; j++) {
                    instance.setValue(10 + j, tProbasLangues[j]);
                }
                instance = appliquerFiltreAUneInstance(filtreDoublePhoneme1er, instance, instancesDoublePhoneme1er);

                doublePhoneme1er = resultatClassifieur(instance, classifieurDoublePhoneme1er, instancesDoublePhoneme1er);

                alGraphemes.add(tGraphemes[i]);
                alPhonemes.add(doublePhoneme1er);

                // Double phoneme 2
                instance = new Instance(12 + tProbasLangues.length);
                instance.setDataset(instancesDoublePhoneme2eme);
                instance.setValue(0, tGraphemes[i]);
                instance.setValue(1, doublePhoneme1er);
                instance.setValue(2, (i >= 1) ? tGraphemes[i - 1] : Configuration.GRAPHEME_DEBUT_DE_MOT_VECTEUR);
                instance.setValue(3, (i >= 2) ? tGraphemes[i - 2] : Configuration.GRAPHEME_DEBUT_DE_MOT_VECTEUR);
                instance.setValue(4, (i >= 3) ? tGraphemes[i - 3] : Configuration.GRAPHEME_DEBUT_DE_MOT_VECTEUR);
                instance.setValue(5, (i >= 4) ? tGraphemes[i - 4] : Configuration.GRAPHEME_DEBUT_DE_MOT_VECTEUR);
                instance.setValue(6, (i < tGraphemes.length - 1) ? tGraphemes[i + 1] : Configuration.GRAPHEME_FIN_DE_MOT_VECTEUR);
                instance.setValue(7, (i < tGraphemes.length - 2) ? tGraphemes[i + 2] : Configuration.GRAPHEME_FIN_DE_MOT_VECTEUR);
                instance.setValue(8, (i < tGraphemes.length - 3) ? tGraphemes[i + 3] : Configuration.GRAPHEME_FIN_DE_MOT_VECTEUR);
                instance.setValue(9, (i < tGraphemes.length - 4) ? tGraphemes[i + 4] : Configuration.GRAPHEME_FIN_DE_MOT_VECTEUR);
                if (Postag != null) {
                    instance.setValue(10, Postag);
                }
                for (int j = 0; j < tProbasLangues.length; j++) {
                    instance.setValue(11 + j, tProbasLangues[j]);
                }
                instance = appliquerFiltreAUneInstance(filtreDoublePhoneme2eme, instance, instancesDoublePhoneme2eme);

                alGraphemes.add(Configuration.STRING_DE_REMPLACEMENT_GRAPHEME_VIDE);
                alPhonemes.add(resultatClassifieur(instance, classifieurDoublePhoneme2eme, instancesDoublePhoneme2eme));
            }
        }

        return new AlignementGraphemesPhonemes(alGraphemes, alPhonemes);
    }

    private String resultatClassifieur(Instance instance, Classifier classifieur, Instances instances) throws Exception {
        double r = classifieur.classifyInstance(instance);
        return instances.attribute(instances.numAttributes() - 1).value((int) r);
    }

    public J48 getClassifieurDoublePhoneme1er() {
        return classifieurDoublePhoneme1er;
    }

    public void setClassifieurDoublePhoneme1er(J48 classifieurDoublePhoneme1er) {
        this.classifieurDoublePhoneme1er = classifieurDoublePhoneme1er;
    }

    public J48 getClassifieurDoublePhoneme2eme() {
        return classifieurDoublePhoneme2eme;
    }

    public void setClassifieurDoublePhoneme2eme(J48 classifieurDoublePhoneme2eme) {
        this.classifieurDoublePhoneme2eme = classifieurDoublePhoneme2eme;
    }

    public J48 getClassifieurSimpleOuDoublePhoneme() {
        return classifieurSimpleOuDoublePhoneme;
    }

    public void setClassifieurSimpleOuDoublePhoneme(J48 classifieurSimpleOuDoublePhoneme) {
        this.classifieurSimpleOuDoublePhoneme = classifieurSimpleOuDoublePhoneme;
    }

    public J48[] getTClassifieurSimplePhoneme() {
        return tClassifieurSimplePhoneme;
    }

    public void setTClassifieurSimplePhoneme(J48[] tClassifieurSimplePhoneme) {
        this.tClassifieurSimplePhoneme = tClassifieurSimplePhoneme;
    }
}
