/* MediaDecoder
 
   Author: Andrew Stubbs (based on some examples from the docs)
 
   This class opens a file, reads the first audio channel it finds, and returns raw audio data.
   
   Usage:
      MediaDecoder decoder = new MediaDecoder("myfile.m4a");
      short[] data;
      while ((data = decoder.readShortData()) != null) {
         // process data here
      }
*/

/*
 * Notes: this mediacodec is a mess because its api has changed, and it's not available on pre-4.3 android
 * A better way is to not compress audio when saving to file.
 */


package fr.xtof54.jtransapp;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.ByteOrder;

import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;

public class MediaDecoder {
    private MediaExtractor extractor = new MediaExtractor();
    private MediaCodec decoder;
    
    private MediaFormat inputFormat;
    
    private ByteBuffer inputBuffer;
    private boolean end_of_input_file;

    private ByteBuffer outputBuffer;
    private int outputBufferIndex = -1;

    public MediaDecoder(String inputFilename) {
	    try {
		    extractor.setDataSource(inputFilename);

		    // Select the first audio track we find.
		    int numTracks = extractor.getTrackCount();
		    for (int i = 0; i < numTracks; ++i) {
			    MediaFormat format = extractor.getTrackFormat(i);
			    String mime = format.getString(MediaFormat.KEY_MIME);
			    if (mime.startsWith("audio/")) {
				    extractor.selectTrack(i);
				    decoder = MediaCodec.createDecoderByType(mime);
				    decoder.configure(format, null, null, 0);
				    inputFormat = format;
				    break;
			    }
		    }

		    if (decoder == null) {
			    throw new IllegalArgumentException("No decoder for file format");
		    }

		    decoder.start();
	    } catch (Exception e) {
		    e.printStackTrace();
	    }
	    end_of_input_file = false;
    }
    
    // Read the raw data from MediaCodec.
    // The caller should copy the data out of the ByteBuffer before calling this again
    // or else it may get overwritten.
    private ByteBuffer readData(BufferInfo info) {
        if (decoder == null) return null;
        
        for (;;) {
            // Read data from the file into the codec.
            if (!end_of_input_file) {
                int inputBufferIndex = decoder.dequeueInputBuffer(10000);
                if (inputBufferIndex >= 0) {
                    int size = extractor.readSampleData(decoder.getInputBuffer(inputBufferIndex), 0); 
                    if (size < 0) {
                        // End Of File
                        decoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        end_of_input_file = true;
                    } else {
                        decoder.queueInputBuffer(inputBufferIndex, 0, size, extractor.getSampleTime(), 0);
                        extractor.advance();
                    }
                }
            }

            // Read the output from the codec.
            if (outputBufferIndex >= 0)
                // Ensure that the data is placed at the start of the buffer
		decoder.getOutputBuffer(outputBufferIndex).position(0);
                
            outputBufferIndex = decoder.dequeueOutputBuffer(info, 10000);
            if (outputBufferIndex >= 0) {
                // Handle EOF
                if (info.flags != 0) {
                    decoder.stop();
                    decoder.release();
                    decoder = null;
                    return null;
                }
                
                // Release the buffer so MediaCodec can use it again.
                // The data should stay there until the next time we are called.
                decoder.releaseOutputBuffer(outputBufferIndex, false);
                
                return decoder.getOutputBuffer(outputBufferIndex);
                
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // This usually happens once at the start of the file.
            }
        }
    }
    
    // Return the Audio sample rate, in samples/sec.
    public int getSampleRate() {
        return inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
    }
    
    // Read the raw audio data in 16-bit format
    // Returns null on EOF
    public short[] readShortData() {
        BufferInfo info = new BufferInfo();
        ByteBuffer data = readData(info);
        
        if (data == null)
            return null;
        
        int samplesRead = info.size/2;
        short[] returnData = new short[samplesRead];
        
        // Converting the ByteBuffer to an array doesn't actually make a copy
        // so we must do so or it will be overwritten later.
        // System.arraycopy(data.asShortBuffer().array(), 0, returnData, 0, samplesRead);
	ByteBuffer bb = decoder.getOutputBuffer(outputBufferIndex).order(ByteOrder.LITTLE_ENDIAN);
	System.out.println("detjtrapp bytebuf "+bb.remaining()+" "+bb.position()+" "+bb.capacity());
	ShortBuffer bs = bb.asShortBuffer();
	bs.get(returnData);
        return returnData;
    }

    short[] getSamplesForChannel(MediaCodec codec, int bufferId, int channelIx) {
	    ByteBuffer outputBuffer = codec.getOutputBuffer(bufferId);
	    MediaFormat format = codec.getOutputFormat(bufferId);
	    ShortBuffer samples = outputBuffer.order(ByteOrder.nativeOrder()).asShortBuffer();
	    int numChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
	    if (channelIx < 0 || channelIx >= numChannels) {
		    return null;
	    }
	    short[] res = new short[samples.remaining() / numChannels];
	    for (int i = 0; i < res.length; ++i) {
		    res[i] = samples.get(i * numChannels + channelIx);
	    }
	    return res;
    }

    private static short[] conv2short(ByteBuffer rgbBuf) {
	short[] shortArray = new short[rgbBuf.capacity() / 2];
	byte[] tmp = new byte[rgbBuf.remaining()];
	for (int i=0;i<tmp.length;i++) tmp[i]=rgbBuf.get();
	
	for (int i=0; i<shortArray.length; i++) {   
		ByteBuffer bb = ByteBuffer.allocate(2);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		bb.put(tmp[2*i]);
		bb.put(tmp[2*i + 1]);
		shortArray[i] = bb.getShort(0);
	}
	return shortArray;
    }
}

