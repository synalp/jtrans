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
package org.fuin.utils4j.filter;

import java.util.StringTokenizer;

/**
 * Defines a filter on a token.
 */
public class TokenFilter implements Filter {

	private final String constValue;

	private final String separators;

	/**
	 * Constructor with a constant value and separators.
	 * 
	 * @param constValue Value to compare with.
	 * @param newSeparators Separators
	 */
	public TokenFilter(final String constValue, final String newSeparators) {
		super();
		this.constValue = constValue;
		this.separators = newSeparators;
	}

	/**
	 * Returns a string with one or more separators.
	 * 
	 * @return Separators.
	 */
	public final String getSeparators() {
		return separators;
	}

	/**
	 * Returns the constant value any string is compared with.
	 * 
	 * @return String value.
	 */
	public final String getConstValue() {
		return constValue;
	}

	/**
	 * {@inheritDoc}
	 */
	public final boolean complies(final Object value) {
		return complies((String) value, constValue, separators);
	}

	/**
	 * Helper method for subclasses to do the comparation.
	 * 
	 * @param value Object that contains the property.
	 * @param constValue Value to compare with.
	 * @param separators Separators
	 * 
	 * @return If object property contains the value as one of the tokens TRUE
	 *         else FALSE.
	 */
	protected final boolean complies(final String value,
			final String constValue, final String separators) {
		if (value == null) {
			return false;
		} else {
			final StringTokenizer tok = new StringTokenizer(value, separators);
			while (tok.hasMoreTokens()) {
				final String t = tok.nextToken();
				if (t.equals(constValue)) {
					return true;
				}
			}
			return false;
		}

	}

	/**
	 * {@inheritDoc}
	 */
	public final String toString() {
		return " contains '" + constValue + "' [" + separators + "]";
	}

}
