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
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

/**
 * A factory for creating root nodes using the
 * <code>PropertiesFilePreferences</code> implementation.
 */
public final class PropertiesFilePreferencesFactory implements PreferencesFactory {

    /**
     * System property key used containing the path and name of the "system"
     * preferences directory.
     */
    public static final String SYSTEM_PREF_DIR = "PropertiesFilePreferences.SystemDir";

    /**
     * System property key used containing the path and name of the "user"
     * preferences directory.
     */
    public static final String USER_PREF_DIR = "PropertiesFilePreferences.UserDir";

    private final String systemPrefDir;

    private final String userPrefDir;

    private PropertiesFilePreferences systemRoot = null;

    private PropertiesFilePreferences userRoot = null;

    /**
     * Default constructor.
     */
    public PropertiesFilePreferencesFactory() {
        this(System.getProperty(SYSTEM_PREF_DIR), System.getProperty(USER_PREF_DIR));
    }

    /**
     * Constructor with path and filenames of the system and user root
     * directories.
     * 
     * @param systemPrefDir
     *            Path and name of the "system" preferences directory.
     * @param userPrefDir
     *            Path and name of the "user" preferences directory.
     */
    public PropertiesFilePreferencesFactory(final String systemPrefDir, final String userPrefDir) {
        super();
        this.systemPrefDir = systemPrefDir;
        this.userPrefDir = userPrefDir;
    }

    /**
     * {@inheritDoc}
     */
    public final synchronized Preferences systemRoot() {
        if (systemRoot == null) {
            systemRoot = new PropertiesFilePreferences(getValidDir(SYSTEM_PREF_DIR, systemPrefDir));
            // Always sync at shutdown
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    try {
                        systemRoot.flush();
                    } catch (final BackingStoreException ex) {
                        System.err.println("Failed to save 'systemRoot' preferences!");
                        ex.printStackTrace(System.err);
                    }
                }
            });
        }
        return systemRoot;
    }

    /**
     * {@inheritDoc}
     */
    public final synchronized Preferences userRoot() {
        if (userRoot == null) {
            userRoot = new PropertiesFilePreferences(getValidDir(USER_PREF_DIR, userPrefDir));
            // Always sync at shutdown
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    try {
                        userRoot.flush();
                    } catch (final BackingStoreException ex) {
                        System.err.println("Failed to save 'userRoot' preferences!");
                        ex.printStackTrace(System.err);
                    }
                }
            });
        }
        return userRoot;
    }

    /**
     * Checks if the system variable is set and is a valid directory. If this is
     * not the case a {@link RuntimeException} will be thrown.
     * 
     * @param varName
     *            Name of the system variable.
     * @param dirName
     *            Name of the directory (from the system variable).
     * 
     * @return Directory reference.
     */
    private File getValidDir(final String varName, final String dirName) {
        if (dirName == null) {
            throw new RuntimeException("The system variable '" + varName + "' is not set!");
        }
        final File dir = new File(dirName);
        if (!dir.exists()) {
            throw new IllegalArgumentException("The directory '" + dir
                    + "' does not exist! [system variable '" + varName + "']");
        }
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("The name '" + dir
                    + "' is not a directory! [system variable '" + varName + "']");
        }
        return dir;
    }

}
