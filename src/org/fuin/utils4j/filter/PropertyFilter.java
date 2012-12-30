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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Defines a filter on an object property.
 */
public abstract class PropertyFilter implements Filter {

	/** Property to be used for filtering. */
	private final String propertyName;

	/**
	 * Constructor with property.
	 * 
	 * @param newPropertyName
	 *            Property name.
	 */
	public PropertyFilter(final String newPropertyName) {
		super();
		this.propertyName = newPropertyName;
	}

	/**
	 * Returns the property to be used for filtering.
	 * 
	 * @return Name of the property.
	 */
	public final String getPropertyName() {
		return propertyName;
	}

	/**
	 * Creates the appropriate getter names for the type.
	 * 
	 * @param property
	 *            Name of the Property.
	 * 
	 * @return Possible names of the getter.
	 */
	protected abstract String[] createGetterNames(final String property);

	/**
	 * Return the value of a property via reflection.
	 * 
	 * @param obj
	 *            Object to retrieve a value from.
	 * @param property
	 *            Name of the property.
	 * 
	 * @return Value returned via the getter of the property.
	 */
	protected final Object getProperty(final Object obj, final String property) {
		if ((obj == null) || (property == null)
				|| (property.trim().length() == 0)) {
			return null;
		}
		final String[] getterNames = createGetterNames(property);
		for (int i = 0; i < getterNames.length; i++) {
			final String getter = getterNames[i];
			try {
				final Class cl = obj.getClass();
				final Method m = cl.getMethod(getter, new Class[] {});
				return m.invoke(obj, new Object[] {});
			} catch (IllegalAccessException e) {
				throw new RuntimeException("Accessing " + getter
						+ " method of property '" + property
						+ "' failed (private? protected?)! [" + obj.getClass()
						+ "]", e);
			} catch (InvocationTargetException e) {
				throw new RuntimeException("Exception within " + getter
						+ " method of property '" + property + "'! ["
						+ obj.getClass() + "]", e.getCause());
			} catch (NoSuchMethodException e) {
				if (i == getter.length() - 1) {
					throw new RuntimeException("No " + getter
							+ " method found for property! '" + property
							+ "'! [" + obj.getClass() + "]", e);
				}
			}
		}
		throw new IllegalStateException("No getters defined in 'createGetterNames()'!");
	}

}
