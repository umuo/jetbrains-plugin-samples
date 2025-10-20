package cn.lacknb.blog.advanced.actions;

import cn.lacknb.blog.advanced.service.AIService;
import cn.lacknb.blog.advanced.window.AIDevToolWindowFactory;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

/**
 * 解释当前光标所在方法的代码
 */
public class ExplainCodeAction extends AnAction {
    public ExplainCodeAction() {
        super("解释代码");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        PsiFile psiFile = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.PSI_FILE);
        PsiElement element = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.PSI_ELEMENT);
        if (psiFile == null) return;
        if (element == null) {
            int offset = e.getRequiredData(com.intellij.openapi.actionSystem.CommonDataKeys.CARET).getOffset();
            element = psiFile.findElementAt(offset);
        }
        PsiMethod method = PsiTreeUtil.getParentOfType(element, PsiMethod.class);
        if (method == null) return;

        AIService ai = project.getService(AIService.class);
        String desc = ai.explainMethod(method);
        tryShowToolWindow(project);
        AIDevToolWindowFactory.append(project, desc);
    }

    private void tryShowToolWindow(Project project) {
        ToolWindow tw = ToolWindowManager.getInstance(project).getToolWindow("AI Dev Assistant");
        if (tw != null) tw.show();
    }
}
