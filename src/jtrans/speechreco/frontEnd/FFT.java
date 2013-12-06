/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package jtrans.speechreco.frontEnd;

public class FFT implements FrontEnd {
    private FrontEnd predecessor;
    
	private int numberFftPoints = 512;
    private int logBase2NumberFftPoints;
    private int numberDataPoints;

    private Complex[] weightFft;
    private Complex[] inputFrame;
    private Complex[] from;
    private Complex[] to;

    private Complex weightFftTimesFrom2;
    private Complex tempComplex;
    
	public FFT(FrontEnd win) {
		predecessor = win;
		numberDataPoints = win.getNcoefs();
		numberFftPoints = getNumberFftPoints(numberDataPoints);
		init();
	}
	public FFT(int nbPoints) {
		numberFftPoints=nbPoints;
		init();
	}
	public int getNcoefs() {
		return (numberFftPoints >> 1) + 1;
	}
	public void init() {
		computeLogBase2(numberFftPoints);
		createWeightFft(numberFftPoints, false);
        initComplexArrays();
        weightFftTimesFrom2 = new Complex();
		tempComplex = new Complex();
	}

    /**
	 * Initialize all the Complex arrays that will be necessary for FFT.
	 */
	private void initComplexArrays() {
		inputFrame = new Complex[numberFftPoints];
		from = new Complex[numberFftPoints];
		to = new Complex[numberFftPoints];

		for (int i = 0; i < numberFftPoints; i++) {
			inputFrame[i] = new Complex();
			from[i] = new Complex();
			to[i] = new Complex();
		}
	}

	public Object nextSynchro(int t) {
    	// TODO
    	System.err.println("nextSynchro not supported for now in Deltas !!");
    	return null;
	}
	
    public float[] getOneVector() {
    	float[] input = predecessor.getOneVector();
		if (input != null) {
			if (numberFftPoints==0) {
				/*
				 * If numberFftPoints is not set by the user, figure out the
				 * numberFftPoints and initialize the data structures
				 * appropriately.
				 */
				if (numberDataPoints != input.length) {
					numberDataPoints = input.length;
					numberFftPoints = getNumberFftPoints(numberDataPoints);
					init();
				}
			} else {
				/*
				 * Warn if the user-set numberFftPoints is not ideal.
				 */
				if (numberDataPoints != input.length) {
					numberDataPoints = input.length;
					int idealFftPoints = getNumberFftPoints(numberDataPoints);
					if (idealFftPoints != numberFftPoints) {
						System.err.println("User set numberFftPoints ("
								+ numberFftPoints + ") is not ideal ("
								+ idealFftPoints + ")");
					}
				}
			}
			input = process(input);
		}
	    return input;
	}
    
    /**
     * Returns the ideal number of FFT points given the number of samples.
     * The ideal number of FFT points is the closest power of 2 that is 
     * equal to or larger than the number of samples in the incoming window.
     *
     * @param numberSamples the number of samples in the incoming window
     *
     * @return the closest power of 2 that is equal to or larger than
     *         the number of samples in the incoming window
     */
    private final static int getNumberFftPoints(int numberSamples) {
        int fftPoints = 1;
        
        while (fftPoints < numberSamples) {
            fftPoints *= 2;
            if (fftPoints < 1 || fftPoints > Integer.MAX_VALUE) {
                throw new Error("Invalid # of FFT points: " + fftPoints);
            }
        }
        return fftPoints;
    }

    private float[] process(float[] in) throws IllegalArgumentException {

		/**
		 * Create complex input sequence equivalent to the real input sequence.
		 * If the number of points is less than the window size, we incur in
		 * aliasing. If it's greater, we pad the input sequence with zeros.
		 */
		if (numberFftPoints < in.length) {
			int i = 0;
			for (; i < numberFftPoints; i++) {
				inputFrame[i].set(in[i], 0.0f);
			}
			for (; i < in.length; i++) {
				tempComplex.set(in[i], 0.0f);
				inputFrame[i % numberFftPoints].addComplex(inputFrame[i
						% numberFftPoints], tempComplex);
			}
		} else {
			int i = 0;
			for (; i < in.length; i++) {
				inputFrame[i].set(in[i], 0.0f);
			}
			for (; i < numberFftPoints; i++) {
				inputFrame[i].reset();
			}
		}
		/**
		 * Create output sequence.
		 */
		float[] outputSpectrum = new float[(numberFftPoints >> 1) + 1];

		/**
		 * Start Fast Fourier Transform recursion
		 */
		recurseFft(inputFrame, outputSpectrum, numberFftPoints, false);
		/**
		 * Return the power spectrum
		 */
		/*
		float [] powerSpectrum = new float[outputSpectrum.length/2];
		for (int i=0;i<powerSpectrum.length;i++) {
			powerSpectrum[i] = outputSpectrum[2*i]*outputSpectrum[2*i] + outputSpectrum[2*i+1]*outputSpectrum[2*i+1]; 
		}
		*/
		return outputSpectrum;
	}

    /**
	 * Establish the recursion. The FFT computation will be computed by as a
	 * recursion. Each stage in the butterfly will be fully computed during
	 * recursion. In fact, we use the mechanism of recursion only because it's
	 * the simplest way of switching the "input" and "output" vectors. The
	 * output of a stage is the input to the next stage. The butterfly computes
	 * elements in place, but we still need to switch the vectors. We could copy
	 * it (not very efficient...) or, in C, switch the pointers. We can avoid
	 * the pointers by using recursion.
	 * 
	 * @param input
	 *            input sequence
	 * @param output
	 *            output sequence
	 * @param numberFftPoints
	 *            number of points in the FFT
	 * @param invert
	 *            whether it's direct (false) or inverse (true) FFT
	 * 
	 */
	private void recurseFft(Complex[] input, float[] output,
			int numberFftPoints, boolean invert) {

		float divisor;

		/**
		 * The direct and inverse FFT are essentially the same algorithm, except
		 * for two difference: a scaling factor of "numberFftPoints" and the
		 * signal of the exponent in the weightFft vectors, defined in the
		 * method <code>createWeightFft</code>.
		 */

		if (!invert) {
			divisor = (float)1.0;
		} else {
			divisor = (float) numberFftPoints;
		}

		/**
		 * Initialize the "from" and "to" variables.
		 */
		for (int i = 0; i < numberFftPoints; i++) {
			to[i].reset();
			from[i].scaleComplex(input[i], divisor);
		}

		/**
		 * Repeat the recursion log2(numberFftPoints) times, i.e., we have
		 * log2(numberFftPoints) butterfly stages.
		 */
		butterflyStage(from, to, numberFftPoints, numberFftPoints >> 1);

		/**
		 * Compute energy ("float") for each frequency point from the fft
		 * ("complex")
		 */
		if ((this.logBase2NumberFftPoints & 1) == 0) {
			for (int i = 0; i <= (numberFftPoints >> 1); i++) {
				output[i] = (float)from[i].squaredMagnitudeComplex();
			}
		} else {
			for (int i = 0; i <= (numberFftPoints >> 1); i++) {
				output[i] = (float)to[i].squaredMagnitudeComplex();
			}
		}
		return;
	}
    /**
	 * Compute one stage in the FFT butterfly. The name "butterfly" appears
	 * because this method computes elements in pairs, and a flowgraph of the
	 * computation (output "0" comes from input "0" and "1" and output "1" comes
	 * from input "0" and "1") resembles a butterfly.
	 * 
	 * We repeat <code>butterflyStage</code> for <b>log_2(numberFftPoints)</b>
	 * stages, by calling the recursion with the argument
	 * <code>currentDistance</code> divided by 2 at each call, and checking if
	 * it's still > 0.
	 * 
	 * @param from
	 *            the input sequence at each stage
	 * @param to
	 *            the output sequence
	 * @param numberFftPoints
	 *            the total number of points
	 * @param currentDistance
	 *            the "distance" between elements in the butterfly
	 */
	private void butterflyStage(Complex[] from, Complex[] to,
			int numberFftPoints, int currentDistance) {
		int ndx1From;
		int ndx2From;
		int ndx1To;
		int ndx2To;
		int ndxWeightFft;
		if (currentDistance > 0) {

			int twiceCurrentDistance = 2 * currentDistance;

			for (int s = 0; s < currentDistance; s++) {
				ndx1From = s;
				ndx2From = s + currentDistance;
				ndx1To = s;
				ndx2To = s + (numberFftPoints >> 1);
				ndxWeightFft = 0;
				while (ndxWeightFft < (numberFftPoints >> 1)) {
					/**
					 * <b>weightFftTimesFrom2 = weightFft[k] </b> <b>
					 * *from[ndx2From]</b>
					 */
					weightFftTimesFrom2.multiplyComplex(
							weightFft[ndxWeightFft], from[ndx2From]);
					/**
					 * <b>to[ndx1To] = from[ndx1From] </b> <b> +
					 * weightFftTimesFrom2</b>
					 */
					to[ndx1To].addComplex(from[ndx1From], weightFftTimesFrom2);
					/**
					 * <b>to[ndx2To] = from[ndx1From] </b> <b> -
					 * weightFftTimesFrom2</b>
					 */
					to[ndx2To].subtractComplex(from[ndx1From],
							weightFftTimesFrom2);
					ndx1From += twiceCurrentDistance;
					ndx2From += twiceCurrentDistance;
					ndx1To += currentDistance;
					ndx2To += currentDistance;
					ndxWeightFft += currentDistance;
				}
			}

			/**
			 * This call'd better be the last call in this block, so when it
			 * returns we go straight into the return line below.
			 * 
			 * We switch the <i>to</i> and <i>from</i> variables, the total
			 * number of points remains the same, and the <i>currentDistance</i>
			 * is divided by 2.
			 */
			butterflyStage(to, from, numberFftPoints, (currentDistance >> 1));
		}
		return;
	}
    
    /**
	 * Initializes the <b>weightFft[]</b> vector.
	 * <p>
	 * <b>weightFft[k] = w ^ k</b>
	 * </p>
	 * where:
	 * <p>
	 * <b>w = exp(-2 * PI * i / N)</b>
	 * </p>
	 * <p>
	 * <b>i</b> is a complex number such that <b>i * i = -1</b> and <b>N</b>
	 * is the number of points in the FFT. Since <b>w</b> is complex, this is
	 * the same as
	 * </p>
	 * <p>
	 * <b>Re(weightFft[k]) = cos ( -2 * PI * k / N)</b>
	 * </p>
	 * <p>
	 * <b>Im(weightFft[k]) = sin ( -2 * PI * k / N)</b>
	 * </p>
	 * 
	 * @param numberFftPoints
	 *            number of points in the FFT
	 * @param invert
	 *            whether it's direct (false) or inverse (true) FFT
	 * 
	 */
    private void createWeightFft(int numberFftPoints, boolean invert) {
		/**
		 * weightFFT will have numberFftPoints/2 complex elements.
		 */
		weightFft = new Complex[numberFftPoints >> 1];

		/**
		 * For the inverse FFT, w = 2 * PI / numberFftPoints;
		 */
		double w = -2 * Math.PI / numberFftPoints;
		if (invert) {
			w = -w;
		}

		for (int k = 0; k < (numberFftPoints >> 1); k++) {
			weightFft[k] = new Complex(Math.cos(w * k), Math.sin(w * k));
		}
	}

    private void computeLogBase2(int numberFftPoints)
			throws IllegalArgumentException {
		this.logBase2NumberFftPoints = 0;
		for (int k = numberFftPoints; k > 1; k >>= 1, this.logBase2NumberFftPoints++) {
			if (((k % 2) != 0) || (numberFftPoints < 0)) {
				throw new IllegalArgumentException("Not a power of 2: "
						+ numberFftPoints);
			}
		}
	}
}
