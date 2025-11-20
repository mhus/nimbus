package de.mhus.nimbus.tools.generatets;

import de.mhus.nimbus.tools.generatets.java.JavaKind;
import de.mhus.nimbus.tools.generatets.java.JavaModel;
import de.mhus.nimbus.tools.generatets.java.JavaType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Writes a simple set of .java files from the JavaModel.
 *
 * This is intentionally minimal: it emits basic type declarations without
 * package, fields or methods. It aims to provide a tangible output for
 * early integration and can be extended later.
 */
public class JavaModelWriter {

    private final JavaModel model;

    public JavaModelWriter(JavaModel model) {
        this.model = model;
    }

    public void write(File outputDir) throws IOException {
        if (model == null || model.getTypes() == null) return;
        if (outputDir == null) throw new IOException("outputDir is null");
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException("Could not create output directory: " + outputDir.getAbsolutePath());
        }

        for (JavaType t : model.getTypes()) {
            if (t == null) continue;
            String name = t.getName();
            if (!isValidJavaIdentifier(name)) continue;

            File javaFile = new File(outputDir, name + ".java");
            try (FileWriter w = new FileWriter(javaFile, false)) {
                w.write(renderType(t));
            }
        }
    }

    private String renderType(JavaType t) {
        StringBuilder sb = new StringBuilder();
        // No package for now; can be added via config later.
        // sb.append("package ...;\n\n");
        String name = t.getName();
        if (t.getKind() == JavaKind.ENUM) {
            sb.append("public enum ").append(name).append(" {\n}");
        } else if (t.getKind() == JavaKind.INTERFACE) {
            sb.append("public interface ").append(name);
            String ext = renderExtends(t.getExtendsName());
            String impls = renderImplements(t.getImplementsNames()); // for interfaces, treat remaining as extends
            if (!ext.isEmpty() || !impls.isEmpty()) {
                // For interfaces, both lists should be combined using 'extends'
                String combined = combineCsv(ext, impls);
                if (!combined.isEmpty()) sb.append(" extends ").append(combined);
            }
            sb.append(" {\n}");
        } else if (t.getKind() == JavaKind.CLASS) {
            sb.append("public class ").append(name);
            String ext = renderExtends(t.getExtendsName());
            if (!ext.isEmpty()) sb.append(" ").append(ext);
            String impls = renderImplements(t.getImplementsNames());
            if (!impls.isEmpty()) sb.append(" ").append(impls);
            sb.append(" {\n}");
        } else if (t.getKind() == JavaKind.TYPE_ALIAS) {
            sb.append("/** Type alias for: ").append(nullToEmpty(t.getAliasTargetName())).append(" */\n");
            sb.append("public class ").append(name).append(" {\n}");
        } else {
            sb.append("public class ").append(name).append(" {\n}");
        }
        sb.append('\n');
        return sb.toString();
    }

    private String renderExtends(String name) {
        if (!isValidJavaIdentifier(name)) return "";
        return "extends " + name;
    }

    private String renderImplements(List<String> names) {
        if (names == null || names.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String n : names) {
            if (!isValidJavaIdentifier(n)) continue;
            if (sb.length() > 0) sb.append(',').append(' ');
            sb.append(n);
        }
        if (sb.length() == 0) return "";
        return "implements " + sb;
    }

    private String combineCsv(String a, String b) {
        if (a == null || a.isEmpty()) return b == null ? "" : b;
        if (b == null || b.isEmpty()) return a;
        return a + ", " + b;
    }

    private boolean isValidJavaIdentifier(String s) {
        if (s == null || s.isEmpty()) return false;
        if (!Character.isJavaIdentifierStart(s.charAt(0))) return false;
        for (int i = 1; i < s.length(); i++) {
            if (!Character.isJavaIdentifierPart(s.charAt(i))) return false;
        }
        return true;
    }

    private String nullToEmpty(String s) { return s == null ? "" : s; }
}
