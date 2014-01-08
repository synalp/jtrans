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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicTableUI;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

public class SpanTableUI extends BasicTableUI {
	/** TODO not tested with column span */
	private int getRowExtendingMostPastEdge(Rectangle r, boolean top) {
		final TableColumnModel tcm = table.getColumnModel();

		int x = r.x;
		final int y = r.y + (top? 0: r.height);
		int edgeRow = -1;

		while (x < r.x + r.width) {
			int row = table.rowAtPoint(new Point(x, y));
			if (row < 0)
				break;

			if ((top && row < edgeRow) || (!top && row > edgeRow))
				edgeRow = row;

			int col = tcm.getColumnIndexAtX(x);
			x += tcm.getColumn(col).getWidth();
		}

		return edgeRow;
	}

	/**
	 * Returns the row with the lowest Y coordinate within the given rectangle.
	 */
	public int getTopmostRow(Rectangle r) {
		return getRowExtendingMostPastEdge(r, true);
	}

	/**
	 * Returns the row with the highest Y coordinate within the given rectangle.
	 */
	public int getBottommostRow(Rectangle r) {
		return getRowExtendingMostPastEdge(r, false);
	}

	@Override
	public void paint(Graphics g, JComponent c) {
		Rectangle r = g.getClipBounds();
		int firstRow = getTopmostRow(r);
		int lastRow = getBottommostRow(r);
		// -1 is a flag that the ending point is outside the table:
		if (lastRow < 0)
			lastRow = table.getRowCount() - 1;
		for (int row = firstRow; row <= lastRow; row++)
			paintRow(row, g);
	}

	private void paintRow(int row, Graphics g) {
		Rectangle clipRect = g.getClipBounds();
		for (int col = 0; col < table.getColumnCount(); col++) {
			Rectangle cellRect = table.getCellRect(row, col, true);
			if (cellRect.intersects(clipRect)) {
				// If a span is defined, only paint the active (top-left) cell. Otherwise paint the cell.
				Span span = ((SpanTableModel)table.getModel()).getSpanModel().getDefinedSpan(row, col);
				if ((span != null && span.isActive(row, col)) || span == null) {
					// At least a part is visible.
					paintCell(row, col, g, cellRect);
				}
			}
		}
	}

	private void paintCell(int row, int column, Graphics g, Rectangle area) {
		int verticalMargin = table.getRowMargin();
		int horizontalMargin = table.getColumnModel().getColumnMargin();

		Color c = g.getColor();
		g.setColor(table.getGridColor());
		g.drawRect(area.x, area.y, area.width - 1, area.height - 1);
		g.setColor(c);

		area.setBounds(area.x + horizontalMargin / 2, area.y + verticalMargin / 2, area.width - horizontalMargin, area.height - verticalMargin);

		if (table.isEditing() && table.getEditingRow() == row && table.getEditingColumn() == column) {
			Component component = table.getEditorComponent();
			component.setBounds(area);
			component.validate();
		} else {
			TableCellRenderer renderer = table.getCellRenderer(row, column);
			Component component = table.prepareRenderer(renderer, row, column);
			if (component.getParent() == null)
				rendererPane.add(component);
			rendererPane.paintComponent(g, component, table, area.x, area.y,
				area.width, area.height, true);
		}
	}
}

