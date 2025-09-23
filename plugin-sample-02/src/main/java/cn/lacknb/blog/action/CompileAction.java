package cn.lacknb.blog.action;

import cn.lacknb.blog.dialog.CompileDialog;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.project.Project;

public class CompileAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        CompileDialog dialog = new CompileDialog(project);
        dialog.show();
    }
}
