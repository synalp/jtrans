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
 * Defines a filter on a list of sub filters all OR'ed together.
 */
public class OrFilter extends ListFilter {
	/** Used to create a String representation of this filter. */
	public static final String DEFAULT_OR_STR = "or";

	/** Phrase representing this type of filter. */
	private String orStr = DEFAULT_OR_STR;

	/**
	 * Default constructor with no arguments.
	 */
	public OrFilter() {
		super();
	}

	/**
	 * Constructor with two terms.
	 * 
	 * @param firstFilter First term to be or'ed
	 * @param secondFilter Second term to be or'ed
	 */
	public OrFilter(final Filter firstFilter, final Filter secondFilter) {
		super();
		this.addFilter(firstFilter);
		this.addFilter(secondFilter);
	}

	/**
	 * {@inheritDoc}
	 */
	public final boolean complies(final Object obj) {
		if (getFilterList() == null) {
			return true;
		} else {
			for (int i = 0; i < getFilterList().size(); i++) {
				final Filter filter = (Filter) getFilterList().get(i);

				if (filter.complies(obj)) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public final String toString() {
		return toString(orStr);
	}

	/**
	 * Returns the phrase representing this type of filter.
	 * 
	 * @return Name of the filter (default "or")
	 */
	public final String getOrStr() {
		if (orStr.equals(DEFAULT_OR_STR)) {
			return null;
		} else {
			return orStr;
		}
	}

	/**
	 * Set the phrase representing this type of filter A NULL argument will set
	 * the default <code>DEFAULT_OR_STR</code>.
	 * 
	 * @param newOrStr Name of the filter (default "or")
	 */
	public final void setOrStr(final String newOrStr) {
		if (newOrStr == null) {
			orStr = DEFAULT_OR_STR;
		} else {
			orStr = newOrStr;
		}
	}
}
