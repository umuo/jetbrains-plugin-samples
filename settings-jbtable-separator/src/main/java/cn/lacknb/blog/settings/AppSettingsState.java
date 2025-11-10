package cn.lacknb.blog.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 持久化状态管理服务 (V4 - 支持属性对象)
 */
@State(
        name = "cn.lacknb.blog.settings.AppSettingsState",
        storages = @Storage("ThreeTableSettingsPlugin.xml")
)
public class AppSettingsState implements PersistentStateComponent<AppSettingsState> {

    public int leftSelectedIndex = -1;
    public List<LeftTableItem> leftTableItems = new ArrayList<>();

    public static class LeftTableItem {
        public String name;
        public boolean isDefault = false;
        public TableData tableData = new TableData();

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LeftTableItem that = (LeftTableItem) o;
            return isDefault == that.isDefault && Objects.equals(name, that.name) && Objects.equals(tableData, that.tableData);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, isDefault, tableData);
        }
    }

    public static class TableData {
        public List<CustomRule> customRules = new ArrayList<>();
        public int selectedCustomRuleIndex = -1;

        public List<RuleItem> ruleItems = new ArrayList<>();

        // FIX: 使用 PropertyItem 替换 String
        public List<PropertyItem> bottomItems = new ArrayList<>();
        public int bottomSelectedIndex = -1;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TableData tableData = (TableData) o;
            return selectedCustomRuleIndex == tableData.selectedCustomRuleIndex &&
                    bottomSelectedIndex == tableData.bottomSelectedIndex &&
                    Objects.equals(customRules, tableData.customRules) &&
                    Objects.equals(ruleItems, tableData.ruleItems) &&
                    Objects.equals(bottomItems, tableData.bottomItems);
        }

        @Override
        public int hashCode() {
            return Objects.hash(customRules, selectedCustomRuleIndex, ruleItems, bottomItems, bottomSelectedIndex);
        }
    }

    @Tag("CustomRule")
    public static class CustomRule {
        public String name;
        public String value;

        public CustomRule() {}
        public CustomRule(String name, String value) { this.name = name; this.value = value; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CustomRule that = (CustomRule) o;
            return Objects.equals(name, that.name) && Objects.equals(value, that.value);
        }
        @Override
        public int hashCode() { return Objects.hash(name, value); }
    }

    @Tag("Rule")
    public static class RuleItem {
        public String name;
        public String value;

        public RuleItem() {}
        public RuleItem(String name, String value) { this.name = name; this.value = value; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RuleItem ruleItem = (RuleItem) o;
            return Objects.equals(name, ruleItem.name) && Objects.equals(value, ruleItem.value);
        }
        @Override
        public int hashCode() { return Objects.hash(name, value); }
    }

    /**
     * 新增: 代表属性表格的一行 (名称 + Java代码)
     */
    @Tag("Property")
    public static class PropertyItem {
        public String name;
        public String value;

        public PropertyItem() {}
        public PropertyItem(String name, String value) { this.name = name; this.value = value; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PropertyItem that = (PropertyItem) o;
            return Objects.equals(name, that.name) && Objects.equals(value, that.value);
        }
        @Override
        public int hashCode() { return Objects.hash(name, value); }
    }

    public static AppSettingsState getInstance() {
        return ApplicationManager.getApplication().getService(AppSettingsState.class);
    }

    @Nullable
    @Override
    public AppSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull AppSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
