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
        body = normalizeListParagraphs(body);
        return "<html><head>" +
                "<style>" +
                "body{margin:0;padding:0;}" +
                "p{margin:0;}" +
                "ul,ol{margin:0 0 0 1.2em;padding:0;}" +
                "li{margin:0;padding:0;}" +
                "</style>" +
                "</head><body>" + body + "</body></html>";
    }

    private String normalizeListParagraphs(String html) {
        return html.replace("<li><p>", "<li>")
                .replace("</p></li>", "</li>");
    }
}
