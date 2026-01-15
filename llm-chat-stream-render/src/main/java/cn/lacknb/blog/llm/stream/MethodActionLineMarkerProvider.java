package cn.lacknb.blog.llm.stream;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor;
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.DumbAware;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MethodActionLineMarkerProvider extends LineMarkerProviderDescriptor implements DumbAware {
    @Override
    public @Nullable LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
        return null;
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<? extends PsiElement> elements,
                                       @NotNull Collection<? super LineMarkerInfo<?>> result) {
        Set<PsiMethod> seen = new HashSet<>();
        for (PsiElement element : elements) {
            if (element instanceof PsiMethod) {
                PsiMethod method = (PsiMethod) element;
                if (!seen.add(method)) {
                    continue;
                }
                PsiElement nameIdentifier = method.getNameIdentifier();
                if (nameIdentifier == null) {
                    continue;
                }
                result.add(createLineMarker(nameIdentifier, method));
                continue;
            }
            if (!(element instanceof PsiIdentifier)) {
                continue;
            }
            PsiElement parent = element.getParent();
            if (!(parent instanceof PsiMethod)) {
                continue;
            }
            PsiMethod method = (PsiMethod) parent;
            if (!seen.add(method)) {
                continue;
            }
            result.add(createLineMarker(element, method));
        }
    }

    @Override
    public @NotNull String getName() {
        return "LLM method actions";
    }

    @Override
    public @Nullable javax.swing.Icon getIcon() {
        return AllIcons.Actions.Lightning;
    }

    private static LineMarkerInfo<PsiElement> createLineMarker(PsiElement element, PsiMethod method) {
        return new LineMarkerInfo<>(
                element,
                element.getTextRange(),
                AllIcons.Actions.Lightning,
                (Function<PsiElement, String>) psi -> "AI: explain or optimize method",
                new MethodGutterHandler(method),
                GutterIconRenderer.Alignment.LEFT
        );
    }

    private static class MethodGutterHandler implements GutterIconNavigationHandler<PsiElement> {
        private final PsiMethod method;

        private MethodGutterHandler(PsiMethod method) {
            this.method = method;
        }

        @Override
        public void navigate(MouseEvent e, PsiElement elt) {
            Project project = method.getProject();
            DataContext context = DataManager.getInstance().getDataContext(e.getComponent());
            DefaultActionGroup group = new DefaultActionGroup();
            group.add(new AnAction("解释代码", "Explain this method", AllIcons.Actions.Help) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent event) {
                    String prompt = buildPrompt("解释下面的方法：", method);
                    LLMChatToolWindow.showAndSubmit(project, prompt);
                }
            });
            group.add(new AnAction("优化代码", "Optimize this method", AllIcons.Actions.RefactoringBulb) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent event) {
                    String prompt = buildPrompt("优化下面的方法，并说明改动：", method);
                    LLMChatToolWindow.showAndSubmit(project, prompt);
                }
            });

            JBPopupFactory.getInstance()
                    .createActionGroupPopup(
                            "AI Actions",
                            group,
                            context,
                            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                            true
                    )
                    .show(new RelativePoint(e));
        }
    }

    private static String buildPrompt(String prefix, PsiMethod method) {
        return prefix + "\n```java\n" + method.getText() + "\n```\n";
    }
}
