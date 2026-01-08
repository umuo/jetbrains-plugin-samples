package cn.lacknb.blog.llm.stream;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

public class MarkdownRenderer {
    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder().build();

    public String toHtml(String markdown) {
        Node node = parser.parse(markdown == null ? "" : markdown);
        String body = renderer.render(node);
        return "<html><body>" + body + "</body></html>";
    }
}
