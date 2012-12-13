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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * A properties file that is capable of merging concurrent changes made by
 * another JVM or another process.
 */
public class PropertiesFile {

    private final File file;

    private final String encoding;

    private final List props;

    private int tryLockMax = 3;

    private long tryWaitMillis = 100;

    private boolean loaded = false;

    /**
     * Constructor with file. Encoding is "UTF-8".
     * 
     * @param file
     *            File reference.
     */
    public PropertiesFile(final File file) {
        this(file, "UTF-8");
    }

    /**
     * Constructor with file and encoding.
     * 
     * @param file
     *            File reference.
     * @param encoding
     *            File encoding ("UTF-8" etc.)
     */
    public PropertiesFile(final File file, final String encoding) {
        super();
        Utils4J.checkNotNull("file", file);
        this.file = file;

        Utils4J.checkNotNull("encoding", encoding);
        this.encoding = encoding;

        this.props = new ArrayList();

    }

    /**
     * Returns the encoding of the file.
     * 
     * @return File encoding ("UTF-8" etc.)
     */
    public final String getEncoding() {
        return encoding;
    }

    /**
     * Returns if the underlying file has ever been read.
     * 
     * @return If the file was loaded <code>true</code> else <code>false</code>.
     */
    public final boolean isLoaded() {
        return loaded;
    }

    /**
     * Discards all properties in memory (not on disk!).
     */
    public final void clear() {
        props.clear();
    }

    /**
     * Loads or reloads the content of the underlying file. Current properties
     * in memory will NOT be discarded! If you want to discard the current
     * values you must call <code>clear()</code> before!
     * 
     * @throws IOException
     *             Error reading the file.
     * @throws LockingFailedException
     *             Locking the file failed.
     * @throws MergeException
     *             A problem occurred when merging the properties in memory and
     *             from disk.
     */
    public final void load() throws IOException, LockingFailedException, MergeException {

        // Load new data from file
        final RandomAccessFileInputStream in = new RandomAccessFileInputStream(file, "rw");
        try {
            final FileLock lock = in.lock(tryLockMax, tryWaitMillis);
            try {
                // TODO Anyone knows a better/faster solution?
                // Just checking "file.lastModified()" failed here on a
                // irregular base so always merging seems to be the best
                // alternative...
                merge(in);
            } finally {
                lock.release();
            }
        } finally {
            in.close();
        }

    }

    private void load(final RandomAccessFileInputStream in, final File file, final List props,
            final String encoding) throws IOException {

        final LineNumberReader reader = new LineNumberReader(new InputStreamReader(
                new BufferedInputStream(in), encoding));
        String line;
        while ((line = reader.readLine()) != null) {
            final int p = line.indexOf('=');
            if (p > -1) {
                final String key = line.substring(0, p);
                final String value = line.substring(p + 1);
                props.add(new Property(key, value, value));
            }
        }
        // We don't close the reader because this will be done by the caller

        loaded = true;
    }

    private void merge(final RandomAccessFileInputStream in) throws MergeException, IOException {

        final List problems = new ArrayList();

        final List currentProps = new ArrayList();
        load(in, file, currentProps, encoding);

        for (int i = 0; i < currentProps.size(); i++) {
            final Property currentProp = (Property) currentProps.get(i);
            final int idx = props.indexOf(currentProp);
            if (idx == -1) {
                // New property from file
                props.add(currentProp);
            } else {
                final Property prop = (Property) props.get(idx);
                if (prop.hasChanged()) {
                    if (prop.isNew()) {
                        // New property
                        if (!prop.getValue().equals(currentProp.getValue())) {
                            problems.add(new MergeException.Problem(
                                    "Same new property in file with a different value!", prop,
                                    currentProp));
                        }
                    } else {
                        if (prop.isDeleted()) {
                            // Deleted property
                            if (!prop.getInitialValue().equals(currentProp.getValue())) {
                                problems.add(new MergeException.Problem(
                                        "Modified property in file we want to delete!", prop,
                                        currentProp));
                            }
                        } else {
                            // Changed property
                            if (!prop.getInitialValue().equals(currentProp.getValue())) {
                                problems.add(new MergeException.Problem(
                                        "Same property modified in file but different value!",
                                        prop, currentProp));
                            }
                        }
                    }
                } else {
                    // No change, simply replace with file
                    props.set(idx, currentProp);
                }
            }
        }

        if (problems.size() > 0) {
            throw new MergeException(file, (MergeException.Problem[]) problems
                    .toArray(new MergeException.Problem[0]));
        }

    }

    /**
     * Save the content from memory to disk.
     * 
     * @param sortByKey
     *            Sort the properties by key before saving?
     * 
     * @throws IOException
     *             Error writing the file.
     * @throws MergeException
     *             One or more properties were modified concurrently.
     * @throws LockingFailedException
     *             Locking the file failed.
     */
    public final void save(final boolean sortByKey) throws IOException, MergeException,
            LockingFailedException {
        save(new String[] {}, sortByKey);
    }

    /**
     * Save the content from memory to disk.
     * 
     * @param comment
     *            Comment to prepend (Should not include the "#" comment sign -
     *            It will be prepended automatically).
     * @param sortByKey
     *            Sort the properties by key before saving?
     * 
     * @throws IOException
     *             Error writing the file.
     * @throws MergeException
     *             One or more properties were modified concurrently.
     * @throws LockingFailedException
     *             Locking the file failed.
     */
    public final void save(final String comment, final boolean sortByKey) throws IOException,
            MergeException, LockingFailedException {
        save(new String[] { comment }, sortByKey);
    }

    /**
     * Save the content from memory to disk.
     * 
     * @param comments
     *            Comments to prepend (Should not include the "#" comment sign -
     *            It will be prepended automatically).
     * @param sortByKey
     *            Sort the properties by key before saving?
     * 
     * @throws IOException
     *             Error writing the file.
     * @throws MergeException
     *             One or more properties were modified concurrently.
     * @throws LockingFailedException
     *             Locking the file failed.
     */
    public final void save(final String[] comments, final boolean sortByKey) throws IOException,
            MergeException, LockingFailedException {

        final RandomAccessFileOutputStream out = new RandomAccessFileOutputStream(file, "rw");
        try {
            final FileLock lock = out.lock(tryLockMax, tryWaitMillis);
            try {

                // TODO Anyone knows a better/faster solution?
                // Just checking "file.lastModified()" failed here on a
                // irregular base so always merging seems to be the best
                // alternative...
                merge(new RandomAccessFileInputStream(out));
                out.seek(0);
                out.resetCounter();

                // Sort?
                if (sortByKey) {
                    Collections.sort(props);
                }

                // Write the data to disk
                final BufferedOutputStream bout = new BufferedOutputStream(out);
                final Writer writer = new OutputStreamWriter(bout, encoding);
                final String lf = System.getProperty("line.separator");

                // Write comment
                for (int i = 0; i < comments.length; i++) {
                    writer.write("# ");
                    writer.write(comments[i]);
                    writer.write(lf);
                }

                // Save all values
                for (int i = 0; i < props.size(); i++) {
                    final Property prop = (Property) props.get(i);
                    if (!prop.isDeleted()) {
                        writer.write(prop.toKeyValue());
                        writer.write(lf);
                        // Replace the property with the new status
                        props.set(i, new Property(prop.getKey(), prop.getValue(), prop.getValue()));
                    }
                }
                writer.flush();
                out.truncate();
                out.flush();

            } finally {
                lock.release();
            }
        } finally {
            out.close();
        }

        // Remove all deleted entries
        for (int i = props.size() - 1; i >= 0; i--) {
            final Property prop = (Property) props.get(i);
            if (prop.isDeleted()) {
                props.remove(i);
            }
        }

    }

    private Property find(final String key) {
        for (int i = 0; i < props.size(); i++) {
            final Property prop = (Property) props.get(i);
            if (prop.getKey().equals(key)) {
                return prop;
            }
        }
        return null;
    }

    /**
     * Returns a value for a given key.
     * 
     * @param key
     *            Key to find.
     * 
     * @return Value or <code>null</code> if the key is unknown.
     */
    public final String get(final String key) {
        final Property prop = find(key);
        if (prop == null) {
            return null;
        }
        return prop.getValue();
    }

    /**
     * Returns a status text for a given key.
     * 
     * @param key
     *            Key to find.
     * 
     * @return Status text or <code>null</code> if the key is unknown.
     */
    public final String getStatus(final String key) {
        final Property prop = find(key);
        if (prop == null) {
            return null;
        }
        return prop.getStatus();
    }

    /**
     * Set a value for a property. If a property with the key is already known
     * the value will be changed. Otherwise a new property will be created.
     * 
     * @param key
     *            Key to set.
     * @param value
     *            Value to set.
     */
    public final void put(final String key, final String value) {
        final Property prop = find(key);
        if (prop == null) {
            props.add(new Property(key, null, value));
        } else {
            prop.setValue(value);
        }
    }

    /**
     * Remove the property with the given key. The internal property object is
     * not deleted itself but it's value is set to <code>null</code> and the
     * method <code>isDeleted()</code> will return <code>true</code>.
     * 
     * @param key
     *            Key for the property to remove.
     */
    public final void remove(final String key) {
        final Property prop = find(key);
        if (prop != null) {
            prop.setValue(null);
        }
    }

    /**
     * Returns if a property has been deleted.
     * 
     * @param key
     *            Key for the property to check.
     * 
     * @return If the property is unknown or has been deleted <code>true</code>
     *         else <code>false</code>.
     */
    public final boolean isRemoved(final String key) {
        final Property prop = find(key);
        if (prop == null) {
            return true;
        }
        return prop.isDeleted();
    }

    /**
     * Number of properties.
     * 
     * @return All known properties including deleted ones.
     */
    public final int size() {
        return props.size();
    }

    /**
     * Returns the underlying file.
     * 
     * @return Properties file reference.
     */
    public final File getFile() {
        return file;
    }

    /**
     * Returns a list of all known keys including the deleted ones.
     * 
     * @return List of keys.
     */
    public final List getKeyList() {
        final List keys = new ArrayList();
        final Iterator it = keyIterator();
        while (it.hasNext()) {
            keys.add(it.next());
        }
        return keys;
    }

    /**
     * Returns an array of all known keys including the deleted ones.
     * 
     * @return Array of keys.
     */
    public final String[] getKeyArray() {
        return (String[]) getKeyList().toArray(new String[0]);
    }

    /**
     * Returns a key iterator.
     * 
     * @return Iterates over all keys including the deleted ones.
     */
    public final Iterator keyIterator() {
        return new Iterator() {
            private final Iterator it = props.iterator();

            public boolean hasNext() {
                return it.hasNext();
            }

            public Object next() {
                final Property prop = (Property) it.next();
                return prop.getKey();
            }

            public void remove() {
                it.remove();
            }
        };
    }

    /**
     * Returns a copy of all properties.
     * 
     * @return All key/values without deleted ones.
     */
    public final Properties toProperties() {
        final Properties retVal = new Properties();
        for (int i = 0; i < props.size(); i++) {
            final Property prop = (Property) props.get(i);
            if (!prop.isDeleted()) {
                retVal.put(prop.getKey(), prop.getValue());
            }
        }
        return retVal;
    }

    /**
     * Returns the number of tries to lock before throwing an exception.
     * 
     * @return Number of tries (default=3).
     */
    public final int getTryLockMax() {
        return tryLockMax;
    }

    /**
     * Sets the number of tries to lock before throwing an exception.
     * 
     * @param tryLockMax
     *            Number of tries (default=3).
     */
    public final void setTryLockMax(final int tryLockMax) {
        this.tryLockMax = tryLockMax;
    }

    /**
     * Returns the milliseconds to sleep between retries.
     * 
     * @return Milliseconds.
     */
    public final long getTryWaitMillis() {
        return tryWaitMillis;
    }

    /**
     * Sets the milliseconds to sleep between retries.
     * 
     * @param tryWaitMillis
     *            Milliseconds.
     */
    public final void setTryWaitMillis(final long tryWaitMillis) {
        this.tryWaitMillis = tryWaitMillis;
    }

    /**
     * Determines if the underlying file already exists.
     * 
     * @return If the file exists <code>true</code> else <code>false</code>
     */
    public final boolean exists() {
        return file.exists();
    }

    /**
     * Tries to delete the underlying file. The properties in memory remain
     * unchanged. If you want also to remove the properties in memory call
     * <code>clear()</code>.
     * 
     * @return If the files was deleted <code>true</code> else
     *         <code>false</code>
     */
    public final boolean delete() {
        return file.delete();
    }

}
