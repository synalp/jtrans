/**
 * Copyright (C) 2009 Future Invent Informationsmanagement GmbH. All rights
 * reserved. <http://www.fuin.org/>
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.fuin.utils4j;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 * A random access based file output stream.
 */
public class RandomAccessFileOutputStream extends OutputStream {

    private final RandomAccessFile file;

    private long counter = 0;

    /**
     * Constructor with file.
     * 
     * @param file
     *            File the stream operates on - Cannot be <code>null</code>.
     * @param mode
     *            Access mode, as described in <code>RandomAccessFile</code> -
     *            Cannot be <code>null</code>.
     * 
     * @throws FileNotFoundException
     *             If the mode is "r" but the given file object does not denote
     *             an existing regular file, or if the mode begins with "rw" but
     *             the given file object does not denote an existing, writable
     *             regular file and a new regular file of that name cannot be
     *             created, or if some other error occurs while opening or
     *             creating the file.
     */
    public RandomAccessFileOutputStream(final File file, final String mode)
            throws FileNotFoundException {
        super();
        Utils4J.checkNotNull("file", file);
        Utils4J.checkNotNull("mode", mode);
        this.file = new RandomAccessFile(file, mode);
    }

    /**
     * Constructor with output stream. The new stream shares the
     * <code>RandomAccessFile</code> with the argument. Be aware that closing
     * this stream will also close the file used by the argument!
     * 
     * @param out
     *            The <code>RandomAccessFile</code> instance from this argument
     *            will be used - Cannot be <code>null</code>.
     */
    public RandomAccessFileOutputStream(final RandomAccessFileOutputStream out) {
        super();
        Utils4J.checkNotNull("out", out);
        file = out.getRandomAccessFile();
    }

    /**
     * Constructor with input stream. The new stream shares the
     * <code>RandomAccessFile</code> with the argument. Be aware that closing
     * this stream will also close the file used by the argument!
     * 
     * @param in
     *            The <code>RandomAccessFile</code> instance from this argument
     *            will be used - Cannot be <code>null</code>.
     */
    public RandomAccessFileOutputStream(final RandomAccessFileInputStream in) {
        super();
        Utils4J.checkNotNull("in", in);
        file = in.getRandomAccessFile();
    }

    /**
     * Writes the specified byte to this file. The write starts at the current
     * file pointer.
     * 
     * @param b
     *            the <code>byte</code> to be written.
     * 
     * @exception IOException
     *                if an I/O error occurs.
     */
    public final void write(final int b) throws IOException {
        file.write(b);
        counter++;
    }

    /**
     * Writes <code>b.length</code> bytes from the specified byte array to this
     * file, starting at the current file pointer.
     * 
     * @param b
     *            the data.
     * 
     * @exception IOException
     *                if an I/O error occurs.
     */
    public final void write(final byte[] b) throws IOException {
        file.write(b);
        counter = counter + b.length;
    }

    /**
     * Writes <code>len</code> bytes from the specified byte array starting at
     * offset <code>off</code> to the file.
     * 
     * @param b
     *            the data.
     * @param off
     *            the start offset in the data.
     * @param len
     *            the number of bytes to write.
     * 
     * @exception IOException
     *                If an I/O error occurs.
     */
    public final void write(final byte[] b, final int off, final int len) throws IOException {
        file.write(b, off, len);
        counter = counter + len;
    }

    /**
     * Calls <code>sync()</code> of the underlying file's descriptor.
     * 
     * @throws IOException
     *             If an I/O error occurs.
     */
    public final void flush() throws IOException {
        file.getChannel().force(true);
    }

    /**
     * Closes the underlying file and sets the length to the current position.
     * 
     * @throws IOException
     *             If an I/O error occurs.
     */
    public final void close() throws IOException {
        file.close();
    }

    /**
     * Returns the channel used by the random access file.
     * 
     * @return Channel.
     */
    public final FileChannel getChannel() {
        return file.getChannel();
    }

    /**
     * Returns the underlying file.
     * 
     * @return Random access file.
     */
    final RandomAccessFile getRandomAccessFile() {
        return file;
    }

    /**
     * Sets the file-pointer offset, measured from the beginning of this file,
     * at which the next read or write occurs. The offset may be set beyond the
     * end of the file. Setting the offset beyond the end of the file does not
     * change the file length. The file length will change only by writing after
     * the offset has been set beyond the end of the file.
     * 
     * @param pos
     *            the offset position, measured in bytes from the beginning of
     *            the file, at which to set the file pointer.
     * 
     * @exception IOException
     *                if <code>pos</code> is less than <code>0</code> or if an
     *                I/O error occurs.
     */
    public final void seek(final long pos) throws IOException {
        file.seek(pos);
    }

    /**
     * Sets the length of this file.
     * 
     * @param newLength
     *            The desired length of the file
     * 
     * @exception IOException
     *                If an I/O error occurs
     */
    public final void setLength(final long newLength) throws IOException {
        file.setLength(newLength);
    }

    /**
     * Sets the internal counter to <code>0</code>.
     */
    public final void resetCounter() {
        counter = 0;
    }

    /**
     * Returns the number of bytes written since start or since last call to
     * <code>resetCounter()</code>.
     * 
     * @return Number of bytes written.
     */
    public final long getCounter() {
        return counter;
    }

    /**
     * Sets the length of the file to the number of written bytes. This is the
     * same as calling <code>setLength(getCounter())</code>.
     * 
     * @throws IOException
     *             Error setting the file length.
     */
    public final void truncate() throws IOException {
        file.setLength(counter);
    }

    /**
     * Lock the file.
     * 
     * @param tryLockMax
     *            Number of tries to lock before throwing an exception.
     * @param tryWaitMillis
     *            Milliseconds to sleep between retries.
     * 
     * @return FileLock.
     * 
     * @throws LockingFailedException
     *             Locking the file failed.
     */
    public final FileLock lock(final int tryLockMax, final long tryWaitMillis)
            throws LockingFailedException {

        return Utils4J.lockRandomAccessFile(file, tryLockMax, tryWaitMillis);

    }

}
