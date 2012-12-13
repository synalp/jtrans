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
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 * A random access based file input stream. The <code>readlimit</code> argument
 * of the <code>mark(int)</code> method is ignored (there is no limit).
 */
public final class RandomAccessFileInputStream extends InputStream {

    private final RandomAccessFile file;

    private long mark = -1;

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
    public RandomAccessFileInputStream(final File file, final String mode)
            throws FileNotFoundException {
        super();
        Utils4J.checkNotNull("file", file);
        Utils4J.checkNotNull("mode", mode);
        this.file = new RandomAccessFile(file, mode);
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
    public RandomAccessFileInputStream(final RandomAccessFileInputStream in) {
        super();
        Utils4J.checkNotNull("in", in);
        file = in.getRandomAccessFile();
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
    public RandomAccessFileInputStream(final RandomAccessFileOutputStream out) {
        super();
        Utils4J.checkNotNull("out", out);
        file = out.getRandomAccessFile();
    }

    /**
     * {@inheritDoc}
     */
    public final int read() throws IOException {
        return file.read();
    }

    /**
     * {@inheritDoc}
     */
    public final int read(final byte[] b) throws IOException {
        return file.read(b);
    }

    /**
     * {@inheritDoc}
     */
    public final int read(final byte[] b, final int off, final int len) throws IOException {
        return file.read(b, off, len);
    }

    /**
     * {@inheritDoc}
     */
    public final long skip(final long n) throws IOException {
        if (n > Integer.MAX_VALUE) {
            return super.skip(Integer.MAX_VALUE);
        }
        return file.skipBytes((int) n);
    }

    /**
     * Returns the size of the underlying file.
     * 
     * @return Number of bytes.
     * 
     * @throws IOException
     *             IOException if an I/O error occurs.
     */
    public final long fileSize() throws IOException {
        return file.length();
    }

    /**
     * {@inheritDoc}
     */
    public final int available() throws IOException {
        if (file.getFilePointer() > file.length()) {
            return 0;
        }
        final long avail = file.length() - file.getFilePointer();
        if (avail > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) avail;
    }

    /**
     * {@inheritDoc}
     */
    public final void close() throws IOException {
        if (file.getChannel().isOpen()) {
            file.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void mark(final int readlimit) {
        if (file.getChannel().isOpen()) {
            try {
                mark = file.getFilePointer();
            } catch (final IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public final synchronized void reset() throws IOException {
        if (mark == -1) {
            throw new IOException("The method 'mark()' has not been called "
                    + "since the stream was created!");
        }
        file.seek(mark);
    }

    /**
     * {@inheritDoc}
     */
    public final boolean markSupported() {
        return true;
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
