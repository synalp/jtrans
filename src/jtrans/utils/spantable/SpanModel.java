/*
 * Copyright (c) 2011, Jonathan Keatley. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library.  If not, see <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package jtrans.utils.spantable;

import java.util.List;

/**
 * A model for use by SpanTable, to hold a set of spans that are to be
 * wider or higher than a single cell.
 * @author Jonathan Keatley
 */
public interface SpanModel {
	/**
	 * Adds a Span to the model.
	 * @param span The span to add.
	 * @throws IllegalArgumentException if the new span intersects any that are
	 *  already in the model.
	 */
	void addSpan(Span span);

	/**
	 * Removes a span from the model.
	 * @param span The span to remove.
	 */
	void removeSpan(Span span);

	/**
	 * Remove all spans from the model.
	 */
	void clear();

	/**
	 * Get the span that is defined at this row and column.
	 * @param row The row
	 * @param column The column
	 * @return The active span, or {@code null} if none is there.
	 */
	Span getDefinedSpan(int row, int column);

	/**
	 * Get all spans in the model.
	 * @return The list of all spans in the model.
	 */
	List<Span> getSpans();

	/**
	 * Adds a SpanListener.
	 * @param listener The listener
	 */
	void addSpanListener(SpanListener listener);

	/**
	 * Removes a SpanListener.
	 * @param listener The listener
	 */
	void removeSpanListener(SpanListener listener);
}

