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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * A wrapper for lists that keeps track of all changes made to the list since
 * construction. Only adding, replacing or deleting elements is tracked (not
 * changes inside the objects). Duplicates elements are not allowed for the list
 * - This is like a {@link java.util.Set} but at the same time ordered like a {@link List}
 * . It's also possible to revert all changes.
 */
public class ChangeTrackingUniqueList implements List, Taggable {

    private final List list;

    private final List added;

    private final List deleted;

    private boolean tagged;

    /**
     * Constructor with covered list. The list is tagged at construction time -
     * This means {@link #isTagged()} will return <code>true</code> without
     * calling {@link #tag()} first. If this behavior is not wanted you can call
     * {@link #untag()} after constructing the list.
     * 
     * @param list
     *            Wrapped list - Be aware that this list will be changed by this
     *            class. There is no internal copy of the list - The reference
     *            itself is used.
     */
    public ChangeTrackingUniqueList(final List list) {
        super();
        Utils4J.checkNotNull("list", list);
        this.list = list;
        this.added = new ArrayList();
        this.deleted = new ArrayList();
        tagged = true;
    }

    /**
     * Returns if the list has changed. If the list is not in tag mode (this
     * means {@link #isTagged()} returns <code>true</code>) this method will
     * always return <code>false</code>.
     * 
     * @return If elements have been added or deleted <code>true</code> else
     *         <code>false</code>.
     */
    public final boolean isChanged() {
        return (added.size() > 0) || (deleted.size() > 0);
    }

    /**
     * Returns deleted elements. If the list is not in tag mode (this means
     * {@link #isTagged()} returns <code>true</code>) this method will always
     * return an empty list.
     * 
     * @return Elements that have been deleted since construction of this
     *         instance - Unmodifiable list!
     */
    public final List getDeleted() {
        return Collections.unmodifiableList(deleted);
    }

    /**
     * Returns added elements. If the list is not in tag mode (this means
     * {@link #isTagged()} returns <code>true</code>) this method will always
     * return an empty list.
     * 
     * @return Elements that have been added since construction of this instance
     *         - Unmodifiable list!
     */
    public final List getAdded() {
        return Collections.unmodifiableList(added);
    }

    /**
     * Roll back all changes made since construction. WARNING: The position of
     * the elements is <b>not</b> guaranteed to be the same again! This is the
     * same function ad {@link #revertToTag()}. If the list is not in tag mode (
     * this means {@link #isTagged()} returns <code>true</code>) this method
     * will do nothing.
     */
    public final void revert() {

        if (tagged) {

            // Remove the added entries
            final Iterator addedIt = added.iterator();
            while (addedIt.hasNext()) {
                final Object entry = addedIt.next();
                list.remove(entry);
                addedIt.remove();
            }

            // Add the removed entries
            final Iterator removedIt = deleted.iterator();
            while (removedIt.hasNext()) {
                final Object entry = removedIt.next();
                list.add(entry);
                removedIt.remove();
            }

        }

    }

    private void addIntern(final Object o) {
        if (tagged) {
            final int idx = deleted.indexOf(o);
            if (idx == -1) {
                added.add(o);
            } else {
                deleted.remove(idx);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public final boolean add(final Object o) {
        if (list.contains(o)) {
            throw new IllegalArgumentException("The argument is already in the list: " + o);
        }
        final boolean b = list.add(o);
        if (b) {
            addIntern(o);
        }
        return b;
    }

    /**
     * {@inheritDoc}
     */
    public final void add(final int index, final Object o) {
        if (list.contains(o)) {
            throw new IllegalArgumentException("The argument is already in the list: " + o);
        }
        list.add(index, o);
        addIntern(o);
    }

    /**
     * {@inheritDoc}
     */
    public final boolean addAll(final Collection c) {
        int count = 0;
        final Iterator it = c.iterator();
        while (it.hasNext()) {
            if (add(it.next())) {
                count++;
            }
        }
        return (count > 0);
    }

    /**
     * {@inheritDoc}
     */
    public final boolean addAll(final int index, final Collection c) {
        int count = 0;
        final Iterator it = c.iterator();
        while (it.hasNext()) {
            add(index + count, it.next());
            count++;
        }
        return (count > 0);
    }

    /**
     * {@inheritDoc}
     */
    public final void clear() {
        for (int i = 0; i < list.size(); i++) {
            final Object o = list.get(i);
            if (tagged && !added.contains(o)) {
                deleted.add(o);
            }
        }
        if (tagged) {
            added.clear();
        }
        list.clear();
    }

    /**
     * {@inheritDoc}
     */
    public final boolean contains(final Object o) {
        return list.contains(o);
    }

    /**
     * {@inheritDoc}
     */
    public final boolean containsAll(final Collection c) {
        return list.containsAll(c);
    }

    /**
     * {@inheritDoc}
     */
    public final Object get(final int index) {
        return list.get(index);
    }

    /**
     * {@inheritDoc}
     */
    public final int indexOf(final Object o) {
        return list.indexOf(o);
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isEmpty() {
        return list.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    public final Iterator iterator() {
        return list.iterator();
    }

    /**
     * {@inheritDoc}
     */
    public final int lastIndexOf(final Object o) {
        return list.lastIndexOf(o);
    }

    /**
     * {@inheritDoc}
     */
    public final ListIterator listIterator() {
        return list.listIterator();
    }

    /**
     * {@inheritDoc}
     */
    public final ListIterator listIterator(final int index) {
        return list.listIterator(index);
    }

    private void removeIntern(final Object o) {
        if (tagged) {
            final int idx = added.indexOf(o);
            if (idx == -1) {
                deleted.add(o);
            } else {
                added.remove(idx);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public final boolean remove(final Object o) {
        final boolean b = list.remove(o);
        if (b) {
            removeIntern(o);
        }
        return b;
    }

    /**
     * {@inheritDoc}
     */
    public final Object remove(final int index) {
        final Object o = list.remove(index);
        if (o != null) {
            removeIntern(o);
        }
        return o;
    }

    /**
     * {@inheritDoc}
     */
    public final boolean removeAll(final Collection c) {
        boolean changed = false;
        final Iterator it = c.iterator();
        while (it.hasNext()) {
            final Object o = it.next();
            if (remove(o)) {
                changed = true;
            }
        }
        return changed;
    }

    /**
     * {@inheritDoc}
     */
    public final boolean retainAll(final Collection c) {
        boolean changed = false;
        for (int i = list.size() - 1; i >= 0; i--) {
            final Object o = list.get(i);
            if (!c.contains(o)) {
                remove(i);
                changed = true;
            }
        }
        return changed;
    }

    /**
     * {@inheritDoc}
     */
    public final Object set(final int index, final Object o) {
        final Object removed = list.set(index, o);
        addIntern(o);
        removeIntern(removed);
        return removed;
    }

    /**
     * {@inheritDoc}
     */
    public final int size() {
        return list.size();
    }

    /**
     * {@inheritDoc}
     */
    public final List subList(final int fromIndex, final int toIndex) {
        return list.subList(fromIndex, toIndex);
    }

    /**
     * {@inheritDoc}
     */
    public final Object[] toArray() {
        return list.toArray();
    }

    /**
     * {@inheritDoc}
     */
    public final Object[] toArray(final Object[] a) {
        return list.toArray(a);
    }

    /**
     * {@inheritDoc}
     */
    public final String toString() {
        return list.toString();
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
            deleted.clear();
        }
    }

}
