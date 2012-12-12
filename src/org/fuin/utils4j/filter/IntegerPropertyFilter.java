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

import org.fuin.utils4j.filter.ComparableFilter.Operator;

/**
 * Defines a filter on a Integer property.
 */
public class IntegerPropertyFilter extends ComparablePropertyFilter {

	/**
	 * Constructor.
	 * 
	 * @param newPropertyName
	 *            Name of the property.
	 * @param newOperator
	 *            Operator to use.
	 * @param constValue
	 *            Value to compare with.
	 */
	public IntegerPropertyFilter(final String newPropertyName,
			final String newOperator, final Integer constValue) {
		super(newPropertyName, new IntegerFilter(newOperator, constValue));
	}

	/**
	 * Constructor.
	 * 
	 * @param newPropertyName
	 *            Name of the property.
	 * @param newOperator
	 *            Operator to use.
	 * @param constValue
	 *            Value to compare with.
	 */
	public IntegerPropertyFilter(final String newPropertyName,
			final Operator newOperator, final Integer constValue) {
		super(newPropertyName, new IntegerFilter(newOperator, constValue));
	}

	/**
	 * {@inheritDoc}
	 */
	protected final String[] createGetterNames(final String property) {
		return new String[] {"get" + Character.toUpperCase(property.charAt(0))
				+ property.substring(1)};
	}
	
	/**
	 * Returns the constant value any value is compared with.
	 * 
	 * @return Integer value.
	 */
	public final Integer getConstValue() {
		return ((IntegerFilter) getFilter()).getConstValue();
	}

}
