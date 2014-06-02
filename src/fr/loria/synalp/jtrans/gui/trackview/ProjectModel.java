package fr.loria.synalp.jtrans.gui.trackview;

import fr.loria.synalp.jtrans.project.Project;
import fr.loria.synalp.jtrans.project.Token;
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
		Map<Token, Integer> tokenRowMap = new HashMap<>();
		Token highlighted;
		int highlightedRow = -1;

		public Column(int spkID) {
			this.spkID = spkID;
		}

		void highlightElement(Token token) {
			int oldHLRow = highlightedRow;
			Integer tc = tokenRowMap.get(token);
			int newHLRow = tc==null? -1: tc;

			// if newHLRow>=0: don't un-highlight cell if null word
			if (oldHLRow != newHLRow && newHLRow >= 0) {
				if (oldHLRow >= 0)
					fireTableCellUpdated(oldHLRow, spkID);
				highlightedRow = newHLRow;
			}

			highlighted = token;
			if (newHLRow >= 0)
				fireTableCellUpdated(newHLRow, spkID);
		}
	}

	public void highlightToken(int spkID, Token token) {
		columns.get(spkID).highlightElement(token);
	}

	public int getHighlightedRow(int col) {
		return columns.get(col).highlightedRow;
	}


	public Token getHighlightedToken(int col) {
		return columns.get(col).highlighted;
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
