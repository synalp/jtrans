/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.phonetiseur;

/**
 * Cette classe est une association entre les phonemes, le postag
 * et son type (singulier, pluriel...)
 */
public class SuitePhonemes {

    private String[] tPhonemes;
    private String posTag;

    public SuitePhonemes(String[] tPhonemes, String posTag) {
        this.tPhonemes = tPhonemes;
        this.posTag = posTag;
    }

    /**
     * Permet d'obtenir un tableau des phonemes
     * @return le tableau des phonemes
     */
    public String[] getTPhonemes() {
        return tPhonemes;
    }

    /**
     * Permet d'obtenir le PosTag
     * @return le PosTag
     */
    public String getPosTag() {
        return posTag;
    }

    /**
     * Permet d'obtenir le ieme phoneme
     * @param i le ieme phoneme que l'on souhaite obtenir
     * @return le ieme phoneme
     */
    public String getPhoneme(int i) {
        return tPhonemes[i];
    }

    /**
     * Permet d'obtenir le nombre de phonemes
     * @return le nombre de phonemes
     */
    public int getNbPhonemes() {
        return tPhonemes.length;
    }

    /**
     * Retourn une String representant les phonemes ainsi que le PosTag et le type
     */
    public String toStringDebug() {
        StringBuffer str = new StringBuffer();
        str.append(this.toString());
        str.append("(" + posTag + ")");
        return str.toString();
    }

    @Override
    public String toString() {
        StringBuffer str = new StringBuffer();
        for (int i = 0; i < tPhonemes.length; i++) {
            str.append(tPhonemes[i] + " ");
        }
        return str.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SuitePhonemes other = (SuitePhonemes) obj;
        if (this.tPhonemes.length != other.tPhonemes.length) {
            return false;
        }
        for (int i = 0; i < this.tPhonemes.length; i++) {
            if (!this.tPhonemes[i].equals(other.tPhonemes[i])) {
                return false;
            }
        }
        if ((this.posTag == null) ? (other.posTag != null) : !this.posTag.equals(other.posTag)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + (this.tPhonemes != null ? this.tPhonemes.hashCode() : 0);
        hash = 67 * hash + (this.posTag != null ? this.posTag.hashCode() : 0);
        return hash;
    }
}
