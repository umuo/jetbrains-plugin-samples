package cn.lacknb.blog.llm.stream;

import java.util.Objects;

public class MarkdownBlock {
    public enum Type { TEXT, CODE, THINK, TOOL }

    private final Type type;
    private final String content;
    private final String language;
    private String toolName;
    private final boolean completed;

    public MarkdownBlock(Type type, String content, String language, String toolName, boolean completed) {
        this.type = type;
        this.content = content;
        this.language = language;
        this.toolName = toolName;
        this.completed = completed;
    }

    public MarkdownBlock(Type type, String content, String language, boolean completed) {
        this.type = type;
        this.content = content;
        this.language = language;
        this.completed = completed;
    }

    public Type getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public String getLanguage() {
        return language;
    }

    public String getToolName() {
        return toolName;
    }

    public boolean isCompleted() {
        return completed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MarkdownBlock that = (MarkdownBlock) o;
        return completed == that.completed
                && type == that.type
                && Objects.equals(content, that.content)
                && Objects.equals(language, that.language)
                && Objects.equals(toolName, that.toolName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, content, language, toolName, completed);
    }
}
