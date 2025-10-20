package cn.lacknb.blog.advanced.actions;

import cn.lacknb.blog.advanced.window.AIDevToolWindowFactory;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.compiler.CompilerMessage;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * 使用IDE编译器编译当前打开的Java文件
 */
public class CompileCurrentFileAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (file == null || !"java".equalsIgnoreCase(file.getExtension())) {
            AIDevToolWindowFactory.append(project, "请选择一个Java文件");
            return;
        }
        AIDevToolWindowFactory.append(project, "开始编译: " + file.getPath());
        CompilerManager.getInstance(project).compile(new VirtualFile[]{file}, (aborted, errors, warnings, context) -> {
            if (aborted) {
                AIDevToolWindowFactory.append(project, "编译已中止");
                return;
            }
            for (CompilerMessage msg : context.getMessages(CompilerMessageCategory.ERROR)) {
                AIDevToolWindowFactory.append(project, "ERROR: " + msg.getMessage());
            }
            for (CompilerMessage msg : context.getMessages(CompilerMessageCategory.WARNING)) {
                AIDevToolWindowFactory.append(project, "WARN: " + msg.getMessage());
            }
            if (errors == 0) {
                AIDevToolWindowFactory.append(project, "编译成功");
            } else {
                AIDevToolWindowFactory.append(project, "编译结束，存在错误: " + errors);
            }
        });
    }
}
