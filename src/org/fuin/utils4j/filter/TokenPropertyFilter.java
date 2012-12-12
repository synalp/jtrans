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

/**
 * Defines a filter on a String property.
 */
public class TokenPropertyFilter extends PropertyFilter {

	private final TokenFilter filter;

	/**
	 * Constructor with a property, a constant value and separators.
	 * 
	 * @param newPropertyName Name of the property.
	 * @param constValue Value to compare with.
	 * @param newSeparators Separators
	 */
	public TokenPropertyFilter(final String newPropertyName,
			final String constValue, final String newSeparators) {
		super(newPropertyName);
		this.filter = new TokenFilter(constValue, newSeparators);
	}

	/**
	 * {@inheritDoc}
	 */
	protected final String[] createGetterNames(final String property) {
		return new String[] {"get" + Character.toUpperCase(property.charAt(0))
				+ property.substring(1)};
	}
	
	/**
	 * {@inheritDoc}
	 */
	public final boolean complies(final Object obj) {
		final String value = (String) this.getProperty(obj, this
				.getPropertyName());
		return filter.complies(value);
	}

	/**
	 * {@inheritDoc}
	 */
	public final String toString() {
		return getPropertyName() + filter.toString();
	}

	/**
	 * Returns the constant value any value is compared with.
	 * 
	 * @return String value.
	 */
	public final String getConstValue() {
		return filter.getConstValue();
	}

	/**
	 * Returns a string with one or more separators.
	 * 
	 * @return Separators.
	 */
	public final String getSeparators() {
		return filter.getSeparators();
	}

}
