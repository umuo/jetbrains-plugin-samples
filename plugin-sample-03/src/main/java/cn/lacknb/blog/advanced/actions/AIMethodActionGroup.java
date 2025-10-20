package cn.lacknb.blog.advanced.actions;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 方法处的动作集合：解释代码、生成注释、生成单测、覆盖率分析
 */
public class AIMethodActionGroup extends ActionGroup {
    @Override
    public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
        return new AnAction[]{
                new ExplainCodeAction(),
                new GenerateCommentAction(),
                new GenerateTestAction(),
                new AnalyzeCoverageAction()
        };
    }
}
