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
 * Defines a filter on a list of sub filters 'and's them all together.
 */
public class AndFilter extends ListFilter {
	/** Used to create a String representation of this filter. */
	public static final String DEFAULT_AND_STR = "and";

	/** Phrase representing this type of filter. */
	private String andStr = DEFAULT_AND_STR;

	/**
	 * Default constructor with no arguments.
	 */
	public AndFilter() {
		super();
	}

	/**
	 * Constructor with two terms.
	 * 
	 * @param firstFilter First term to be and'ed
	 * @param secondFilter Second term to be and'ed
	 */
	public AndFilter(final Filter firstFilter, final Filter secondFilter) {
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

				if (!filter.complies(obj)) {
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * Returns the phrase representing this type of filter.
	 * 
	 * @return Name of the filter (default "and")
	 */
	public final String getAndStr() {
		if (andStr.equals(DEFAULT_AND_STR)) {
			return null;
		} else {
			return andStr;
		}
	}

	/**
	 * Set the phrase representing this type of filter A NULL argument will set
	 * the default <code>DEFAULT_AND_STR</code>.
	 * 
	 * @param newAndStr Name of the filter (default "and")
	 */
	public final void setAndStr(final String newAndStr) {
		if (newAndStr == null) {
			andStr = DEFAULT_AND_STR;
		} else {
			andStr = newAndStr;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public final String toString() {
		return toString(andStr);
	}

}
