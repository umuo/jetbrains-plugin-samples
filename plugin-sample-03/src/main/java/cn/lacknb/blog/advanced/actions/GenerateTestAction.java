package cn.lacknb.blog.advanced.actions;

import cn.lacknb.blog.advanced.service.AIService;
import cn.lacknb.blog.advanced.window.AIDevToolWindowFactory;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 为当前方法生成JUnit5测试文件，优先写入测试源目录，否则与源类同包。
 */
public class GenerateTestAction extends AnAction {
    public GenerateTestAction() { super("生成单元测试"); }

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
        String testCode = ai.generateJUnit5TestSkeleton(method);

        PsiClass owner = method.getContainingClass();
        if (owner == null || owner.getQualifiedName() == null) return;
        String pkg = owner.getQualifiedName();
        if (owner.getQualifiedName().contains(".")) {
            pkg = owner.getQualifiedName().substring(0, owner.getQualifiedName().lastIndexOf('.'));
        } else {
            pkg = "";
        }
        String testClassName = owner.getName() + "Test";
        String fileName = testClassName + ".java";

        // 寻找测试源目录
        VirtualFile targetRoot = findTestSourceRoot(project, psiFile.getVirtualFile());
        if (targetRoot == null) {
            // 若没有测试目录，落到源文件相同包
            targetRoot = Objects.requireNonNull(psiFile.getVirtualFile().getParent());
        }
        try {
            final VirtualFile dir = ensurePackageDir(targetRoot, pkg);
            WriteAction.run(() -> {
                try {
                    VirtualFile testFile = dir.findChild(fileName);
                    if (testFile == null) {
                        testFile = dir.createChildData(this, fileName);
                    }
                    testFile.setBinaryContent(testCode.getBytes(StandardCharsets.UTF_8));
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });
            AIDevToolWindowFactory.append(project, "测试文件已生成: " + (pkg.isEmpty() ? fileName : (pkg.replace('.', '/') + "/" + fileName)));
        } catch (Exception ex) {
            AIDevToolWindowFactory.append(project, "生成测试失败: " + ex.getMessage());
        }
    }

    private VirtualFile ensurePackageDir(VirtualFile root, String pkg) throws IOException {
        if (pkg == null || pkg.isEmpty()) return root;
        String rel = pkg.replace('.', '/');
        return VfsUtil.createDirectories(root.getPath() + "/" + rel);
    }

    private VirtualFile findTestSourceRoot(Project project, VirtualFile contextFile) {
        ProjectFileIndex index = ProjectFileIndex.getInstance(project);
        com.intellij.openapi.module.Module module = index.getModuleForFile(contextFile);
        if (module == null) return null;
        for (VirtualFile root : ModuleRootManager.getInstance(module).getSourceRoots(true)) {
            if (index.isInTestSourceContent(root)) {
                return root;
            }
        }
        return null;
    }
}
