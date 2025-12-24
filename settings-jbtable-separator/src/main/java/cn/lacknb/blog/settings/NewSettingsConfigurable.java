package cn.lacknb.blog.settings;

import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBIntSpinner;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class NewSettingsConfigurable implements SearchableConfigurable {

    private final Project project;

    public NewSettingsConfigurable(Project project) {
        this.project = project;
    }

    @Override
    public @NotNull String getId() {
        return "NewSettingsConfigurable";
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "New Settings";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        // a big panel with BoxLayout
        JPanel mainPanel = new JBPanel<>();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // first sub-panel with FlowLayout
        // Set hgap/vgap to 0 for alignment
        JPanel topPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 0, 0));
        topPanel.add(new JComboBox<>(new String[]{"Option 1", "Option 2", "Option 3"}));
        // Add some bottom spacing
        topPanel.setBorder(JBUI.Borders.emptyBottom(5));
        mainPanel.add(topPanel);

        // second sub-panel with BoxLayout
        JPanel bottomPanel = new JBPanel<>();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setBorder(JBUI.Borders.emptyTop(10));

        // row 1: a checkbox
        // Changed to 0, 0 to match other rows
        JPanel row1 = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row1.add(new JBCheckBox("Enable feature"));
        // Add vertical spacing via border
        row1.setBorder(JBUI.Borders.emptyBottom(5));
        bottomPanel.add(row1);

        // row 2: checkbox + label + spinner
        JPanel row2 = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row2.add(new JBCheckBox("Enable another feature"));
        
        // Add 20px gap between Checkbox and Label
        row2.add(Box.createHorizontalStrut(JBUI.scale(20))); 
        
        row2.add(new JBLabel("Value:"));
        
        // Add 5px gap between Label and Spinner
        row2.add(Box.createHorizontalStrut(JBUI.scale(5))); 
        
        row2.add(new JBIntSpinner(0, 0, 100));
        // Add vertical spacing via border
        row2.setBorder(JBUI.Borders.emptyBottom(5));
        bottomPanel.add(row2);

        // row 3: checkbox + label + spinner
        JPanel row3 = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row3.add(new JBCheckBox("Enable a third feature"));
        
        // Add 20px gap
        row3.add(Box.createHorizontalStrut(JBUI.scale(20)));
        
        row3.add(new JBLabel("Another value:"));
        
        // Add 5px gap
        row3.add(Box.createHorizontalStrut(JBUI.scale(5)));
        
        row3.add(new JBIntSpinner(5, 0, 20));
        // Add vertical spacing via border
        row3.setBorder(JBUI.Borders.emptyBottom(5));
        bottomPanel.add(row3);

        mainPanel.add(bottomPanel);

        // Wrapper panel to align components to the top
        JPanel wrapper = new JBPanel<>(new BorderLayout());
        wrapper.add(mainPanel, BorderLayout.NORTH);
        return wrapper;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void apply() {
        // Apply settings
    }

    @Override
    public void reset() {
        // Reset settings
    }
}
