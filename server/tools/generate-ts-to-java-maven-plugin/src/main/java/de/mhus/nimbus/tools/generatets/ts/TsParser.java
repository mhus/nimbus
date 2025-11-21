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
    private static final Pattern DECL_ENUM = Pattern.compile("\\bexport\\s+enum\\s+([A-Za-z0-9_]+)\\b|\\benum\\s+([A-Za-z0-9_]+)\\b");
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
            // declarations
            extractDeclarations(src, ts);
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

    private void extractDeclarations(String src, TsSourceFile file) {
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
            extractPropertiesFromBody(body, d.properties);
            file.getInterfaces().add(d);
        }
        // Enums
        for (NameOccur n : findNamed(src, DECL_ENUM, '{')) {
            TsDeclarations.TsEnum d = new TsDeclarations.TsEnum();
            d.name = n.name;
            String body = safeSub(src, n.startIndex, n.endIndex);
            extractEnumValuesFromBody(body, d.values);
            file.getEnums().add(d);
        }
        // Classes
        for (NameOccur n : findNamed(src, DECL_CLASS, '{')) {
            TsDeclarations.TsClass d = new TsDeclarations.TsClass();
            d.name = n.name;
            String body = safeSub(src, n.startIndex, n.endIndex);
            extractPropertiesFromBody(body, d.properties);
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
        if (body == null || out == null) return;
        // Very simple matcher for properties: [visibility]? name[?]: type; and exclude lines with '(' before ':' to avoid methods
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(?m)^[\\t ]*(public|private|protected)?[\\t ]*([A-Za-z_$][A-Za-z0-9_$]*)[\\t ]*(\\?)?[\\t ]*:[\\t ]*([^;\\r\\n]+)[\\t ]*;[\\t ]*");
        java.util.regex.Matcher m = p.matcher(body);
        while (m.find()) {
            // Skip index signatures like [key: string]: any;
            try {
                int nameStart = m.start(2);
                if (nameStart > 0 && body.charAt(nameStart - 1) == '[') {
                    continue;
                }
                // Skip if there's a '(' before the ':' in the matched segment (likely a method signature)
                int colon = body.indexOf(':', m.start());
                if (colon > m.start()) {
                    String beforeColon = body.substring(m.start(), colon);
                    if (beforeColon.contains("(")) {
                        continue;
                    }
                }
            } catch (Exception ignored) {}
            String typeTxt = m.group(4) == null ? null : m.group(4).trim();
            // Skip complex inline object types or import() types that tend to break naive generation
            if (typeTxt != null && (typeTxt.contains("{") || typeTxt.contains("}") || typeTxt.contains("import("))) {
                continue;
            }
            TsDeclarations.TsProperty pr = new TsDeclarations.TsProperty();
            pr.visibility = m.group(1);
            pr.name = m.group(2);
            pr.optional = m.group(3) != null && !m.group(3).isEmpty();
            pr.type = typeTxt;
            out.add(pr);
        }
    }

    private void extractEnumValuesFromBody(String body, java.util.List<String> out) {
        if (body == null || out == null) return;
        // Match enum member names: NAME [= ...] , or NAME at end
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(?m)^[\\t ]*([A-Za-z_$][A-Za-z0-9_$]*)[\\t ]*(?:=[^,\\r\\n]+)?[\\t ]*(?:,[\\t ]*)?(?://.*)?$");
        java.util.regex.Matcher m = p.matcher(body);
        while (m.find()) {
            String name = m.group(1);
            if (name == null || name.isEmpty()) continue;
            // Filter out potential keywords
            if (!java.lang.Character.isJavaIdentifierStart(name.charAt(0))) continue;
            boolean ok = true;
            for (int i = 1; i < name.length(); i++) {
                if (!java.lang.Character.isJavaIdentifierPart(name.charAt(i))) { ok = false; break; }
            }
            if (!ok) continue;
            out.add(name);
        }
    }

    private static class NameOccur { String name; int startIndex; int endIndex; }

    private List<NameOccur> findNamed(String src, Pattern pat, char endBy) {
        List<NameOccur> out = new ArrayList<>();
        Matcher m = pat.matcher(src);
        while (m.find()) {
            String n1 = m.group(1);
            String n2 = null;
            try { n2 = m.group(2); } catch (Exception ignored) {}
            String name = n1 != null ? n1 : n2;
            if (name == null) continue;
            int start = m.end();
            int end = (endBy == '{') ? findMatchingBrace(src, src.indexOf('{', start)) : src.indexOf(';', start);
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
