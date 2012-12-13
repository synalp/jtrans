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
 * Defines a filter on an Integer value.
 */
public class IntegerFilter extends ComparableFilter {

	/**
	 * Constructor with a string and a value.
	 * 
	 * @param newOperator Operator to use.
	 * @param constValue Integer value to compare with.
	 */
	public IntegerFilter(final String newOperator, final Integer constValue) {
		super(ComparableFilter.Operator.getInstance(newOperator), constValue);
	}

	/**
	 * Constructor with an operator and a value.
	 * 
	 * @param newOperator Operator to use.
	 * @param constValue Integer value to compare with.
	 */
	public IntegerFilter(final Operator newOperator, final Integer constValue) {
		super(newOperator, constValue);
	}

	/**
	 * Returns the constant value any value is compared with.
	 * 
	 * @return Integer value.
	 */
	public final Integer getConstValue() {
		return (Integer) getConstValueIntern();
	}

}
