package com.vibevault.ui.components;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.FontMetrics;

public final class TableUiUtils {
    private TableUiUtils() {
    }

    public static void hideColumn(JTable table, int columnIndex) {
        table.getColumnModel().getColumn(columnIndex).setMinWidth(0);
        table.getColumnModel().getColumn(columnIndex).setMaxWidth(0);
        table.getColumnModel().getColumn(columnIndex).setPreferredWidth(0);
    }

    public static void installEllipsisRenderer(JTable table, int columnIndex) {
        if (columnIndex < 0 || columnIndex >= table.getColumnCount()) {
            return;
        }
        table.getColumnModel().getColumn(columnIndex).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(
                    JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int column
            ) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                String text = value == null ? "" : value.toString();
                int availableWidth = Math.max(8, t.getColumnModel().getColumn(column).getWidth() - 18);
                label.setText(ellipsizeText(label.getFontMetrics(label.getFont()), text, availableWidth));
                return label;
            }
        });
    }

    private static String ellipsizeText(FontMetrics metrics, String text, int maxWidth) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (metrics.stringWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int ellipsisWidth = metrics.stringWidth(ellipsis);
        if (ellipsisWidth >= maxWidth) {
            return ellipsis;
        }
        int end = text.length();
        while (end > 0) {
            String candidate = text.substring(0, end) + ellipsis;
            if (metrics.stringWidth(candidate) <= maxWidth) {
                return candidate;
            }
            end--;
        }
        return ellipsis;
    }
}
