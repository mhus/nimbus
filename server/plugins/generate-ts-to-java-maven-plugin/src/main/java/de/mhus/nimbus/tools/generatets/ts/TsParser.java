package de.mhus.nimbus.tools.generatets.ts;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight generic TypeScript source parser.
 *
 * It scans .ts files (excluding .d.ts) and extracts:
 * - import specifiers (the raw module string)
 * - declarations: interface, enum, class, type alias (names only for now)
 *
 * This is intentionally simple (regex + brace balancing) to keep the Java side
 * generic and robust. Detailed semantics can be enriched later.
 */
public class TsParser {

    private static final Pattern IMPORT_FROM = Pattern.compile("(?m)\\bimport\\s+[^;]*?from\\s*['\"]([^'\"]+)['\"];?");
    private static final Pattern IMPORT_SIDE_EFFECT = Pattern.compile("(?m)\\bimport\\s*['\"]([^'\"]+)['\"];?");

    private static final Pattern DECL_INTERFACE = Pattern.compile("\\bexport\\s+interface\\s+([A-Za-z0-9_]+)\\b|\\binterface\\s+([A-Za-z0-9_]+)\\b");
    private static final Pattern DECL_ENUM = Pattern.compile("(?m)^\\s*(?:export\\s+)?enum\\s+([A-Za-z0-9_]+)\\s*\\{");
    private static final Pattern DECL_CLASS = Pattern.compile("\\bexport\\s+class\\s+([A-Za-z0-9_]+)\\b|\\bclass\\s+([A-Za-z0-9_]+)\\b");
    private static final Pattern DECL_TYPE = Pattern.compile("(?m)\\bexport\\s+type\\s+([A-Za-z0-9_]+)\\s*=|\\btype\\s+([A-Za-z0-9_]+)\\s*=");

    public TsModel parse(List<File> sourceDirs) throws IOException {
        TsModel model = new TsModel();
        List<File> files = new ArrayList<>();
        for (File dir : sourceDirs) {
            if (dir != null && dir.exists()) {
                collectFiles(dir, files);
            }
        }
        for (File f : files) {
            String content = readFile(f);
            String src = stripComments(content);
            TsSourceFile ts = new TsSourceFile(relativize(f));
            // imports
            extractImports(src, ts);
            // declarations - pass both stripped and ORIGINAL (unstripped) source
            extractDeclarations(src, content, ts);  // content is the original with comments!
            model.addFile(ts);
        }
        return model;
    }

    private void collectFiles(File dir, List<File> out) {
        if (dir.isFile()) {
            if (dir.getName().endsWith(".ts") && !dir.getName().endsWith(".d.ts")) {
                out.add(dir);
            }
            return;
        }
        File[] list = dir.listFiles();
        if (list == null) return;
        for (File f : list) {
            if (f.isDirectory()) collectFiles(f, out);
            else if (f.getName().endsWith(".ts") && !f.getName().endsWith(".d.ts")) out.add(f);
        }
    }

    private String readFile(File f) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    private String stripComments(String src) {
        // Remove /* */ comments
        String noBlock = src.replaceAll("/\\*.*?\\*/", " ");
        // Remove // comments
        return noBlock.replaceAll("(?m)//.*$", " ");
    }

    private void extractImports(String src, TsSourceFile file) {
        Matcher m = IMPORT_FROM.matcher(src);
        while (m.find()) {
            String module = m.group(1);
            if (module != null) file.getImports().add(module);
        }
        Matcher m2 = IMPORT_SIDE_EFFECT.matcher(src);
        while (m2.find()) {
            String module = m2.group(1);
            if (module != null && !file.getImports().contains(module)) file.getImports().add(module);
        }
    }

    private void extractDeclarations(String src, String originalSrc, TsSourceFile file) {
        // Interfaces
        for (NameOccur n : findNamed(src, DECL_INTERFACE, '{')) {
            TsDeclarations.TsInterface d = new TsDeclarations.TsInterface();
            d.name = n.name;
            // Parse header between name and opening '{' to capture extends list
            int braceIdx = src.indexOf('{', n.startIndex);
            if (braceIdx > n.startIndex) {
                String header = src.substring(n.startIndex, braceIdx);
                java.util.regex.Matcher em = java.util.regex.Pattern.compile("\\bextends\\s+([^\\{]+)").matcher(header);
                if (em.find()) {
                    String list = em.group(1);
                    if (list != null) {
                        for (String part : list.split(",")) {
                            String id = part.trim();
                            // strip generic args if any, keep simple identifier
                            int lt = id.indexOf('<');
                            if (lt > 0) id = id.substring(0, lt).trim();
                            id = id.replaceAll("\\s+", "");
                            if (!id.isEmpty()) d.extendsList.add(id);
                        }
                    }
                }
            }
            String body = safeSub(src, n.startIndex, n.endIndex);
            String originalBody = safeSub(originalSrc, n.startIndex, n.endIndex);
            extractPropertiesFromBodyWithOriginal(body, originalBody, d.properties);
            file.getInterfaces().add(d);
        }
        // Enums
        for (NameOccur n : findNamed(src, DECL_ENUM, '{')) {
            TsDeclarations.TsEnum d = new TsDeclarations.TsEnum();
            d.name = n.name;
            String body = safeSub(src, n.startIndex, n.endIndex);
            extractEnumValuesFromBody(body, d.values);
            extractEnumValuesAndAssignments(body, d.enumValues);
            file.getEnums().add(d);
        }
        // Classes
        for (NameOccur n : findNamed(src, DECL_CLASS, '{')) {
            TsDeclarations.TsClass d = new TsDeclarations.TsClass();
            d.name = n.name;
            String body = safeSub(src, n.startIndex, n.endIndex);
            String originalBody = safeSub(originalSrc, n.startIndex, n.endIndex);
            extractPropertiesFromBodyWithOriginal(body, originalBody, d.properties);
            file.getClasses().add(d);
        }
        // Type aliases (end by semicolon)
        for (NameOccur n : findNamed(src, DECL_TYPE, ';')) {
            TsDeclarations.TsTypeAlias d = new TsDeclarations.TsTypeAlias();
            d.name = n.name;
            // Extract target type between '=' and ';'
            String decl = safeSub(src, n.startIndex, n.endIndex);
            if (decl != null) {
                int eq = decl.indexOf('=');
                if (eq >= 0) {
                    String rhs = decl.substring(eq + 1).trim();
                    // remove trailing semicolon if present (safeSub ends before ';' but keep safety)
                    if (rhs.endsWith(";")) rhs = rhs.substring(0, rhs.length() - 1).trim();
                    // collapse multiple spaces
                    rhs = rhs.replaceAll("\n|\r", " ").trim();
                    d.target = rhs.isEmpty() ? null : rhs;
                }
            }
            file.getTypeAliases().add(d);
        }
    }

    private String safeSub(String s, int start, int end) {
        int a = Math.max(0, Math.min(s.length(), start));
        int b = Math.max(a, Math.min(s.length(), end));
        return s.substring(a, b);
    }

    private void extractPropertiesFromBody(String body, List<TsDeclarations.TsProperty> out) {
        // This method is called with stripped source, we need the original for javaType hints
        // For now, use the existing logic without javaType hints
        extractPropertiesFromBodyWithOriginal(body, body, out);
    }

    private void extractPropertiesFromBodyWithOriginal(String strippedBody, String originalBody, List<TsDeclarations.TsProperty> out) {
        if (strippedBody == null || out == null) return;
        // Match TS property declarations of the form: [visibility]? name[?]: type;
        java.util.regex.Pattern propPat = java.util.regex.Pattern.compile("(?ms)^[\\t ]*(public|private|protected)?[\\t ]*([A-Za-z_$][A-Za-z0-9_$]*)[\\t ]*(\\?)?[\\t ]*:[\\t ]*(.+?)\\s*;[\\t ]*");
        java.util.regex.Matcher m = propPat.matcher(strippedBody);
        while (m.find()) {
            int startIdx = m.start();
            // Only accept properties at top-level of the declaration body (depth == 1 inside outer braces)
            if (braceDepthAt(strippedBody, startIdx) != 1) {
                continue;
            }
            // Skip index signatures like [key: string]: any;
            try {
                int nameStart = m.start(2);
                if (nameStart > 0 && strippedBody.charAt(nameStart - 1) == '[') {
                    continue;
                }
                // Skip if there's a '(' before the ':' in the matched segment (likely a method signature)
                int colon = strippedBody.indexOf(':', m.start());
                if (colon > m.start()) {
                    String beforeColon = strippedBody.substring(m.start(), colon);
                    if (beforeColon.contains("(")) {
                        continue;
                    }
                }
            } catch (Exception ignored) {}

            String typeTxt = m.group(4) == null ? null : m.group(4).trim();
            // If type starts with an inline object '{', try to capture the full object literal up to matching '}'
            if (typeTxt != null && typeTxt.startsWith("{")) {
                int absTypeStart = m.start(4);
                int objEnd = findMatchingBrace(strippedBody, absTypeStart);
                if (objEnd > absTypeStart) {
                    typeTxt = strippedBody.substring(absTypeStart, objEnd).trim();
                }
            }
            // Do NOT skip inline object types here; we keep them so the generator can synthesize helper classes.
            // Still skip import(...) types that we cannot resolve sensibly.
            if (typeTxt != null && typeTxt.contains("import(")) {
                continue;
            }

            TsDeclarations.TsProperty pr = new TsDeclarations.TsProperty();
            pr.visibility = m.group(1);
            pr.name = m.group(2);
            pr.optional = m.group(3) != null && !m.group(3).isEmpty();
            pr.type = typeTxt;

            // Extract both comment and javaTypeHint from the original (non-stripped) source
            String[] commentAndHint = extractCommentAndJavaTypeHint(originalBody, pr.name);
            pr.comment = commentAndHint[0];
            pr.javaTypeHint = commentAndHint[1];

            out.add(pr);
        }
    }

    /**
     * Extract both comment and javaType hint from the original source for a property
     * Returns an array with [comment, javaTypeHint] where either can be null
     */
    private String[] extractCommentAndJavaTypeHint(String originalBody, String propertyName) {
        if (originalBody == null || propertyName == null) {
            return new String[]{null, null};
        }

        // Search the original body for a line containing this property name and a comment
        String[] lines = originalBody.split("\n");
        for (String line : lines) {
            // Look for property declaration line - more flexible matching
            if (line.contains(propertyName) && line.contains(":") && line.contains("//")) {
                // Verify this is actually a property declaration, not just any line with the name
                String trimmedLine = line.trim();
                if (trimmedLine.contains(propertyName + ":") ||
                    trimmedLine.matches(".*\\b" + propertyName + "\\s*:.*")) {

                    // Extract the comment part (everything after //)
                    int commentStart = line.lastIndexOf("//");
                    String fullComment = line.substring(commentStart).trim();

                    // Extract javaType hint from this comment
                    String javaTypeHint = parseJavaTypeHintFromLine(line);

                    return new String[]{fullComment, javaTypeHint};
                }
            }
        }

        return new String[]{null, null};
    }

    /**
     * Robust parsing of javaType hints from a TypeScript line
     * Handles various formats: //javaType:type, // javaType: type, //javaType=type
     */
    private String parseJavaTypeHintFromLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }

        // Find the last // comment in the line
        int commentStart = line.lastIndexOf("//");
        if (commentStart == -1) {
            return null;
        }

        String comment = line.substring(commentStart + 2).trim();
        String lowerComment = comment.toLowerCase();

        // Search for "javatype" (case insensitive) followed by : or =
        int javaTypeStart = lowerComment.indexOf("javatype");
        if (javaTypeStart == -1) {
            return null;
        }

        // Find the separator after "javatype"
        int separatorPos = javaTypeStart + "javatype".length();

        // Skip whitespace
        while (separatorPos < lowerComment.length() && Character.isWhitespace(lowerComment.charAt(separatorPos))) {
            separatorPos++;
        }

        // Check for : or =
        if (separatorPos >= lowerComment.length()) {
            return null;
        }

        char separator = lowerComment.charAt(separatorPos);
        if (separator != ':' && separator != '=') {
            return null;
        }

        // Extract the type after the separator
        int typeStart = separatorPos + 1;

        // Skip whitespace after the separator
        while (typeStart < comment.length() && Character.isWhitespace(comment.charAt(typeStart))) {
            typeStart++;
        }

        if (typeStart >= comment.length()) {
            return null;
        }

        String typeStr = comment.substring(typeStart);

        // Remove trailing comments (if any)
        int nextComment = typeStr.indexOf("//");
        if (nextComment != -1) {
            typeStr = typeStr.substring(0, nextComment).trim();
        }

        // Remove trailing whitespace and semicolon
        typeStr = typeStr.replaceAll("[\\s;]*$", "");

        return typeStr.isEmpty() ? null : typeStr;
    }

    /**
     * Find the start of the line containing the given position
     */
    private int findLineStart(String body, int fromPos) {
        for (int i = fromPos - 1; i >= 0; i--) {
            if (body.charAt(i) == '\n') {
                return i + 1;
            }
        }
        return 0; // Beginning of file
    }

    /**
     * Find the start of the next line after the given position
     */
    private int findNextLineStart(String body, int fromPos) {
        for (int i = fromPos; i < body.length(); i++) {
            if (body.charAt(i) == '\n') {
                return i + 1 < body.length() ? i + 1 : -1;
            }
        }
        return -1;
    }

    /**
     * Find the end of the line starting from the given position
     */
    private int findLineEnd(String body, int fromPos) {
        for (int i = fromPos; i < body.length(); i++) {
            if (body.charAt(i) == '\n') {
                return i;
            }
        }
        return body.length();
    }

    /**
     * Compute the brace nesting depth at a given position within a block that includes outer braces.
     * Depth starts at 0 before the first '{'. After the first '{' it becomes 1 for the outer body.
     */
    private int braceDepthAt(String s, int pos) {
        int depth = 0;
        for (int i = 0; i < s.length() && i < pos; i++) {
            char c = s.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') depth = Math.max(0, depth - 1);
        }
        return depth;
    }

    private void extractEnumValuesFromBody(String body, List<String> out) {
        if (body == null || out == null) return;
        // Match enum member names: NAME [= ...] , or NAME at end
        Pattern p = Pattern.compile("(?m)^[\\t ]*([A-Za-z_$][A-Za-z0-9_$]*)[\\t ]*(?:=[^,\\r\\n]+)?[\\t ]*(?:,[\\t ]*)?(?://.*)?$");
        Matcher m = p.matcher(body);
        while (m.find()) {
            String name = m.group(1);
            if (name == null || name.isEmpty()) continue;
            // Filter out potential keywords
            if (!Character.isJavaIdentifierStart(name.charAt(0))) continue;
            boolean ok = true;
            for (int i = 1; i < name.length(); i++) {
                if (!Character.isJavaIdentifierPart(name.charAt(i))) { ok = false; break; }
            }
            if (!ok) continue;
            out.add(name);
        }
    }

    private void extractEnumValuesAndAssignments(String body, List<TsDeclarations.TsEnumValue> out) {
        if (body == null || out == null) return;
        Pattern p = Pattern.compile("(?m)^[\\t ]*([A-Za-z_$][A-Za-z0-9_$]*)[\\t ]*(?:=[\\t ]*(?:(['\"])(.*?)\\2|([^,\\r\\n]+)))?[\\t ]*(?:,[\\t ]*)?(?://.*)?$");
        Matcher m = p.matcher(body);
        while (m.find()) {
            String name = m.group(1);
            if (name == null || name.isEmpty()) continue;
            // Filter out potential keywords
            if (!Character.isJavaIdentifierStart(name.charAt(0))) continue;
            boolean ok = true;
            for (int i = 1; i < name.length(); i++) {
                if (!Character.isJavaIdentifierPart(name.charAt(i))) { ok = false; break; }
            }
            if (!ok) continue;

            String value = null;
            if (m.group(3) != null) {
                // String value in quotes
                value = m.group(3);
            } else if (m.group(4) != null) {
                // Other value (number, etc.) - trim whitespace
                value = m.group(4).trim();
            } else {
                // No assignment, use the name as default value
                value = name;
            }

            out.add(new TsDeclarations.TsEnumValue(name, value));
        }
    }

    private static class NameOccur {
        String name;
        int startIndex;
        int endIndex;
    }

    private List<NameOccur> findNamed(String src, Pattern pat, char endBy) {
        List<NameOccur> out = new ArrayList<>();
        Matcher m = pat.matcher(src);
        while (m.find()) {
            String n1 = m.group(1);
            String n2 = null;
            try { n2 = m.group(2); } catch (Exception ignored) {}
            String name = n1 != null ? n1 : n2;
            if (name == null) continue;

            int start, end;
            if (pat == DECL_ENUM) {
                // For the new ENUM pattern that includes the opening brace
                // The match already includes the opening brace, so find the matching closing brace
                int openBrace = src.lastIndexOf('{', m.end());
                if (openBrace >= 0) {
                    start = openBrace + 1; // Start after the opening brace
                    end = findMatchingBrace(src, openBrace);
                } else {
                    // Fallback if brace not found
                    start = m.end();
                    end = src.indexOf('}', start);
                }
            } else {
                // Original logic for other patterns
                start = m.end();
                end = (endBy == '{') ? findMatchingBrace(src, src.indexOf('{', start)) : src.indexOf(';', start);
            }

            NameOccur o = new NameOccur();
            o.name = name;
            o.startIndex = start;
            o.endIndex = end < 0 ? start : end;
            out.add(o);
        }
        return out;
    }

    private int findMatchingBrace(String src, int openIndex) {
        if (openIndex < 0) return -1;
        Deque<Character> stack = new ArrayDeque<>();
        for (int i = openIndex; i < src.length(); i++) {
            char c = src.charAt(i);
            if (c == '{') stack.push(c);
            else if (c == '}') {
                if (stack.isEmpty()) return i; // unmatched, stop
                stack.pop();
                if (stack.isEmpty()) return i + 1; // end index exclusive
            }
        }
        return -1;
    }

    private String relativize(File f) {
        return f.getPath();
    }
}
