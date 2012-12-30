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
 * Defines a filter on a Comparable property.
 */
public abstract class ComparablePropertyFilter extends PropertyFilter {

	private final ComparableFilter filter;

	/**
	 * Constructor with a property and an operator.
	 * 
	 * @param newPropertyName Name of the property.
	 * @param filter Filter to use.
	 */
	public ComparablePropertyFilter(final String newPropertyName,
			final ComparableFilter filter) {
		super(newPropertyName);
		this.filter = filter;
	}

	/**
	 * {@inheritDoc}
	 */
	public final boolean complies(final Object obj) {
		final Comparable value = (Comparable) this.getProperty(obj, this
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
	 * Returns the filter used for compare operations.
	 * 
	 * @return Filter.
	 */
	protected final ComparableFilter getFilter() {
		return filter;
	}

	/**
	 * Returns the operator for compare operations.
	 * 
	 * @return Operator.
	 */
	public final Operator getOperator() {
		return filter.getOperator();
	}

	/**
	 * Returns the operator for compare operations.
	 * 
	 * @return Operator.
	 */
	public final String getOperatorName() {
		return filter.getOperatorName();
	}

}
