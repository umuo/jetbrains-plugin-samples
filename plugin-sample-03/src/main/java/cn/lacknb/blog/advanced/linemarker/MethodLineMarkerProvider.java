package cn.lacknb.blog.advanced.linemarker;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

/**
 * 在每个Java方法的行号左侧显示一个图标，作为AI助手入口
 */
public class MethodLineMarkerProvider extends RelatedItemLineMarkerProvider {
    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement element,
                                            @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
        if (!(element instanceof PsiMethod)) return;
        PsiMethod method = (PsiMethod) element;
        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(AllIcons.Actions.IntentionBulb)
                .setTarget(method)
                .setTooltipText("AI助手：解释/注释/生成测试/覆盖率分析")
                .setPopupTitle("AI助手功能")
                .setAlignment(GutterIconRenderer.Alignment.RIGHT)
                .setTargets(Collections.singletonList(method));
        if (method.getNameIdentifier() != null) {
            result.add(builder.createLineMarkerInfo(method.getNameIdentifier()));
        }
    }
}
