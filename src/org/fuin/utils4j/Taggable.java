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
 * Something that can be tagged to memorize changes made after that point in
 * time. It's not defined what exactly is monitored and how. This depends on the
 * concrete implementation. This is like a kind of transaction but it has only a
 * "rollback" ({@link #revertToTag()}) but no "commit" mode. It's named
 * {@link Taggable} to avoid confusion with database or other real transactions.
 */
public interface Taggable {

    /**
     * Start memorizing changes.
     */
    public void tag();

    /**
     * Stop memorizing changes and clear internal state. It's <b>not</b>
     * reverting any change! It's simply a "forget all changes".
     */
    public void untag();

    /**
     * Returns if the object is currently tagged.
     * 
     * @return If a tag is set <code>true</code> else <code>false</code>.
     */
    public boolean isTagged();

    /**
     * Reverts all changes made since setting the tag and clears internal state.
     */
    public void revertToTag();

    /**
     * Returns if the content of the object has changed since setting the tag.
     * 
     * @return If something has changed <code>true</code> else
     *         <code>false</code>.
     */
    public boolean hasChangedSinceTagging();

}
