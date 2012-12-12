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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;

/**
 * A directory and <code>PropertiesFile</code> based <code>Preferences</code>
 * API implementation.
 */
public final class PropertiesFilePreferences extends AbstractPreferences {

    /** Filename the properties of this node are stored under. */
    public static final String FILENAME = "preferences.properties";

    private final File dir;

    private final PropertiesFile file;

    private boolean removed;

    /**
     * Constructor with directory. This is constructing the "root" node.
     * 
     * @param dir
     *            Directory where the preferences are stored.
     */
    public PropertiesFilePreferences(final File dir) {
        this(null, dir, "");
    }

    /**
     * Constructor with parent node and directory.
     * 
     * @param parent
     *            Parent node.
     * @param dir
     *            Directory where the preferences are stored.
     */
    public PropertiesFilePreferences(final PropertiesFilePreferences parent, final File dir) {
        this(parent, dir, dir.getName());
    }

    /**
     * Constructor with parent node and directory.
     * 
     * @param parent
     *            Parent node.
     * @param dir
     *            Directory where the preferences are stored.
     * @param name
     *            Name of the node.
     */
    private PropertiesFilePreferences(final PropertiesFilePreferences parent, final File dir,
            final String name) {
        super(parent, name);
        this.dir = dir;
        this.file = new PropertiesFile(new File(dir, FILENAME));
        this.removed = false;
    }

    /**
     * {@inheritDoc}
     */
    protected final AbstractPreferences childSpi(final String name) {
        final File childDir = new File(dir, name);
        return new PropertiesFilePreferences(this, childDir, name);
    }

    /**
     * {@inheritDoc}
     */
    protected final String[] childrenNamesSpi() throws BackingStoreException {
        try {
            final List childs = new ArrayList();
            final File[] files = dir.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    if (files[i].isDirectory()) {
                        childs.add(files[i].getName());
                    }
                }
            }
            return (String[]) childs.toArray(new String[0]);
        } catch (final RuntimeException ex) {
            throw new BackingStoreException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void flushSpi() throws BackingStoreException {
        try {
            if (removed) {
                file.delete();
                dir.delete();
            } else {
                final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                final String[] comments = new String[] { "DO NOT EDIT!",
                        "Created by " + this.getClass().getName(), sdf.format(new Date()) };
                mkdirIfNecessary();
                file.save(comments, true);
            }
        } catch (final Exception ex) {
            throw new BackingStoreException(ex);
        }
    }

    private void mkdirIfNecessary() throws BackingStoreException {
        if (!dir.exists() && !dir.mkdirs()) {
            throw new BackingStoreException("Failed to create directory '" + dir + "'!");
        }
    }

    /**
     * {@inheritDoc}
     */
    protected final String getSpi(final String key) {
        loadIfNecessary();
        return file.get(key);
    }

    /**
     * {@inheritDoc}
     */
    protected final String[] keysSpi() throws BackingStoreException {
        loadIfNecessary();
        return file.getKeyArray();
    }

    /**
     * {@inheritDoc}
     */
    protected final void putSpi(final String key, final String value) {
        loadIfNecessary();
        file.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    protected final void removeNodeSpi() throws BackingStoreException {
        file.clear();
        removed = true;
    }

    /**
     * {@inheritDoc}
     */
    protected final void removeSpi(final String key) {
        loadIfNecessary();
        file.remove(key);
    }

    private void loadIfNecessary() {
        if (!file.isLoaded()) {
            try {
                syncSpi();
            } catch (final BackingStoreException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    protected final void syncSpi() throws BackingStoreException {
        if (dir.exists() && file.exists()) {
            try {
                file.load();
            } catch (final Exception ex) {
                throw new BackingStoreException(ex);
            }
        }
    }

    /**
     * Returns a copy of all properties.
     * 
     * @return All key/values without deleted ones.
     */
    public final Properties toProperties() {
        return file.toProperties();
    }

}
