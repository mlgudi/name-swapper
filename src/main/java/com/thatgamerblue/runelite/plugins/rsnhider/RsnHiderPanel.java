package com.thatgamerblue.runelite.plugins.rsnhider;

import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.PluginPanel;
import com.google.inject.Inject;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

class RsnHiderPanel extends PluginPanel
{
    private final RsnHiderPlugin plugin;
    private DefaultTableModel tableModel;
    private JTable table;

    @Inject
    RsnHiderPanel(RsnHiderPlugin plugin) {
        super(false);
        this.plugin = plugin;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));

        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return true;  // all cells are editable
            }
        };

        tableModel.addColumn("Original Name");
        tableModel.addColumn("Swapped Name");

        table = new JTable(tableModel);

        loadNamesToTable();

        JButton addButton = new JButton("Add");
        addButton.addActionListener(e -> {
            tableModel.addRow(new Object[]{"", ""});
        });

        JButton deleteButton = new JButton("Delete");
        deleteButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow != -1) {
                tableModel.removeRow(selectedRow);
            }
        });

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new FlowLayout());
        buttonsPanel.add(addButton);
        buttonsPanel.add(deleteButton);

        add(buttonsPanel, BorderLayout.NORTH);

        JButton saveButton = new JButton("Save Changes");
        saveButton.addActionListener(this::onSave);

        JPanel saveButtonPanel = new JPanel();
        saveButtonPanel.setLayout(new FlowLayout());
        saveButtonPanel.add(saveButton);

        add(saveButtonPanel, BorderLayout.SOUTH);

        JPanel basePanel = new JPanel();
        basePanel.setLayout(new DynamicGridLayout(0, 1));
        basePanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        basePanel.add(new JScrollPane(table));

        add(basePanel, BorderLayout.CENTER);
    }

    private void loadNamesToTable() {
        tableModel.setRowCount(0);
        HashMap<String, String> namesToSwap = plugin.namesToSwap;
        for (Map.Entry<String, String> entry : namesToSwap.entrySet()) {
            tableModel.addRow(new Object[] { entry.getKey(), entry.getValue() });
        }
    }

    private void onSave(ActionEvent e) {
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }

        plugin.namesToSwap.clear();

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String originalName = (String) tableModel.getValueAt(i, 0);
            String swappedName = (String) tableModel.getValueAt(i, 1);
            plugin.namesToSwap.put(originalName, swappedName);
        }

        plugin.saveNames();
    }
}