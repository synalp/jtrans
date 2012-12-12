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
 * Thrown to indicate that a method has been passed an illegal <code>null</code>
 * argument.
 */
public final class IllegalNullArgumentException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    /** Name of the argument that caused the exception. */
    private final String argument;

    /**
     * Constructor with argument name.
     * 
     * @param argument
     *            Name of the argument that caused the exception.
     */
    public IllegalNullArgumentException(final String argument) {
        super("The argument '" + argument + "' cannot be null!");
        this.argument = argument;
    }

    /**
     * Returns the name of the argument that caused the exception.
     * 
     * @return Argument name.
     */
    public final String getArgument() {
        return argument;
    }

}
