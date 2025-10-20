package cn.lacknb.blog.advanced.service;

import com.intellij.openapi.components.Service;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 轻量的“AI”服务：
 * - 通过对PSI抽象语法树的静态分析来给出代码解释
 * - 生成基础的JUnit5单元测试骨架
 * 无需外部网络依赖，保证在本地IDE环境可运行。
 */
@Service
public final class AIService {

    public String explainMethod(PsiMethod method) {
        StringBuilder sb = new StringBuilder();
        PsiClass cls = method.getContainingClass();
        sb.append("方法: ").append(method.getName()).append("()\n");
        if (cls != null) sb.append("所属类: ").append(cls.getQualifiedName()).append("\n");

        // 参数与返回值
        sb.append("返回类型: ").append(method.getReturnType() == null ? "void" : method.getReturnType().getPresentableText()).append("\n");
        sb.append("参数: ");
        if (method.getParameterList().getParametersCount() == 0) {
            sb.append("无\n");
        } else {
            sb.append(Arrays.stream(method.getParameterList().getParameters())
                    .map(p -> p.getType().getPresentableText() + " " + p.getName())
                    .collect(Collectors.joining(", "))).append("\n");
        }

        // 统计语句特征
        PsiCodeBlock body = method.getBody();
        if (body != null) {
            int ifs = PsiTreeUtil.findChildrenOfType(body, PsiIfStatement.class).size();
            int loops = PsiTreeUtil.findChildrenOfType(body, PsiLoopStatement.class).size();
            int switches = PsiTreeUtil.findChildrenOfType(body, PsiSwitchStatement.class).size();
            int tryCount = PsiTreeUtil.findChildrenOfType(body, PsiTryStatement.class).size();
            int calls = PsiTreeUtil.findChildrenOfType(body, PsiMethodCallExpression.class).size();
            sb.append("控制流: if=").append(ifs).append(", loop=").append(loops)
                    .append(", switch=").append(switches).append(", try/catch=").append(tryCount).append("\n");
            // 列出被调用的方法（去重）
            Set<String> invoked = new LinkedHashSet<>();
            for (PsiMethodCallExpression call : PsiTreeUtil.findChildrenOfType(body, PsiMethodCallExpression.class)) {
                PsiMethod target = call.resolveMethod();
                if (target != null) {
                    String owner = Optional.ofNullable(target.getContainingClass()).map(PsiClass::getQualifiedName).orElse("<unknown>");
                    invoked.add(owner + "#" + target.getName());
                }
            }
            if (!invoked.isEmpty()) {
                sb.append("可能调用的方法: \n");
                invoked.forEach(s -> sb.append("  - ").append(s).append("\n"));
            }
            if (loops > 0) {
                sb.append("复杂度提示: 存在循环，时间复杂度至少为 O(n)。嵌套循环越深，复杂度越高。\n");
            }
        }
        return sb.toString();
    }

    /**
     * 生成JUnit5测试类或追加测试方法
     */
    public String generateJUnit5TestSkeleton(PsiMethod targetMethod) {
        PsiClass owner = targetMethod.getContainingClass();
        if (owner == null || owner.getQualifiedName() == null) {
            return "无法定位目标类";
        }
        String className = owner.getName();
        String testClassName = className + "Test";
        String methodName = targetMethod.getName();

        StringBuilder b = new StringBuilder();
        b.append("import org.junit.jupiter.api.Test;\n");
        b.append("import static org.junit.jupiter.api.Assertions.*;\n\n");
        if (owner.getQualifiedName().contains(".")) {
            b.append("import ").append(owner.getQualifiedName()).append(";\n\n");
        }
        b.append("public class ").append(testClassName).append(" {\n\n");
        b.append("    @Test\n");
        b.append("    public void ").append(methodName).append("_basicCase() {\n");
        b.append("        // TODO: 初始化入参并断言\n");
        b.append("        ").append(className).append(" obj = new ").append(className).append("();\n");
        // 形参初始化
        List<String> args = new ArrayList<>();
        for (PsiParameter parameter : targetMethod.getParameterList().getParameters()) {
            String type = parameter.getType().getPresentableText();
            String name = parameter.getName();
            String init = defaultInit(type);
            b.append("        ").append(type).append(" ").append(name).append(" = ").append(init).append(";\n");
            args.add(name);
        }
        b.append("        var result = obj.").append(methodName).append("(")
                .append(String.join(", ", args)).append(");\n");
        b.append("        // TODO: 使用assertEquals/assertTrue等断言校验结果\n");
        b.append("    }\n\n");
        b.append("}");
        return b.toString();
    }

    private String defaultInit(String type) {
        switch (type) {
            case "int":
            case "short":
            case "byte":
            case "long":
                return "0";
            case "double":
            case "float":
                return "0.0";
            case "boolean":
                return "false";
            case "char":
                return "'\0'";
            default:
                if (type.endsWith("[]")) return "new " + type.replace("[]", "[0]");
                return "null"; // 无法推断具体构造，交由用户完善
        }
    }
}
