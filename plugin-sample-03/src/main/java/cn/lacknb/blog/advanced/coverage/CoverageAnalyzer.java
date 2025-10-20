package cn.lacknb.blog.advanced.coverage;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.*;

/**
 * 读取JaCoCo XML报告，统计特定方法的行/分支覆盖情况。
 * 默认会在项目根目录下递归搜索常见的JaCoCo报告文件名：
 * - jacoco.xml
 * - jacocoTestReport.xml
 */
public class CoverageAnalyzer {
    private final Project project;
    private static final Set<String> CANDIDATE_NAMES = new HashSet<>(Arrays.asList(
            "jacoco.xml", "jacocoTestReport.xml", "jacocoTestReport/jacocoTestReport.xml"
    ));

    public CoverageAnalyzer(Project project) { this.project = project; }

    public static class MethodCoverage {
        public final Set<Integer> coveredLines = new TreeSet<>();
        public final Set<Integer> missedLines = new TreeSet<>();
        public int coveredBranches = 0;
        public int missedBranches = 0;
    }

    @Nullable
    public MethodCoverage findCoverageForMethod(PsiMethod method) {
        VirtualFile base = ProjectUtil.guessProjectDir(project);
        if (base == null) return null;

        List<VirtualFile> reports = new ArrayList<>();
        VfsUtilCore.visitChildrenRecursively(base, new VfsUtilCore.VirtualFileVisitor<>() {
            @Override
            public boolean visitFile(@org.jetbrains.annotations.NotNull VirtualFile file) {
                if (!file.isDirectory()) {
                    String path = file.getPath();
                    for (String cand : CANDIDATE_NAMES) {
                        if (path.endsWith(cand)) {
                            reports.add(file);
                            break;
                        }
                    }
                }
                return true;
            }
        });
        for (VirtualFile vf : reports) {
            MethodCoverage mc = parseReportFor(method, vf);
            if (mc != null) return mc;
        }
        return null;
    }

    @Nullable
    private MethodCoverage parseReportFor(PsiMethod method, VirtualFile vf) {
        try (InputStream in = vf.getInputStream()) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(in);
            doc.getDocumentElement().normalize();

            PsiClass owner = method.getContainingClass();
            if (owner == null) return null;

            String classInternalName = Objects.requireNonNull(owner.getQualifiedName()).replace('.', '/');
            String methodName = method.getName();
            String methodDesc = toJvmDescriptor(method);

            NodeList packages = doc.getElementsByTagName("package");
            for (int i = 0; i < packages.getLength(); i++) {
                Element pkg = (Element) packages.item(i);
                NodeList classes = pkg.getElementsByTagName("class");
                for (int j = 0; j < classes.getLength(); j++) {
                    Element cls = (Element) classes.item(j);
                    if (!classInternalName.equals(cls.getAttribute("name"))) continue;
                    NodeList methods = cls.getElementsByTagName("method");
                    for (int k = 0; k < methods.getLength(); k++) {
                        Element m = (Element) methods.item(k);
                        if (methodName.equals(m.getAttribute("name")) && methodDesc.equals(m.getAttribute("desc"))) {
                            MethodCoverage mc = new MethodCoverage();
                            // 行级别
                            NodeList lines = m.getElementsByTagName("line");
                            for (int x = 0; x < lines.getLength(); x++) {
                                Element line = (Element) lines.item(x);
                                int nr = Integer.parseInt(line.getAttribute("nr"));
                                int ci = Integer.parseInt(line.getAttribute("ci")); // covered instructions
                                int mi = Integer.parseInt(line.getAttribute("mi")); // missed instructions
                                int cb = Integer.parseInt(line.getAttribute("cb")); // covered branches
                                int mb = Integer.parseInt(line.getAttribute("mb")); // missed branches
                                if (ci > 0) mc.coveredLines.add(nr); else if (mi > 0) mc.missedLines.add(nr);
                                mc.coveredBranches += cb;
                                mc.missedBranches += mb;
                            }
                            return mc;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String toJvmDescriptor(PsiMethod m) {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        for (PsiParameter p : m.getParameterList().getParameters()) {
            sb.append(toJvmType(p.getType()));
        }
        sb.append(')');
        sb.append(toJvmType(m.getReturnType()));
        return sb.toString();
    }

    private String toJvmType(PsiType type) {
        if (type == null || PsiType.VOID.equals(type)) return "V";
        String present = type.getCanonicalText();
        if (present.endsWith("[]")) {
            // 处理一维数组即可
            PsiType elem = PsiType.getTypeByName(present.substring(0, present.length() - 2), project, GlobalSearchScope.allScope(project));
            return "[" + toJvmType(elem);
        }
        switch (present) {
            case "byte": return "B";
            case "char": return "C";
            case "double": return "D";
            case "float": return "F";
            case "int": return "I";
            case "long": return "J";
            case "short": return "S";
            case "boolean": return "Z";
            default:
                return "L" + present.replace('.', '/') + ";";
        }
    }
}
