package cn.lacknb.blog.llm.stream;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StreamMarkdownParser {
    private static final Pattern MATCHER_PATTERN = Pattern.compile(
            "```([\\w#+.-]*\\n*)?(.*?)```" +
                    "|```([\\w#+.-]*\\n*)?(.*)" +
                    "|<think>(.*?)</think>" +
                    "|<think>(.*)",
            Pattern.DOTALL
    );

    public static List<MarkdownBlock> parse(String fullText) {
        List<MarkdownBlock> blocks = new ArrayList<>();
        Matcher matcher = MATCHER_PATTERN.matcher(fullText);
        int lastEnd = 0;

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                blocks.add(new MarkdownBlock(
                        MarkdownBlock.Type.TEXT,
                        fullText.substring(lastEnd, matcher.start()),
                        null,
                        true
                ));
            }

            if (matcher.group(1) != null) {
                blocks.add(new MarkdownBlock(
                        MarkdownBlock.Type.CODE,
                        matcher.group(2),
                        matcher.group(1).trim(),
                        true
                ));
            } else if (matcher.group(3) != null || fullText.startsWith("```", matcher.start())) {
                String lang = matcher.group(3) != null ? matcher.group(3).trim() : "";
                blocks.add(new MarkdownBlock(
                        MarkdownBlock.Type.CODE,
                        matcher.group(4),
                        lang,
                        false
                ));
            } else if (matcher.group(5) != null) {
                blocks.add(new MarkdownBlock(
                        MarkdownBlock.Type.THINK,
                        matcher.group(5),
                        null,
                        true
                ));
            } else if (matcher.group(6) != null) {
                blocks.add(new MarkdownBlock(
                        MarkdownBlock.Type.THINK,
                        matcher.group(6),
                        null,
                        false
                ));
            }

            lastEnd = matcher.end();
        }

        if (lastEnd < fullText.length()) {
            blocks.add(new MarkdownBlock(
                    MarkdownBlock.Type.TEXT,
                    fullText.substring(lastEnd),
                    null,
                    true
            ));
        }

        return blocks;
    }
}
