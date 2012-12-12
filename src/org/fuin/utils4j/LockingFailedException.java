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

/**
 * Getting a lock failed.
 */
public final class LockingFailedException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor with message.
     * 
     * @param message
     *            Error message.
     */
    public LockingFailedException(final String message) {
        super(message);
    }

    /**
     * Constructor with message.
     * 
     * @param message
     *            Error message.
     * @param cause
     *            Original error.
     */
    public LockingFailedException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
