package pt.monitorizapt.ui;

import pt.monitorizapt.service.SensorSnapshot;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class SensorTableModel extends AbstractTableModel {

    private final String[] columnNames = {"ID", "Location", "Type", "Value", "Alert"};
    private final List<SensorSnapshot> data = new ArrayList<>();

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public String getColumnName(int col) {
        return columnNames[col];
    }

    @Override
    public Object getValueAt(int row, int col) {
        SensorSnapshot snapshot = data.get(row);
        return switch (col) {
            case 0 -> snapshot.id();
            case 1 -> snapshot.localizacao();
            case 2 -> snapshot.tipo();
            case 3 -> String.format("%.2f %s", snapshot.valor(), snapshot.unidade());
            case 4 -> snapshot.alerta();
            default -> null;
        };
    }

    public void upsert(SensorSnapshot snapshot) {
        int rowIndex = -1;
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).id().equals(snapshot.id())) {
                rowIndex = i;
                break;
            }
        }

        if (rowIndex >= 0) {
            data.set(rowIndex, snapshot);
            fireTableRowsUpdated(rowIndex, rowIndex);
        } else {
            data.add(snapshot);
            fireTableRowsInserted(data.size() - 1, data.size() - 1);
        }
    }
}