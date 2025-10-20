package cn.lacknb.blog.advanced.actions;

import cn.lacknb.blog.advanced.window.AIDevToolWindowFactory;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

/**
 * 为当前方法生成简单注释（Javadoc），若已存在则不覆盖。
 */
public class GenerateCommentAction extends AnAction {
    public GenerateCommentAction() { super("生成注释"); }

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

        try {
            PsiDocComment doc = method.getDocComment();
            if (doc == null) {
                com.intellij.openapi.application.WriteCommandAction.runWriteCommandAction(project, () -> {
                    StringBuilder javadoc = new StringBuilder();
                    javadoc.append("/**\n");
                    javadoc.append(" * 自动生成的注释: ").append(method.getName()).append("\n");
                    for (PsiParameter param : method.getParameterList().getParameters()) {
                        javadoc.append(" * @param ")
                                .append(param.getName()).append(" ")
                                .append(param.getType().getPresentableText()).append("\n");
                    }
                    if (method.getReturnType() != null && !method.getReturnType().equals(com.intellij.psi.PsiType.VOID)) {
                        javadoc.append(" * @return ").append(method.getReturnType().getPresentableText()).append("\n");
                    }
                    javadoc.append(" */\n");
                    com.intellij.psi.PsiElementFactory factory = com.intellij.psi.JavaPsiFacade.getInstance(project).getElementFactory();
                    PsiDocComment newDoc = factory.createDocCommentFromText(javadoc.toString());
                    method.addBefore(newDoc, method.getFirstChild());
                });
                tryShowToolWindow(project);
                AIDevToolWindowFactory.append(project, "已为方法生成注释: " + method.getName());
            } else {
                tryShowToolWindow(project);
                AIDevToolWindowFactory.append(project, "方法已存在注释: " + method.getName());
            }
        } catch (IncorrectOperationException ex) {
            tryShowToolWindow(project);
            AIDevToolWindowFactory.append(project, "生成注释失败: " + ex.getMessage());
        }
    }

    private void tryShowToolWindow(Project project) {
        ToolWindow tw = ToolWindowManager.getInstance(project).getToolWindow("AI Dev Assistant");
        if (tw != null) tw.show();
    }
}
