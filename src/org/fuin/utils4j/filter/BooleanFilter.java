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
 * Defines a filter on a Boolean property value.
 */
public class BooleanFilter implements Filter {

	private final Boolean constValue;

	/**
	 * Constructor with all necessary values.
	 * 
	 * @param constValue Value the property is compared with.
	 */
	public BooleanFilter(final Boolean constValue) {
		super();
		this.constValue = constValue;
	}

	/**
	 * Returns the value the property is compared with.
	 * 
	 * @return Value.
	 */
	public final Boolean getConstValue() {
		return constValue;
	}

	/**
	 * {@inheritDoc}
	 */
	public final boolean complies(final Object value) {
		if (value == null) {
			return (constValue == null);
		} else {
			return value.equals(constValue);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public final String toString() {
		return " = " + constValue.toString();
	}

}
