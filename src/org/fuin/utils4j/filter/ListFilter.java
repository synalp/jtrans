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

import java.util.ArrayList;
import java.util.List;

/**
 * Defines a filter on a list of sub filters.
 */
public abstract class ListFilter implements Filter {
	/** Used to build a String representation out of this filter. */
	public static final String DEFAULT_OPEN_BRACKET = "(";

	/** Used to build a String representation out of this filter. */
	public static final String DEFAULT_CLOSE_BRACKET = ")";

	/** List containing two or more filters to be concatenated. */
	private List filterList = new ArrayList();

	/** Opening the complete term. */
	private String openBracket = DEFAULT_OPEN_BRACKET;

	/** Closing the complete term. */
	private String closeBracket = DEFAULT_CLOSE_BRACKET;

	/**
	 * Default constructor with no arguments.
	 */
	public ListFilter() {
		super();
	}

	/**
	 * Constructor with two terms.
	 * 
	 * @param firstFilter First filter part
	 * @param secondFilter Second filter part
	 */
	public ListFilter(final Filter firstFilter, final Filter secondFilter) {
		super();
		this.addFilter(firstFilter);
		this.addFilter(secondFilter);
	}

	/**
	 * Adds another filter term to the list.
	 * 
	 * @param filter Part to be added
	 */
	public final void addFilter(final Filter filter) {
		filterList.add(filter);
	}

	/**
	 * Removes a filter term from the list.
	 * 
	 * @param filter Part to be removed
	 */
	public final void removeFilter(final Filter filter) {
		filterList.remove(filter);
	}

	/**
	 * Helper for creating a String representation of the filter.
	 * 
	 * @param opStr Name of the operator ("and", "or" ...)
	 * 
	 * @return String with brackets
	 */
	protected final String toString(final String opStr) {
		final StringBuffer sb = new StringBuffer();
		sb.append(openBracket);

		for (int i = 0; i < filterList.size(); i++) {
			if (i > 0) {
				sb.append(" ");
				sb.append(opStr);
				sb.append(" ");
			}

			final Filter filter = (Filter) filterList.get(i);
			sb.append(filter.toString());
		}

		sb.append(closeBracket);

		return sb.toString();
	}

	/**
	 * Returns the String used as a closing bracket for the term.
	 * 
	 * @return Bracket
	 */
	public final String getCloseBracket() {
		if (closeBracket.equals(DEFAULT_CLOSE_BRACKET)) {
			return null;
		} else {
			return closeBracket;
		}
	}

	/**
	 * Returns the String used as an opening bracket for the term.
	 * 
	 * @return Bracket
	 */
	public final String getOpenBracket() {
		if (openBracket.equals(DEFAULT_OPEN_BRACKET)) {
			return null;
		} else {
			return openBracket;
		}
	}

	/**
	 * Sets the String used as a closing bracket for the term.
	 * 
	 * @param newCloseBracket Bracket
	 */
	public final void setCloseBracket(final String newCloseBracket) {
		if (newCloseBracket == null) {
			closeBracket = DEFAULT_CLOSE_BRACKET;
		} else {
			closeBracket = newCloseBracket;
		}
	}

	/**
	 * Sets the String used as an opening bracket for the term.
	 * 
	 * @param newOpenBracket Bracket
	 */
	public final void setOpenBracket(final String newOpenBracket) {
		if (newOpenBracket == null) {
			openBracket = DEFAULT_OPEN_BRACKET;
		} else {
			openBracket = newOpenBracket;
		}
	}

	/**
	 * Returns a list of all subfilters. (Always non-NULL).
	 * 
	 * @return List with <code>Filter</code> objects
	 */
	public final List getFilterList() {
		if (filterList.size() == 0) {
			return null;
		} else {
			return filterList;
		}
	}

	/**
	 * Set a list of subfilters. A NULL argument will simply clear the internal.
	 * list.
	 * 
	 * @param newList List with filters to be used
	 */
	public final void setFilterList(final List newList) {
		if (newList == null) {
			filterList.clear();
		} else {
			filterList = newList;
		}
	}
}
