/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package jtrans.speechreco.frontEnd;

public class DCT implements FrontEnd {
    /**
     * Compute the MelCosine filter bank.
     */
	private int numberMelFilters;
	public int cepstrumSize = 13;
	private float[][] melcosine;
	private FrontEnd input;
	
	public DCT(FrontEnd melin) {
		input = melin;
		numberMelFilters = melin.getNcoefs();
		computeMelCosine();
	}

	public int getNcoefs() {
		return cepstrumSize;
	}
	
	private void computeMelCosine() {
		melcosine = new float[cepstrumSize][numberMelFilters];
		double period = (double) 2 * numberMelFilters;
		for (int i = 0; i < cepstrumSize; i++) {
			double frequency = 2 * Math.PI * i / period;
			for (int j = 0; j < numberMelFilters; j++) {
				melcosine[i][j] = (float) Math.cos(frequency * (j + 0.5));
			}
		}
	}

	private float[] process(float[] melspectrum)
			throws IllegalArgumentException {
		if (melspectrum.length != numberMelFilters) {
			throw new IllegalArgumentException(
					"MelSpectrum size is incorrect: melspectrum.length == "
							+ melspectrum.length + ", numberMelFilters == "
							+ numberMelFilters);
		}
		// first compute the log of the spectrum
		for (int i = 0; i < melspectrum.length; ++i) {
			if (melspectrum[i] > 0) {
				melspectrum[i] = (float)Math.log(melspectrum[i]);
			} else {
				// in case melspectrum[i] isn't greater than 0
				// instead of trying to compute a log we just
				// assign a very small number
				melspectrum[i] = (float)-1.0e+5;
			}
		}
		// create the cepstrum by apply the melcosine filter
		float[] cepstrum = applyMelCosine(melspectrum);
		return cepstrum;
	}

    private float[] applyMelCosine(float[] melspectrum) {
        // create the cepstrum
        float[] cepstrum = new float[cepstrumSize];
        float period = (float) numberMelFilters;
        float beta = 0.5f;
        // apply the melcosine filter
        for (int i = 0; i < cepstrum.length; i++) {
            if (numberMelFilters > 0) {
                float[] melcosine_i = melcosine[i];
                int j = 0;
                cepstrum[i] += (beta * melspectrum[j] * melcosine_i[j]);
                for (j = 1; j < numberMelFilters; j++) {
                    cepstrum[i] += (melspectrum[j] * melcosine_i[j]);
                }
                cepstrum[i] /= period;
            }
        }
        return cepstrum;
    }
    
    public float[] getOneVector() {
    	float[] inf = input.getOneVector();
    	if (inf==null) return null;
        float[] output = process(inf);
        return output;
    }
}
