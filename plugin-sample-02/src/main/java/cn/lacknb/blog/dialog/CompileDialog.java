package cn.lacknb.blog.dialog;

import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.junit.JUnitConfiguration;
import com.intellij.execution.junit.JUnitConfigurationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectFileIndex;
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
    private final JButton runButton = new JButton("运行测试");

    private String fullyQualifiedClassName;
    private String testMethodName;
    private VirtualFile sourceVirtualFile;

    public CompileDialog(Project project) {
        super(true);
        this.project = project;
        setTitle("在当前项目环境中编译和运行");
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        codeArea.setText("package com.example.test;\n\n" +
                "import org.junit.Test;\n" +
                "import static org.junit.Assert.*;\n\n" +
                "public class MyNativeTest {\n\n" +
                "    @Test\n" +
                "    public void nativeTest() {\n" +
                "        System.out.println(\"Test running via native IDE runner!\");\n" +
                "        assertEquals(4, 2 + 2);\n" +
                "    }\n" +
                "}");
        panel.add(new JBScrollPane(codeArea), BorderLayout.CENTER);
        resultArea.setEditable(false);
        JPanel resultPanel = new JPanel(new BorderLayout(0,5));
        resultPanel.add(new JLabel("结果:"), BorderLayout.NORTH);
        resultPanel.add(new JBScrollPane(resultArea), BorderLayout.CENTER);
        panel.add(resultPanel, BorderLayout.SOUTH);
        return panel;
    }

    @Override
    protected JComponent createSouthPanel() {
        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        runButton.setEnabled(false);
        runButton.addActionListener(e -> doRunAction());
        JButton compileButton = new JButton("编译");
        compileButton.addActionListener(e -> doOKAction());
        JButton cancelButton = new JButton("取消");
        cancelButton.addActionListener(e -> doCancelAction());
        southPanel.add(runButton);
        southPanel.add(compileButton);
        southPanel.add(cancelButton);
        return southPanel;
    }

    @Override
    public void doOKAction() {
        runButton.setEnabled(false);
        resultArea.setText("");
        String code = codeArea.getText();
        String packageName = getPackageName(code);
        String className = getClassName(code);
        this.fullyQualifiedClassName = packageName.isEmpty() ? className : packageName + "." + className;
        File sourceFile = createJavaFileInProject(packageName, className, code);
        if (sourceFile == null) {
            return;
        }
        this.sourceVirtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(sourceFile);
        if (sourceVirtualFile == null) {
            resultArea.setText("错误: 无法将IOFile转换为VirtualFile。");
            return;
        }
        compileWithCompilerManager(sourceVirtualFile);
    }

    private void doRunAction() {
        Module module = ProjectFileIndex.getInstance(project).getModuleForFile(sourceVirtualFile);
        if (module == null) {
            resultArea.append("\n错误: 找不到文件所属的模块。无法确定类路径。");
            return;
        }

        RunManager runManager = RunManager.getInstance(project);
        JUnitConfigurationType jUnitConfigurationType = JUnitConfigurationType.getInstance();
        RunnerAndConfigurationSettings settings = runManager.createConfiguration(
                "[Dynamic] " + testMethodName,
                jUnitConfigurationType.getConfigurationFactories()[0]
        );

        JUnitConfiguration configuration = (JUnitConfiguration) settings.getConfiguration();
        configuration.setModule(module);

        JUnitConfiguration.Data data = configuration.getPersistentData();
        data.TEST_OBJECT = JUnitConfiguration.TEST_METHOD;
        data.MAIN_CLASS_NAME = fullyQualifiedClassName;
        data.METHOD_NAME = testMethodName;

        ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance());
        close(DialogWrapper.OK_EXIT_CODE);
    }

    private File createJavaFileInProject(String packageName, String className, String code) {
        Module[] modules = ModuleManager.getInstance(project).getModules();
        if (modules.length == 0) {
            resultArea.setText("错误: 当前项目没有模块。");
            return null;
        }
        VirtualFile sourceRoot = null;
        for (Module module : modules) {
            VirtualFile[] sourceRoots = ModuleRootManager.getInstance(module).getSourceRoots(false);
            if (sourceRoots.length > 0) {
                sourceRoot = sourceRoots[0];
                break;
            }
        }
        if (sourceRoot == null) {
            resultArea.setText("错误: 在项目中找不到任何源码根目录。");
            return null;
        }
        try {
            File directory = new File(sourceRoot.getPath() + "/" + packageName.replace('.', '/'));
            if (!directory.exists()) {
                directory.mkdirs();
            }
            File sourceFile = new File(directory, className + ".java");
            try (FileWriter writer = new FileWriter(sourceFile)) {
                writer.write(code);
            }
            resultArea.setText("文件已创建于: " + sourceFile.getAbsolutePath() + "\n");
            return sourceFile;
        } catch (IOException e) {
            resultArea.setText("创建文件时出错: " + e.getMessage());
            return null;
        }
    }

    private void compileWithCompilerManager(VirtualFile virtualFile) {
        ApplicationManager.getApplication().invokeLater(() -> {
            resultArea.append("开始使用项目环境进行编译...\n");
            CompilerManager.getInstance(project).compile(new VirtualFile[]{virtualFile}, (aborted, errors, warnings, compileContext) -> {
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (errors > 0) {
                        resultArea.append("编译失败!\n");
                        for (com.intellij.openapi.compiler.CompilerMessage message : compileContext.getMessages(com.intellij.openapi.compiler.CompilerMessageCategory.ERROR)) {
                            VirtualFile file = message.getVirtualFile();
                            String fileName = (file != null) ? file.getName() : "Unknown File";
                            int line = -1;
                            Navigatable navigatable = message.getNavigatable();
                            if (navigatable instanceof OpenFileDescriptor) {
                                line = ((OpenFileDescriptor) navigatable).getLine() + 1;
                            }
                            resultArea.append(String.format("%s:%d - %s\n", fileName, line, message.getMessage()));
                        }
                    } else if (!aborted) {
                        resultArea.append("编译成功!\n");
                        findTestMethod(codeArea.getText());
                    }
                });
            });
        });
    }

    private void findTestMethod(String code) {
        Pattern pattern = Pattern.compile("@Test\\s+public\\s+void\\s+([\\w]+)\\s*\\(");
        Matcher matcher = pattern.matcher(code);
        if (matcher.find()) {
            this.testMethodName = matcher.group(1);
            resultArea.append(String.format("发现 @Test 方法: %s\n", testMethodName));
            runButton.setEnabled(true);
        } else {
            this.testMethodName = null;
            resultArea.append("警告: 未发现 @Test 方法。\n");
        }
    }

    private String getPackageName(String code) {
        Pattern pattern = Pattern.compile("package\\s+([\\w.]+);\n");
        Matcher matcher = pattern.matcher(code);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String getClassName(String code) {
        Pattern pattern = Pattern.compile("\\bpublic\\s+(?:final\\s+|abstract\\s+)?class\\s+(\\w+)");
        Matcher matcher = pattern.matcher(code);
        return matcher.find() ? matcher.group(1) : null;
    }
}
