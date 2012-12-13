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
 * Defines a filter with a regular expression on a property.
 */
public class RegExprPropertyFilter extends PropertyFilter {

	private final RegExprFilter filter;

	/**
	 * Constructor with property and pattern.
	 * 
	 * @param newPropertyName Name of the property.
	 * @param pattern Pattern to use (Cannot be NULL!)
	 */
	public RegExprPropertyFilter(final String newPropertyName,
			final String pattern) {
		super(newPropertyName);
		this.filter = new RegExprFilter(pattern);
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
	 * Returns the type of the matching.
	 * 
	 * @return Type (0,1,2)
	 */
	public final int getType() {
		return filter.getType();
	}

	/**
	 * Sets the type of the matching.
	 * 
	 * @param type Type (0,1,2)
	 */
	public final void setType(final int type) {
		filter.setType(type);
	}

	/**
	 * Returns the type name of the matching.
	 * 
	 * @return Type name ("matches", "lookingAt" or "find")
	 */
	public final String getTypeName() {
		return filter.getTypeName();
	}

	/**
	 * Sets the type name of the matching.
	 * 
	 * @param typeName Type name ("matches", "lookingAt" or "find")
	 */
	public final void setTypeName(final String typeName) {
		filter.setTypeName(typeName);
	}

	/**
	 * Return the pattern.
	 * 
	 * @return Pattern
	 */
	public final String getPattern() {
		return filter.getPattern();
	}

}
