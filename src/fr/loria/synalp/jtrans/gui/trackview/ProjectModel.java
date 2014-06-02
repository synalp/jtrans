package fr.loria.synalp.jtrans.gui.trackview;

import fr.loria.synalp.jtrans.project.Element;
import fr.loria.synalp.jtrans.project.Project;
import fr.loria.synalp.jtrans.utils.spantable.DefaultSpanModel;
import fr.loria.synalp.jtrans.utils.spantable.SpanModel;
import fr.loria.synalp.jtrans.utils.spantable.SpanTableModel;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Non-editable model.
 */
public abstract class ProjectModel<ProjectT extends Project>
		extends AbstractTableModel
		implements SpanTableModel
{
	protected ProjectT project;
	protected List<Column> columns = new ArrayList<>();
	protected SpanModel spanModel = new DefaultSpanModel();

	protected class Column {
		final int spkID;

		// Highlight variables
		Map<Element, Integer> elementRowMap = new HashMap<>();
		Element highlightedElement;
		int highlightedRow = -1;

		public Column(int spkID) {
			this.spkID = spkID;
		}

		void highlightElement(Element el) {
			int oldHLRow = highlightedRow;
			Integer tc = elementRowMap.get(el);
			int newHLRow = tc==null? -1: tc;

			// if newHLRow>=0: don't un-highlight cell if null word
			if (oldHLRow != newHLRow && newHLRow >= 0) {
				if (oldHLRow >= 0)
					fireTableCellUpdated(oldHLRow, spkID);
				highlightedRow = newHLRow;
			}

			highlightedElement = el;
			if (newHLRow >= 0)
				fireTableCellUpdated(newHLRow, spkID);
		}
	}

	public void highlightElement(int spkID, Element word) {
		columns.get(spkID).highlightElement(word);
	}

	public int getHighlightedRow(int col) {
		return columns.get(col).highlightedRow;
	}


	public Element getHighlightedElement(int col) {
		return columns.get(col).highlightedElement;
	}


	public int getColumnCount() {
		return project.speakerCount();
	}

	public String getColumnName(int column) {
		return project.getSpeakerName(columns.get(column).spkID);
	}

	public SpanModel getSpanModel() {
		return spanModel;
	}

}
