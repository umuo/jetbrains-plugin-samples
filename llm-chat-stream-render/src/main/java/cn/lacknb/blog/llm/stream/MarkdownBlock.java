package cn.lacknb.blog.llm.stream;

import java.util.Objects;

public class MarkdownBlock {
    public enum Type { TEXT, CODE, THINK }

    private final Type type;
    private final String content;
    private final String language;
    private final boolean completed;

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
                && Objects.equals(language, that.language);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, content, language, completed);
    }
}
