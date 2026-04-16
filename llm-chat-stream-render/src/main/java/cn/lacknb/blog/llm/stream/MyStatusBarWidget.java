package cn.lacknb.blog.llm.stream;

import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.CustomStatusBarWidget;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.ui.awt.RelativePoint;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MyStatusBarWidget implements CustomStatusBarWidget {
    private final Project project;
    private final MyAuthService authService;
    private final OAuthLoginService loginService;
    private final JPanel panel;
    private final JLabel label;
    private StatusBar statusBar;

    public MyStatusBarWidget(Project project) {
        this.project = project;
        this.authService = project.getService(MyAuthService.class);
        this.loginService = project.getService(OAuthLoginService.class);
        this.panel = new JPanel(new BorderLayout());
        this.label = new JLabel();
        this.label.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 4, 0, 4));
        this.label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        this.label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleClick(e);
            }
        });
        this.panel.setOpaque(false);
        this.panel.add(label, BorderLayout.CENTER);
        refreshLabel();
        project.getMessageBus().connect(this).subscribe(AuthStatusListener.TOPIC, (AuthStatusListener) (session, message) -> {
            ApplicationManager.getApplication().invokeLater(() -> {
                refreshLabel();
                if (statusBar != null) {
                    statusBar.updateWidget(ID());
                }
            });
        });
    }

    @Override
    public @NotNull String ID() {
        return "llm.chat.auth.status.widget";
    }

    @Override
    public @NotNull JComponent getComponent() {
        return panel;
    }

    @Override
    public void install(@NotNull StatusBar statusBar) {
        this.statusBar = statusBar;
        refreshLabel();
    }

    private void refreshLabel() {
        if (authService.isLoggedIn()) {
            label.setIcon(getWidgetIcon(true));
            label.setText("");
            label.setToolTipText("已登录，点击查看更多操作");
            return;
        }
        label.setIcon(getWidgetIcon(false));
        label.setText(" 未登录");
        label.setToolTipText(authService.getUnauthenticatedMessage());
    }

    private void handleClick(MouseEvent event) {
        if (!authService.isLoggedIn()) {
            loginService.startLogin();
            return;
        }
        if (!project.isInitialized()) {
            Messages.showInfoMessage(project, "项目仍在初始化中，请稍后再试。", "LLM Chat");
            return;
        }
        DefaultActionGroup group = new DefaultActionGroup();
        group.add(new AnAction("退出登录", "退出登录", AllIcons.Actions.Exit) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                authService.setLoggedOut("已退出登录。");
            }
        });
        DataContext dataContext = DataManager.getInstance().getDataContext(label);
        JBPopupFactory.getInstance()
                .createActionGroupPopup(
                        "LLM Chat",
                        group,
                        dataContext,
                        JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                        true
                )
                .show(new RelativePoint(event));
    }

    private static Icon getWidgetIcon(boolean loggedIn) {
        return loggedIn ? AllIcons.General.User : AllIcons.General.Warning;
    }

    @Override
    public void dispose() {
        statusBar = null;
    }
}
