package fr.xtof54.jtransapp;

import edu.cmu.sphinx.frontend.*;
import edu.cmu.sphinx.frontend.feature.DeltasFeatureExtractor;
import edu.cmu.sphinx.frontend.feature.LiveCMN;
import edu.cmu.sphinx.frontend.filter.Dither;
import edu.cmu.sphinx.frontend.filter.Preemphasizer;
import edu.cmu.sphinx.frontend.frequencywarp.MelFrequencyFilterBank;
import edu.cmu.sphinx.frontend.transform.DiscreteCosineTransform;
import edu.cmu.sphinx.frontend.transform.DiscreteFourierTransform;
import edu.cmu.sphinx.frontend.util.AudioFileDataSource;
import edu.cmu.sphinx.frontend.window.RaisedCosineWindower;

import edu.cmu.sphinx.util.props.*;
import edu.cmu.sphinx.frontend.util.DataUtil;

import java.util.ArrayList;
import java.util.List;
import java.io.InputStream;

public class MFCC {

	public static int FRAMES_PER_SECOND = 100;

	private static class AndroidMikeDataSource extends BaseDataProcessor {

		/** The property for the number of bytes to read from the InputStream each time. */
		// default 3200
		@S4Integer(defaultValue = 200)
			public static final String PROP_BYTES_PER_READ = "bytesPerRead";

		protected InputStream dataStream;
		protected int sampleRate;
		protected int bytesPerRead;
		protected int bytesPerValue;
		private long totalValuesRead;
		protected boolean bigEndian;
		protected boolean signedData;
		private boolean streamEndReached;
		private boolean utteranceEndSent;
		private boolean utteranceStarted;

		public AndroidMikeDataSource(int bytesPerRead) {
			initLogger();
			create(bytesPerRead);
		}

		public AndroidMikeDataSource() {
		}

		@Override
		public void newProperties(PropertySheet ps) throws PropertyException {
			super.newProperties(ps);
			logger = ps.getLogger();
			create(ps.getInt(PROP_BYTES_PER_READ));
		}

		private void create( int bytesPerRead) {
			this.bytesPerRead = bytesPerRead;
			initialize();
		}


		@Override
		public void initialize() {
			super.initialize();

			// reset all stream tags
			streamEndReached = false;
			utteranceEndSent = false;
			utteranceStarted = false;

			if (bytesPerRead % 2 == 1) {
				bytesPerRead++;
			}
		}

		/**
		 * Sets the InputStream from which this StreamDataSource reads.
		 *
		 * @param inputStream the InputStream from which audio data comes
		 * @param streamName  the name of the InputStream
		 */
		public void setInputStream(InputStream inputStream, String streamName) {
			dataStream = inputStream;
			streamEndReached = false;
			utteranceEndSent = false;
			utteranceStarted = false;

			sampleRate = 16000;
			bigEndian = false;

			// throw new Error("StreamDataSource: bits per sample must be a multiple of 8.");
			bytesPerValue = 2;
			signedData = true;
			totalValuesRead = 0;
		}


		/**
		 * Reads and returns the next Data from the InputStream of StreamDataSource, return null if no data is read and end
		 * of file is reached.
		 *
		 * @return the next Data or <code>null</code> if none is available
		 * @throws edu.cmu.sphinx.frontend.DataProcessingException
		 *          if there is a data processing error
		 */
		@Override
		public Data getData() throws DataProcessingException {
			Data output = null;
			if (streamEndReached) {
				if (!utteranceEndSent) {
					// since 'firstSampleNumber' starts at 0, the last
					// sample number should be 'totalValuesRead - 1'
					output = createDataEndSignal();
					utteranceEndSent = true;
				}
			} else {
				if (!utteranceStarted) {
					utteranceStarted = true;
					output = new DataStartSignal(sampleRate);
				} else {
					if (dataStream != null) {
						output = readNextFrame();
						if (output == null) {
							if (!utteranceEndSent) {
								output = createDataEndSignal();
								utteranceEndSent = true;
							}
						}
					}
				}
			}
			return output;
		}


		private DataEndSignal createDataEndSignal() {
			return new DataEndSignal(getDuration());
		}


		/**
		 * Returns the next Data from the input stream, or null if there is none available
		 *
		 * @return a Data or null
		 * @throws edu.cmu.sphinx.frontend.DataProcessingException
		 */
		private Data readNextFrame() throws DataProcessingException {
			// read one frame's worth of bytes
			int read;
			int totalRead = 0;
			final int bytesToRead = bytesPerRead;
			byte[] samplesBuffer = new byte[bytesPerRead];
			long firstSample = totalValuesRead;
			try {
				do {
					read = dataStream.read(samplesBuffer, totalRead, bytesToRead - totalRead);
					if (read > 0) {
						totalRead += read;
					}
				} while (read != -1 && totalRead < bytesToRead);
				if (totalRead <= 0) {
					closeDataStream();
					return null;
				}
				// shrink incomplete frames
				totalValuesRead += (totalRead / bytesPerValue);
				if (totalRead < bytesToRead) {
					totalRead = (totalRead % 2 == 0) ? totalRead + 2 : totalRead + 3;
					byte[] shrinkedBuffer = new byte[totalRead];
					System.arraycopy(samplesBuffer, 0, shrinkedBuffer, 0, totalRead);
					samplesBuffer = shrinkedBuffer;
					closeDataStream();
				}
			} catch (Exception ioe) {
				throw new DataProcessingException("Error reading data", ioe);
			}
			// turn it into an Data object
			double[] doubleData;
			if (bigEndian) {
				doubleData = DataUtil.bytesToValues(samplesBuffer, 0, totalRead, bytesPerValue, signedData);
			} else {
				doubleData = DataUtil.littleEndianBytesToValues(samplesBuffer, 0, totalRead, bytesPerValue, signedData);
			}

			long collectTime = firstSample * 1000/sampleRate;
			return new DoubleData(doubleData, sampleRate, collectTime, firstSample);
		}


		private void closeDataStream() throws Exception {
			streamEndReached = true;
			if (dataStream != null) {
				dataStream.close();
			}
		}


		/**
		 * Returns the duration of the current data stream in milliseconds.
		 *
		 * @return the duration of the current data stream in milliseconds
		 */
		private long getDuration() {
			return (long) (((double) totalValuesRead / (double) sampleRate) * 1000.0);
		}


		public int getSampleRate() {
			return sampleRate;
		}


		public boolean isBigEndian() {
			return bigEndian;
		}

		public void setMikeStream(InputStream mike) {
			dataStream = mike;
			sampleRate = 16000;
			bigEndian  = false;
			bytesPerValue = 2;
			signedData = true;
			totalValuesRead = 0;
			super.initialize();

		}
	}


	public static List<FloatData> getMFCC(InputStream audio) {
		AndroidMikeDataSource afds = new AndroidMikeDataSource(200);
		afds.setMikeStream(audio);
		FrontEnd fe = getFrontEnd(true, afds);

		List<FloatData> data = new ArrayList<>();
		for (;;) {
			Data d = fe.getData();
			if (d instanceof DataEndSignal) break;
			try {
				data.add(FloatData.toFloatData(d));
			} catch (IllegalArgumentException ex) {
				// not a FloatData/DoubleData
			}
		}
		System.out.println("Got " + data.size() + " frames");
		return data;
	}

	private static FrontEnd getFrontEnd(boolean withMFCC, DataProcessor... sourceList) {
		ArrayList<DataProcessor> frontEndList = new ArrayList<>();
		for (DataProcessor source: sourceList) {
			if (null != source) {
				frontEndList.add(source);
			}
		}

		frontEndList.add(new Dither(2,false,Double.MAX_VALUE,-Double.MAX_VALUE));
		frontEndList.add(new DataBlocker(50));
		frontEndList.add(new Preemphasizer(0.97));
		frontEndList.add(new RaisedCosineWindower(0.46f, 25.625f, FRAMES_PER_SECOND/10f));
		frontEndList.add(new DiscreteFourierTransform(512, false));

		if (withMFCC) {
			frontEndList.add(new MelFrequencyFilterBank(133.33334, 6855.4976, 40));
			frontEndList.add(new DiscreteCosineTransform(40, 13));
			frontEndList.add(new LiveCMN(12, 100, 160));
			frontEndList.add(new DeltasFeatureExtractor(3));
		}

		return new FrontEnd(frontEndList);
	}
}
