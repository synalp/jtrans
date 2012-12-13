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
package org.fuin.utils4j;

/**
 * A key value pair that maintains an additional change state. The state
 * contains the information if the value has been changed since construction of
 * the instance. The methods <code>hashCode()</code> and
 * <code>equals(Object)</code> are based on the <code>key</code> attribute. The
 * attributes <code>key</code> and <code>initialValue</code> are immutable.
 */
public class Property implements Comparable {

    private final String key;

    private final String initialValue;

    private String value;

    /**
     * Constructor with all values.
     * 
     * @param key
     *            Unique name - Cannot be <code>null</code>.
     * @param initialValue
     *            Initial value.
     * @param value
     *            Value.
     */
    public Property(final String key, final String initialValue, final String value) {
        super();
        Utils4J.checkNotNull("key", key);
        this.key = key;
        this.initialValue = initialValue;
        this.value = value;
    }

    /**
     * Returns the current value of the property.
     * 
     * @return Actual value.
     */
    public final String getValue() {
        return value;
    }

    /**
     * Sets the current value of the property.
     * 
     * @param value
     *            Value to set.
     */
    public final void setValue(final String value) {
        this.value = value;
    }

    /**
     * Returns the initial value of the property.
     * 
     * @return Value at construction time.
     */
    public final String getInitialValue() {
        return initialValue;
    }

    /**
     * Returns the key of the property.
     * 
     * @return Unique name.
     */
    public final String getKey() {
        return key;
    }

    /**
     * Returns if the value has changed since construction.
     * 
     * @return If the value has changed <code>true</code> else
     *         <code>false</code>.
     */
    public final boolean hasChanged() {
        if (initialValue == null) {
            // New values signal a change (if non-null)
            return (value != null);
        }
        return !initialValue.equals(value);
    }

    /**
     * Returns if the value has been deleted since construction.
     * 
     * @return If the value has been set to <code>null</code> <code>true</code>
     *         else <code>false</code>.
     */
    public final boolean isDeleted() {
        if (initialValue == null) {
            return false;
        }
        return (value == null);
    }

    /**
     * Returns if the value has been created since construction.
     * 
     * @return If the property has been set to a non-<code>null</code> value
     *         <code>true</code> else <code>false</code>.
     */
    public final boolean isNew() {
        if (initialValue != null) {
            return false;
        }
        return (value != null);
    }

    /**
     * {@inheritDoc}
     */
    public final int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public final boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Property other = (Property) obj;
        if (key == null) {
            if (other.key != null) {
                return false;
            }
        } else if (!key.equals(other.key)) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public final int compareTo(final Object o) {
        final Property other = (Property) o;
        return key.compareTo(other.key);
    }

    /**
     * Returns the key and value separated by an equal sign.
     * 
     * @return A text like "key=value".
     */
    public final String toKeyValue() {
        return key + "=" + value;
    }

    /**
     * Returns a 3-code status text.
     * 
     * @return "NEW" (new), "DEL" (deleted), "CHG" (changed) or "---"
     *         (unchanged).
     */
    public final String getStatus() {
        if (isNew()) {
            return "NEW";
        }
        if (isDeleted()) {
            return "DEL";
        }
        if (hasChanged()) {
            return "CHG";
        }
        return "---";
    }

    /**
     * {@inheritDoc}
     */
    public final String toString() {
        return "[" + getStatus() + "] " + key + "=" + value;
    }

}
