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
 * Calling a method with reflection failed. Used to wrap all the different
 * exceptions when using reflection.
 */
public final class InvokeMethodFailedException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor with error message.
     * 
     * @param message
     *            Message.
     */
    public InvokeMethodFailedException(final String message) {
        super(message);
    }

    /**
     * Constructor with error message and cause.
     * 
     * @param message
     *            Message.
     * @param cause
     *            Exception that caused the failure.
     */
    public InvokeMethodFailedException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
