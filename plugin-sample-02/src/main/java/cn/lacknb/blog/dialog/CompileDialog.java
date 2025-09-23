package cn.lacknb.blog.dialog;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CompileDialog extends DialogWrapper {

    private final Project project;
    private final JTextArea codeArea = new JTextArea(25, 80);
    private final JTextArea resultArea = new JTextArea(10, 80);

    public CompileDialog(Project project) {
        super(true);
        this.project = project;
        setTitle("动态编译Java代码 (使用CompilerManager)");
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 5));

        panel.add(new JLabel("输入Java代码:"), BorderLayout.NORTH);
        codeArea.setLineWrap(true);
        codeArea.setWrapStyleWord(true);
        panel.add(new JBScrollPane(codeArea), BorderLayout.CENTER);

        JPanel resultPanel = new JPanel(new BorderLayout(0, 5));
        resultPanel.add(new JLabel("编译结果:"), BorderLayout.NORTH);
        resultArea.setEditable(false);
        resultPanel.add(new JBScrollPane(resultArea), BorderLayout.CENTER);

        panel.add(resultPanel, BorderLayout.SOUTH);

        codeArea.setText("package com.example.test;\n\n" +
                "public class MyTestClass {\n" +
                "    public void sayHello() {\n" +
                "        System.out.println(\"Hello from dynamically compiled class!\")\n" +
                "    }\n" +
                "}");

        return panel;
    }

    @Override
    protected void doOKAction() {
        String code = codeArea.getText();
        if (code.trim().isEmpty()) {
            resultArea.setText("错误：代码不能为空。");
            return;
        }

        String packageName = getPackageName(code);
        String className = getClassName(code);

        if (className == null) {
            resultArea.setText("错误：无法在代码中找到 public class。");
            return;
        }

        File sourceFile = createJavaFile(packageName, className, code);
        if (sourceFile == null) {
            return;
        }

        compileWithCompilerManager(sourceFile);
    }

    private String getPackageName(String code) {
        Pattern pattern = Pattern.compile("package\\s+([\\w.]+);", Pattern.DOTALL); // Added DOTALL flag
        Matcher matcher = pattern.matcher(code);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private String getClassName(String code) {
        Pattern pattern = Pattern.compile("public\\s+class\\s+([\\w]+)", Pattern.DOTALL); // Added DOTALL flag
        Matcher matcher = pattern.matcher(code);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private File createJavaFile(String packageName, String className, String code) {
        try {
            String projectBasePath = project.getBasePath();
            String packagePath = packageName.replace('.', '/');
            File directory = new File(projectBasePath + "/src/main/java/" + packagePath);

            if (!directory.exists()) {
                if (!directory.mkdirs()) {
                    resultArea.setText("错误：创建目录失败: " + directory.getAbsolutePath());
                    return null;
                }
            }
            File sourceFile = new File(directory, className + ".java");
            FileWriter writer = new FileWriter(sourceFile);
            writer.write(code);
            writer.close();
            resultArea.setText("成功创建文件: " + sourceFile.getAbsolutePath() + "\n");
            return sourceFile;
        } catch (IOException e) {
            resultArea.setText("创建文件时出错: " + e.getMessage());
            return null;
        }
    }

    private void compileWithCompilerManager(File sourceFile) {
        ApplicationManager.getApplication().invokeLater(() -> {
            VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(sourceFile);
            if (virtualFile == null) {
                resultArea.append("错误: 找不到对应的虚拟文件 (VirtualFile)。");
                return;
            }

            CompilerManager compilerManager = CompilerManager.getInstance(project);
            resultArea.append("开始使用CompilerManager进行编译...\n");

            compilerManager.compile(new VirtualFile[]{virtualFile}, new CompileStatusNotification() {
                @Override
                public void finished(boolean aborted, int errors, int warnings, CompileContext compileContext) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (errors > 0) {
                            resultArea.append("编译失败!\n");
                            resultArea.append("错误数量: " + errors + "\n");
                            for (com.intellij.openapi.compiler.CompilerMessage message : compileContext.getMessages(com.intellij.openapi.compiler.CompilerMessageCategory.ERROR)) {
                                VirtualFile file = message.getVirtualFile();
                                String fileName = (file != null) ? file.getName() : "Unknown File";
                                int line = -1;

                                Navigatable navigatable = message.getNavigatable();
                                if (navigatable instanceof OpenFileDescriptor) {
                                    // Line numbers in OpenFileDescriptor are 0-based, so we add 1 for display.
                                    line = ((OpenFileDescriptor) navigatable).getLine() + 1;
                                }

                                String detailedMessage = String.format("%s:%d - %s\n",
                                        fileName,
                                        line,
                                        message.getMessage());
                                resultArea.append(detailedMessage);
                            }
                        } else if (aborted) {
                            resultArea.append("编译被中止。");
                        } else {
                            resultArea.append("编译成功!\n");
                            resultArea.append("警告数量: " + warnings + "\n");
                        }
                    });
                }
            });
        });
    }
}
