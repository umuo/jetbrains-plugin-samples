package cn.lacknb.blog.advanced.actions;

import cn.lacknb.blog.advanced.coverage.CoverageAnalyzer;
import cn.lacknb.blog.advanced.window.AIDevToolWindowFactory;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.*;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Query;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * 分析当前方法的覆盖率：
 * - 解析项目中的 JaCoCo XML 报告（若存在）
 * - 列出覆盖的/未覆盖的行与分支
 * - 列出引用该方法的测试用例方法
 */
public class AnalyzeCoverageAction extends AnAction {
    public AnalyzeCoverageAction() { super("分析覆盖率"); }

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

        CoverageAnalyzer analyzer = new CoverageAnalyzer(project);
        CoverageAnalyzer.MethodCoverage mc = analyzer.findCoverageForMethod(method);

        tryShowToolWindow(project);
        if (mc == null) {
            AIDevToolWindowFactory.append(project, "未找到JaCoCo覆盖率报告或未匹配到方法。请确保已生成 jacocoTestReport.xml。");
        } else {
            AIDevToolWindowFactory.append(project, "覆盖率（行/分支）: " + mc.coveredLines.size() + "/" + (mc.coveredLines.size()+mc.missedLines.size())
                    + "， 分支: 已覆盖=" + mc.coveredBranches + " 未覆盖=" + mc.missedBranches);
            if (!mc.missedLines.isEmpty()) {
                AIDevToolWindowFactory.append(project, "未覆盖的行: " + mc.missedLines);
            }
        }

        // 列出测试方法引用
        Set<String> tests = findReferencingTests(project, method);
        if (tests.isEmpty()) {
            AIDevToolWindowFactory.append(project, "未发现引用该方法的测试");
        } else {
            AIDevToolWindowFactory.append(project, "可能覆盖该方法的测试如下:");
            for (String t : tests) {
                AIDevToolWindowFactory.append(project, "  - " + t);
            }
        }
    }

    private Set<String> findReferencingTests(Project project, PsiMethod target) {
        Set<String> result = new HashSet<>();
        Query<com.intellij.psi.PsiReference> query = ReferencesSearch.search(target);
        ProjectFileIndex index = ProjectFileIndex.getInstance(project);
        for (com.intellij.psi.PsiReference ref : query) {
            PsiElement e = ref.getElement();
            PsiMethod inMethod = PsiTreeUtil.getParentOfType(e, PsiMethod.class);
            if (inMethod == null) continue;
            PsiFile file = inMethod.getContainingFile();
            if (file == null) continue;
            if (index.isInTestSourceContent(file.getVirtualFile())) {
                String cls = inMethod.getContainingClass() != null ? inMethod.getContainingClass().getQualifiedName() : "<unknown>";
                result.add(cls + "#" + inMethod.getName());
            }
        }
        return result;
    }

    private void tryShowToolWindow(Project project) {
        ToolWindow tw = ToolWindowManager.getInstance(project).getToolWindow("AI Dev Assistant");
        if (tw != null) tw.show();
    }
}
