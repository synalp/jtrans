/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package speechreco.frontEnd;

public class MelFreq implements FrontEnd {
	private FFT input;
    private MelFilter[] filter;
    private double sampleRate;
	private int numberFftPoints;
	public int numberFilters = 40;
	public float minFreq = 130f;
	public float maxFreq = 6800f;
	
	public MelFreq(FFT in, float sampPeriod) {
		input = in;
		numberFftPoints = (in.getNcoefs() - 1) * 2;
		// la periode est donne en 10E-7 secondes dans HTK.
		sampleRate = 10000000/sampPeriod;
		buildFilterbank(numberFftPoints, numberFilters, minFreq, maxFreq);
	}
	
	public int getNcoefs() {
		return numberFilters;
	}
	
    private float[] process(float[] in) throws IllegalArgumentException {
		if (in.length != ((numberFftPoints >> 1) + 1)) {
			throw new IllegalArgumentException(
					"Window size is incorrect: in.length == " + in.length
							+ ", numberFftPoints == "
							+ ((numberFftPoints >> 1) + 1));
		}
		float[] output = new float[numberFilters];
		/**
		 * Filter input power spectrum
		 */
		for (int i = 0; i < numberFilters; i++) {
			output[i] = filter[i].filterOutput(in);
		}
		return output;
	}

	public float[] getOneVector() {
		float[] inf = input.getOneVector();
		if (inf==null) return null;
		float[] output = process(inf);
		return output;
	}

	/**
	 * Compute mel frequency from linear frequency.
	 * 
	 * Since we don't have <code>log10()</code>, we have to compute it using
	 * natural log: <b>log10(x) = ln(x) / ln(10) </b>
	 * 
	 * @param inputFreq
	 *            the input frequency in linear scale
	 * 
	 * @return the frequency in a mel scale
	 * 
	 */
    private double linToMelFreq(double inputFreq) {
        return (2595.0 * (Math.log(1.0 + inputFreq / 700.0) / Math.log(10.0)));
    }
    /**
     * Compute linear frequency from mel frequency.
     * 
     * @param inputFreq
     *                the input frequency in mel scale
     * 
     * @return the frequency in a linear scale
     *  
     */
    private double melToLinFreq(double inputFreq) {
        return (700.0 * (Math.pow(10.0, (inputFreq / 2595.0)) - 1.0));
    }
    /**
     * Sets the given frequency to the nearest frequency bin from the FFT. The
     * FFT can be thought of as a sampling of the actual spectrum of a signal.
     * We use this function to find the sampling point of the spectrum that is
     * closest to the given frequency.
     * 
     * @param inFreq
     *                the input frequency
     * @param stepFreq
     *                the distance between frequency bins
     * 
     * @return the closest frequency bin
     * 
     * @throws IllegalArgumentException
     */
    private double setToNearestFrequencyBin(double inFreq, double stepFreq)
            throws IllegalArgumentException {
        if (stepFreq == 0) {
            throw new IllegalArgumentException("stepFreq is zero");
        }
        return stepFreq * Math.round(inFreq / stepFreq);
    }

    /**
     * Build a mel filterbank with the parameters given. Each filter will be
     * shaped as a triangle. The triangles overlap so that they cover the whole
     * frequency range requested. The edges of a given triangle will be by
     * default at the center of the neighboring triangles.
     * 
     * @param numberFftPoints
     *                number of points in the power spectrum
     * @param numberFilters
     *                number of filters in the filterbank
     * @param minFreq
     *                lowest frequency in the range of interest
     * @param maxFreq
     *                highest frequency in the range of interest
     * 
     * @throws IllegalArgumentException
     */
    private void buildFilterbank(int numberFftPoints, int numberFilters,
            double minFreq, double maxFreq) throws IllegalArgumentException {
        double minFreqMel;
        double maxFreqMel;
        double deltaFreqMel;
        double[] leftEdge = new double[numberFilters];
        double[] centerFreq = new double[numberFilters];
        double[] rightEdge = new double[numberFilters];
        double nextEdgeMel;
        double nextEdge;
        double initialFreqBin;
        double deltaFreq;
        this.filter = new MelFilter[numberFilters];
        /**
         * In fact, the ratio should be between <code>sampleRate /
         * 2</code>
         * and <code>numberFftPoints / 2</code> since the number of points in
         * the power spectrum is half of the number of FFT points - the other
         * half would be symmetrical for a real sequence -, and these points
         * cover up to the Nyquist frequency, which is half of the sampling
         * rate. The two "divide by 2" get canceled out.
         */
        if (numberFftPoints == 0) {
            throw new IllegalArgumentException("Number of FFT points is zero");
        }
        deltaFreq = (double) sampleRate / numberFftPoints;
        /**
         * Initialize edges and center freq. These variables will be updated so
         * that the center frequency of a filter is the right edge of the
         * filter to its left, and the left edge of the filter to its right.
         */
        if (numberFilters < 1) {
            throw new IllegalArgumentException("Number of filters illegal: "
                    + numberFilters);
        }
        minFreqMel = linToMelFreq(minFreq);
        maxFreqMel = linToMelFreq(maxFreq);
        deltaFreqMel = (maxFreqMel - minFreqMel) / (numberFilters + 1);
        leftEdge[0] = setToNearestFrequencyBin(minFreq, deltaFreq);
        nextEdgeMel = minFreqMel;
        for (int i = 0; i < numberFilters; i++) {
            nextEdgeMel += deltaFreqMel;
            nextEdge = melToLinFreq(nextEdgeMel);
            centerFreq[i] = setToNearestFrequencyBin(nextEdge, deltaFreq);
            if (i > 0) {
                rightEdge[i - 1] = centerFreq[i];
            }
            if (i < numberFilters - 1) {
                leftEdge[i + 1] = centerFreq[i];
            }
        }
        nextEdgeMel = nextEdgeMel + deltaFreqMel;
        nextEdge = melToLinFreq(nextEdgeMel);
        rightEdge[numberFilters - 1] = setToNearestFrequencyBin(nextEdge,
                deltaFreq);
        for (int i = 0; i < numberFilters; i++) {
            initialFreqBin = setToNearestFrequencyBin(leftEdge[i], deltaFreq);
            if (initialFreqBin < leftEdge[i]) {
                initialFreqBin += deltaFreq;
            }
            this.filter[i] = new MelFilter(leftEdge[i], centerFreq[i],
                    rightEdge[i], initialFreqBin, deltaFreq);
        }
    }
}

class MelFilter {

	private double[] weight;

	private int initialFreqIndex;

	/**
	 * Constructs a filter from the parameters.
	 * 
	 * In the current implementation, the filter is a bandpass filter with a
	 * triangular shape. We're given the left and right edges and the center
	 * frequency, so we can determine the right and left slopes, which could
	 * be not only assymmetric but completely different. We're also given
	 * the initial frequency, which may or may not coincide with the left
	 * edge, and the frequency step.
	 * 
	 * @param leftEdge
	 *            the filter's lowest passing frequency
	 * @param centerFreq
	 *            the filter's center frequency
	 * @param rightEdge
	 *            the filter's highest passing frequency
	 * @param initialFreq
	 *            the first frequency bin in the pass band
	 * @param deltaFreq
	 *            the step in the frequency axis between frequency bins
	 * 
	 * @throws IllegalArgumentException
	 * 
	 */
	public MelFilter(double leftEdge, double centerFreq, double rightEdge,
			double initialFreq, double deltaFreq)
			throws IllegalArgumentException {

		double filterHeight;
		double leftSlope;
		double rightSlope;
		double currentFreq;
		int indexFilterWeight;
		int numberElementsWeightField;

		if (deltaFreq == 0) {
			throw new IllegalArgumentException("deltaFreq has zero value");
		}
		/**
		 * Check if the left and right boundaries of the filter are too close.
		 */
		if ((Math.round(rightEdge - leftEdge) == 0)
				|| (Math.round(centerFreq - leftEdge) == 0)
				|| (Math.round(rightEdge - centerFreq) == 0)) {
			throw new IllegalArgumentException("Filter boundaries too close");
		}
		/**
		 * Let's compute the number of elements we need in the
		 * <code>weight</code> field by computing how many frequency bins we
		 * can fit in the current frequency range.
		 */
		numberElementsWeightField = (int) Math.round((rightEdge - leftEdge)
				/ deltaFreq + 1);
		/**
		 * Initialize the <code>weight</code> field.
		 */
		if (numberElementsWeightField == 0) {
			throw new IllegalArgumentException("Number of elements in mel"
					+ " is zero.");
		}
		weight = new double[numberElementsWeightField];

		/**
		 * Let's make the filter area equal to 1.
		 */
		filterHeight = 2.0f / (rightEdge - leftEdge);

		/**
		 * Now let's compute the slopes based on the height.
		 */
		leftSlope = filterHeight / (centerFreq - leftEdge);
		rightSlope = filterHeight / (centerFreq - rightEdge);

		/**
		 * Now let's compute the weight for each frequency bin. We initialize
		 * and update two variables in the <code>for</code> line.
		 */
		for (currentFreq = initialFreq, indexFilterWeight = 0; currentFreq <= rightEdge; currentFreq += deltaFreq, indexFilterWeight++) {
			/**
			 * A straight line that contains point <b>(x0, y0)</b> and has
			 * slope <b>m</b> is defined by:
			 * 
			 * <b>y = y0 + m * (x - x0)</b>
			 * 
			 * This is used for both "sides" of the triangular filter below.
			 */
			if (currentFreq < centerFreq) {
				weight[indexFilterWeight] = leftSlope
						* (currentFreq - leftEdge);
			} else {
				weight[indexFilterWeight] = filterHeight + rightSlope
						* (currentFreq - centerFreq);
			}
		}
		
		/**
		 * Initializing frequency related fields.
		 */
		this.initialFreqIndex = (int) Math.round(initialFreq / deltaFreq);
	}

	/**
	 * Compute the output of a filter. We're given a power spectrum, to
	 * which we apply the appropriate weights.
	 * 
	 * @param spectrum
	 *            the input power spectrum to be filtered
	 * 
	 * @return the filtered value, in fact a weighted average of power in
	 *         the frequency range of the filter pass band
	 */
	public float filterOutput(float[] spectrum) {
		float output = 0.0f;
		int indexSpectrum;

		for (int i = 0; i < this.weight.length; i++) {
			indexSpectrum = this.initialFreqIndex + i;
			if (indexSpectrum < spectrum.length) {
				output += spectrum[indexSpectrum] * this.weight[i];
			}
		}
		return output;
	}
}