package cn.lacknb.blog.llm.stream;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

public class AuthStartupActivity implements StartupActivity {
    @Override
    public void runActivity(@NotNull Project project) {
        project.getService(MyAuthService.class).restoreSession();
    }
}
