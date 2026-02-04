package cn.lacknb.blog.llm.stream;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

public class MarkdownRenderer {
    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder()
            .softbreak("<br/>")
            .escapeHtml(true)
            .build();

    public String toHtml(String markdown) {
        Node node = parser.parse(markdown == null ? "" : markdown);
        String body = renderer.render(node);
        body = normalizeListParagraphs(body);
        return "<html><head>" +
                "<style>" +
                "body{text-align:left;margin:0;padding:0;line-height:1.45;word-break:break-word;}" +
                "p{margin:0 0 0.6em 0;}" +
                "ul,ol{margin:0.2em 0 0.6em 1.2em;padding:0;text-align:left;}" +
                "li{margin:0.1em 0;padding:0;}" +
                "code{font-family:monospace;}" +
                "pre{margin:0.4em 0;white-space:pre-wrap;}" +
                "blockquote{margin:0.4em 0;padding:0 0 0 0.8em;border-left:3px solid #CCCCCC;}" +
                "</style>" +
                "</head><body>" + body + "</body></html>";
    }

    private String normalizeListParagraphs(String html) {
        return html.replace("<li><p>", "<li>")
                .replace("</p></li>", "</li>");
    }
}
