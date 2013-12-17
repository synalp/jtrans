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

/**
 * A class that defines a rectangular span of rows and/or columns that should
 * be treated as a single cell.  The 'row' and 'column' fields refer to the
 * top-left corner of the spanned cell.  The 'activeRow' and 'activeColumn'
 * fields determine the address of the content displayed in the span, and must
 * be located within the span.  The 'height' and 'width' fields determine the
 * height and width of the spanned area.
 * @author Jonathan Keatley
 */
public class Span {
	private final int row;
	private final int column;
	private final int height;
	private final int width;
	private final int activeRow;
	private final int activeColumn;
	private int _hc;

	/**
	 * Create a Span given the row, column, height, width, and the active row
	 * and column within the span.
	 * @param row The top-most row of the span
	 * @param column The left-most row of the span
	 * @param activeRow The active row
	 * @param activeColumn The active column
	 * @param height The height of the span
	 * @param width The width of the span
	 */
	public Span(int row, int column, int activeRow, int activeColumn, int height, int width) {
		this.row = row;
		this.column = column;
		this.height = height;
		this.width = width;
		this.activeRow = activeRow;
		this.activeColumn = activeColumn;
		
		if (!isDefined(activeRow, activeColumn)) {
			throw new IllegalArgumentException(
				"The active row and column must be inside the span.");
		}
	}

	/**
	 * Create a Span given the row, column, height, and width, with the active
	 * cell being the top-left corner.
	 * @param row The top-most row of the span
	 * @param column The left-most row of the span
	 * @param height The height of the span
	 * @param width The width of the span
	 */
	public Span(int row, int column, int height, int width) {
		this(row, column, row, column, height, width);
	}

	public int getRow() {
		return row;
	}

	public int getColumn() {
		return column;
	}

	public int getActiveRow() {
		return activeRow;
	}

	public int getActiveColumn() {
		return activeColumn;
	}

	public int getHeight() {
		return height;
	}

	public int getWidth() {
		return width;
	}

	/**
	 * Checks if there is any overlap between two Spans.
	 * @param other The other span
	 * @return {@code true} if there is an overlap between this Span and the other.
	 */
	public boolean intersects(Span other) {
		return isDefined(other.row, other.column)		// Other top-left cell
			|| isDefined(other.row + other.height - 1,
				other.column + other.width - 1)			// Other bottom-right cell
			|| other.isDefined(row, column)				// My top-left cell
			|| other.isDefined(row + height - 1,
				column + width - 1);					// My bottom-right cell
	}

	/**
	 * Checks if the row and column fall within the boundaries of the
	 * span.
	 * @param row The row
	 * @param column The column
	 * @return {@code true} if the span contains the row and column.
	 */
	public boolean isDefined(int row, int column) {
		return row >= this.row && row < (this.row + this.height) &&
			column >= this.column && column < (this.column + this.width);
	}

	/**
	 * Checks if the row and column are the active cell in the span.
	 * @param row The row
	 * @param column The column
	 * @return {@code true} if the cell is active.
	 */
	public boolean isActive(int row, int column) {
		return this.activeRow == row && this.activeColumn == column;
	}

	/**
	 * Checks if this span is equal to another one.
	 * @param o The other object
	 * @return {@code true} if the objects are equal.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;

		if (o instanceof Span) {
			Span s = (Span)o;
			return row == s.row && column == s.column &&
				height == s.height && width == s.width &&
				activeRow == s.activeRow &&
				activeColumn == s.activeColumn;
		}
		return false;
	}

	/**
	 * Calculates the hashcode of the span.
	 * @return The hashcode
	 */
	@Override
	public int hashCode() {
		// If uninitialized...
		if (_hc == 0) {
			// Do a lazy eval on the value:
			_hc = 65537*row + 16417*activeRow + 4111*column + 1031*activeColumn
				+ 257*height + width;
			// In the rare case that it is still 0...
			if (_hc == 0) {
				_hc = 1;
			}
		}
		return _hc;
	}
}

