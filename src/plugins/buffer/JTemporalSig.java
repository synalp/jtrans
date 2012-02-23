/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.buffer;

public interface JTemporalSig {
	public short[] getSamples();
	public void rewind();
	public float getFrameRate();
}
