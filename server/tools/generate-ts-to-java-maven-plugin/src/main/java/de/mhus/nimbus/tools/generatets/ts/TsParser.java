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
            file.getInterfaces().add(d);
        }
        // Enums
        for (NameOccur n : findNamed(src, DECL_ENUM, '{')) {
            TsDeclarations.TsEnum d = new TsDeclarations.TsEnum();
            d.name = n.name;
            // In a later step, we could parse values.
            file.getEnums().add(d);
        }
        // Classes
        for (NameOccur n : findNamed(src, DECL_CLASS, '{')) {
            TsDeclarations.TsClass d = new TsDeclarations.TsClass();
            d.name = n.name;
            file.getClasses().add(d);
        }
        // Type aliases (end by semicolon)
        for (NameOccur n : findNamed(src, DECL_TYPE, ';')) {
            TsDeclarations.TsTypeAlias d = new TsDeclarations.TsTypeAlias();
            d.name = n.name;
            file.getTypeAliases().add(d);
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
