package cn.lacknb.blog.llm.stream;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StreamMarkdownParser {
    private static final Pattern MATCHER_PATTERN = Pattern.compile(
            "<tool(?:_call)?\\s+name=\"([^\"]+)\"\\s*>(.*?)</tool(?:_call)?>"
                    + "|<tool(?:_call)?\\s+name=\"([^\"]+)\"\\s*>(.*)"
                    + "|```([\\w#+.-]*\\n*)?(.*?)```"
                    + "|```([\\w#+.-]*\\n*)?(.*)"
                    + "|<think>(.*?)</think>"
                    + "|<think>(.*)",
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
                        null,
                        true
                ));
            }

            if (matcher.group(1) != null) {
                blocks.add(new MarkdownBlock(
                        MarkdownBlock.Type.TOOL,
                        matcher.group(2),
                        null,
                        matcher.group(1),
                        true
                ));
            } else if (matcher.group(3) != null) {
                blocks.add(new MarkdownBlock(
                        MarkdownBlock.Type.TOOL,
                        matcher.group(4),
                        null,
                        matcher.group(3),
                        false
                ));
            } else if (matcher.group(5) != null) {
                blocks.add(new MarkdownBlock(
                        MarkdownBlock.Type.CODE,
                        matcher.group(6),
                        matcher.group(5).trim(),
                        null,
                        true
                ));
            } else if (matcher.group(7) != null || fullText.startsWith("```", matcher.start())) {
                String lang = matcher.group(7) != null ? matcher.group(7).trim() : "";
                blocks.add(new MarkdownBlock(
                        MarkdownBlock.Type.CODE,
                        matcher.group(8),
                        lang,
                        null,
                        false
                ));
            } else if (matcher.group(9) != null) {
                blocks.add(new MarkdownBlock(
                        MarkdownBlock.Type.THINK,
                        matcher.group(9),
                        null,
                        null,
                        true
                ));
            } else if (matcher.group(10) != null) {
                blocks.add(new MarkdownBlock(
                        MarkdownBlock.Type.THINK,
                        matcher.group(10),
                        null,
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
                    null,
                    true
            ));
        }

        return blocks;
    }
}
