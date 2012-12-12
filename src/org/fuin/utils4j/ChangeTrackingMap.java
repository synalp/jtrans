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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A wrapper for maps that keeps track of all changes made to the map since
 * construction. Only adding, replacing or deleting elements is tracked (not
 * changes inside the objects). It's also possible to revert all changes.
 */
public class ChangeTrackingMap implements Map, Taggable {

    private final Map map;

    private final Map added;

    private final Map changed;

    private final Map removed;

    private boolean tagged;
    
    /**
     * Constructor with covered map. The map is tagged at construction time -
     * This means {@link #isTagged()} will return <code>true</code> without
     * calling {@link #tag()} first. If this behavior is not wanted you can call
     * {@link #untag()} after constructing the map.
     * 
     * @param map
     *            Wrapped map - Be aware that this map will be changed by this
     *            class. There is no internal copy of the map - The reference
     *            itself is used.
     */
    public ChangeTrackingMap(final Map map) {
        super();
        Utils4J.checkNotNull("map", map);
        this.map = map;
        this.added = new HashMap();
        this.changed = new HashMap();
        this.removed = new HashMap();
        this.tagged = true;
    }

    /**
     * Returns if the list has changed. If the map is not in tag mode (this means
     * {@link #isTagged()} returns <code>true</code>) this method will always
     * return <code>false</code>.
     * 
     * @return If elements have been added or deleted <code>true</code> else
     *         <code>false</code>.
     */
    public final boolean isChanged() {
        return (added.size() > 0) || (changed.size() > 0) || (removed.size() > 0);
    }

    /**
     * Returns removed elements. If the map is not in tag mode (this means
     * {@link #isTagged()} returns <code>true</code>) this method will always
     * return an empty map.
     * 
     * @return Elements that have been deleted since construction of this
     *         instance - Unmodifiable map!
     */
    public final Map getRemoved() {
        return Collections.unmodifiableMap(removed);
    }

    /**
     * Returns changed elements. If the map is not in tag mode (this means
     * {@link #isTagged()} returns <code>true</code>) this method will always
     * return an empty map.
     * 
     * @return Elements that have been changed since construction of this
     *         instance - Unmodifiable map!
     */
    public final Map getChanged() {
        return Collections.unmodifiableMap(changed);
    }

    /**
     * Roll back all changes made since construction. This is the
     * same function ad {@link #revertToTag()}. If the map is not in tag mode (
     * this means {@link #isTagged()} returns <code>true</code>) this method
     * will do nothing.
     */
    public final void revert() {

        if (tagged) {
            
            // Remove the added entries
            final Iterator addedIt = added.keySet().iterator();
            while (addedIt.hasNext()) {
                final Object key = addedIt.next();
                map.remove(key);
                addedIt.remove();
            }
    
            // Replace the changed entries
            final Iterator changedIt = changed.keySet().iterator();
            while (changedIt.hasNext()) {
                final Object key = changedIt.next();
                final Object value = changed.get(key);
                map.put(key, value);
                changedIt.remove();
            }
    
            // Add the removed entries
            final Iterator removedIt = removed.keySet().iterator();
            while (removedIt.hasNext()) {
                final Object key = removedIt.next();
                final Object value = removed.get(key);
                map.put(key, value);
                removedIt.remove();
            }

        }
        
    }

    /**
     * Returns added elements. If the map is not in tag mode (this means
     * {@link #isTagged()} returns <code>true</code>) this method will always
     * return an empty map.
     * 
     * @return Elements that have been added since construction of this
     *         instance - Unmodifiable map!
     */
    public final Map getAdded() {
        return Collections.unmodifiableMap(added);
    }

    private void changeIntern(final Object key, final Object oldValue, final Object newValue) {
        if (tagged) {
            final Object addedValue = added.get(key);
            if (addedValue == null) {
                final Object changedValue = changed.get(key);
                if (changedValue == null) {
                    final Object removedValue = removed.get(key);
                    if (removedValue == null) {
                        if (oldValue == null) {
                            added.put(key, newValue);
                        } else {
                            changed.put(key, oldValue);
                        }
                    } else {
                        removed.remove(key);
                        if (!removedValue.equals(newValue)) {
                            changed.put(key, removedValue);
                        }
                    }
                } else {
                    if (changedValue.equals(newValue)) {
                        changed.remove(key);
                    }
                }
            } else {
                if (!addedValue.equals(newValue) && (newValue != null)) {
                    added.put(key, newValue);
                }
            }
        }
    }

    private void removeIntern(final Object key, final Object value) {
        if (tagged) {
            if (added.get(key) == null) {
                final Object changedValue = changed.get(key);
                if (changedValue == null) {
                    if ((removed.get(key) == null) && (value != null)) {
                        removed.put(key, value);
                    }
                } else {
                    changed.remove(key);
                    removed.put(key, changedValue);
                }
            } else {
                added.remove(key);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public final void clear() {
        final Iterator it = map.keySet().iterator();
        while (it.hasNext()) {
            final Object key = it.next();
            final Object value = map.get(key);
            removeIntern(key, value);
        }
        if (tagged) {
            added.clear();
        }
        map.clear();
    }

    /**
     * {@inheritDoc}
     */
    public final boolean containsKey(final Object key) {
        return map.containsKey(key);
    }

    /**
     * {@inheritDoc}
     */
    public final boolean containsValue(final Object value) {
        return map.containsValue(value);
    }

    /**
     * {@inheritDoc}
     */
    public final Set entrySet() {
        return map.entrySet();
    }

    /**
     * {@inheritDoc}
     */
    public final Object get(final Object key) {
        return map.get(key);
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    public final Set keySet() {
        return map.keySet();
    }

    /**
     * {@inheritDoc}
     */
    public final Object put(final Object key, final Object newValue) {
        final Object oldValue = map.put(key, newValue);
        changeIntern(key, oldValue, newValue);
        return oldValue;
    }

    /**
     * {@inheritDoc}
     */
    public final void putAll(final Map newMap) {
        final Iterator it = newMap.keySet().iterator();
        while (it.hasNext()) {
            final Object key = it.next();
            final Object newValue = newMap.get(key);
            final Object oldValue = map.put(key, newValue);
            changeIntern(key, oldValue, newValue);
        }
    }

    /**
     * {@inheritDoc}
     */
    public final Object remove(final Object key) {
        final Object oldValue = map.remove(key);
        removeIntern(key, oldValue);
        return oldValue;
    }

    /**
     * {@inheritDoc}
     */
    public final int size() {
        return map.size();
    }

    /**
     * {@inheritDoc}
     */
    public final Collection values() {
        return map.values();
    }

    /**
     * {@inheritDoc}
     */
    public final String toString() {
        return map.toString();
    }

    /**
     * {@inheritDoc}
     */
    public final boolean hasChangedSinceTagging() {
        return isChanged();
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isTagged() {
        return tagged;
    }

    /**
     * {@inheritDoc}
     */
    public final void revertToTag() {
        revert();
    }

    /**
     * {@inheritDoc}
     */
    public final void tag() {
        if (!tagged) {
            tagged = true;
        }
    }

    /**
     * {@inheritDoc}
     */
    public final void untag() {
        if (tagged) {
            tagged = false;
            added.clear();
            changed.clear();
            removed.clear();
        }
    }
    
}
