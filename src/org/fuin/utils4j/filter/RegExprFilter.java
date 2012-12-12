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

import java.util.regex.Pattern;

/**
 * Defines a filter based on a regular expression.
 */
public class RegExprFilter implements Filter {

	/**
	 * The matches method attempts to match the entire input sequence against
	 * the pattern.
	 */
	public static final int MATCHES = 0;

	/**
	 * The lookingAt method attempts to match the input sequence, starting at
	 * the beginning, against the pattern.
	 */
	public static final int LOOKING_AT = 1;

	/**
	 * The find method scans the input sequence looking for the next subsequence
	 * that matches the pattern.
	 */
	public static final int FIND = 2;

	// Different types of matching
	private static final String[] TYPES = new String[] { "matches",
			"lookingAt", "find" };

	/** Compiled pattern. */
	private Pattern p = null;

	/** String pattern to apply. */
	private String pattern = null;

	/** Type of matching. */
	private int type = 1;

	/**
	 * Constructor with pattern.
	 * 
	 * @param pattern Pattern to use (Cannot be NULL!)
	 */
	public RegExprFilter(final String pattern) {
		super();
		this.pattern = pattern;
		p = Pattern.compile(pattern);
	}

	/**
	 * {@inheritDoc}
	 */
	public final boolean complies(final Object obj) {
		final String str = obj.toString();
		if (type == MATCHES) {
			return p.matcher(str).matches();
		} else if (type == LOOKING_AT) {
			return p.matcher(str).lookingAt();
		} else if (type == FIND) {
			return p.matcher(str).find();
		}
		throw new IllegalStateException("Unknown 'type': " + type);
	}

	/**
	 * Return the pattern.
	 * 
	 * @return Pattern
	 */
	public final String getPattern() {
		return pattern;
	}

	/**
	 * {@inheritDoc}
	 */
	public final String toString() {
		return " = RegExpr[" + pattern + "]";
	}

	/**
	 * Returns the type of the matching.
	 * 
	 * @return Type (0,1,2)
	 */
	public final int getType() {
		return type;
	}

	/**
	 * Sets the type of the matching.
	 * 
	 * @param type Type (0,1,2)
	 */
	public final void setType(final int type) {
		this.type = type;
	}

	/**
	 * Returns the type name of the matching.
	 * 
	 * @return Type name ("matches", "lookingAt" or "find") or NULL
	 *         (="lookingAt")
	 */
	public final String getTypeName() {
		if (type == LOOKING_AT) {
			return null;
		} else {
			return TYPES[type];
		}
	}

	/**
	 * Sets the type name of the matching.
	 * 
	 * @param typeName Type name ("matches", "lookingAt" or "find") or NULL
	 *            (="lookingAt")
	 */
	public final void setTypeName(final String typeName) {
		if (typeName == null) {
			type = LOOKING_AT;
		} else if (typeName.equalsIgnoreCase(TYPES[0])) {
			type = MATCHES;
		} else if (typeName.equalsIgnoreCase(TYPES[1])) {
			type = LOOKING_AT;
		} else if (typeName.equalsIgnoreCase(TYPES[2])) {
			type = FIND;
		}
	}

}
