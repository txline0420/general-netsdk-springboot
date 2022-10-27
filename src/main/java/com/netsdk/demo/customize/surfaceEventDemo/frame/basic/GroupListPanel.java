package com.netsdk.demo.customize.surfaceEventDemo.frame.basic;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * @author 47040
 * @since Created in 2021/5/11 11:25
 */
public class GroupListPanel extends JPanel {

    private DefaultTableModel groupInfoModel;
    private final JTable groupInfoTable;

    private final int MaxLine = 50;
    private final String[] groupLabel;

    /**
     * @param titles    表头名称列表
     * @param minWidths 表头字段长度
     */
    public GroupListPanel(String[] titles, int[] minWidths) {
        if (titles == null || minWidths.length != titles.length)
            throw new RuntimeException("Group list panel with no titles.");
        groupLabel = titles;
        SwingUtil.setBorderEx(this, "事件信息", 2);
        setLayout(new BorderLayout());
        Dimension dim = getPreferredSize();
        dim.width = 480;
        dim.height = 310;
        setPreferredSize(dim);

        Object[][] groupData = new Object[MaxLine][groupLabel.length];
        groupInfoModel = new DefaultTableModel(groupData, titles);
        groupInfoTable = new JTable(groupInfoModel) {
            @Override // 不可编辑
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        groupInfoModel = (DefaultTableModel) groupInfoTable.getModel();
        for (int i = 0; i < groupLabel.length; i++) {
            if (i == 0) groupInfoTable.getColumnModel().getColumn(i).setMaxWidth(80);
            else if (i == 1) groupInfoTable.getColumnModel().getColumn(i).setMaxWidth(40);
            else groupInfoTable.getColumnModel().getColumn(i).setPreferredWidth(minWidths[i]);
        }

        groupInfoTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);  // 只能选中一行

        // 列表显示居中
        DefaultTableCellRenderer dCellRenderer = new DefaultTableCellRenderer();
        dCellRenderer.setHorizontalAlignment(JLabel.CENTER);
        groupInfoTable.setDefaultRenderer(Object.class, dCellRenderer);

        JScrollPane scrollPane = new JScrollPane(groupInfoTable);
        add(scrollPane, BorderLayout.CENTER);
    }

    public JTable getGroupInfoTable() {
        return groupInfoTable;
    }

    public DefaultTableModel getGroupInfoModel() {
        return groupInfoModel;
    }

    public void clearTableContent() {
        int rowCount = groupInfoModel.getRowCount();
        int columnCount = groupInfoModel.getColumnCount();
        //清空DefaultTableModel中的内容
        for (int i = 0; i < rowCount; i++)//表格中的行数
        {
            for (int j = 0; j < columnCount; j++) {//表格中的列数
                groupInfoModel.setValueAt(" ", i, j); //逐个清空
            }
        }
    }

    public void updateTableContent(String[][] data) {
        clearTableContent();
        for (int i = 0; i < Math.min(data.length, MaxLine); i++) {
            for (int j = 0; j < Math.min(data[0].length, groupLabel.length); j++) {
                groupInfoModel.setValueAt(data[i][j], i, j);
            }
        }
    }
}
