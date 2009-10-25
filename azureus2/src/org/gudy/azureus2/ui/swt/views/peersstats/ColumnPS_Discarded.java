package org.gudy.azureus2.ui.swt.views.peersstats;

import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;

public class ColumnPS_Discarded
	implements TableCellRefreshListener
{

	public static final String COLUMN_ID = "discarded";

	public ColumnPS_Discarded(TableColumn column) {
		column.initialize(TableColumn.ALIGN_TRAIL, TableColumn.POSITION_LAST, 100);
		column.addListeners(this);
		column.setType(TableColumn.TYPE_TEXT_ONLY);
	}

	public void refresh(TableCell cell) {
		PeersStatsDataSource ds = (PeersStatsDataSource) cell.getDataSource();
		if (ds == null) {
			return;
		}
		long val = ds.bytesDiscarded;
		if (cell.setSortValue(val) || !cell.isValid()) {
			cell.setText(DisplayFormatters.formatByteCountToKiBEtc(val));
		}
	}
}
