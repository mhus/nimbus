package de.mhus.nimbus.tools.generatets;

import de.mhus.nimbus.tools.generatets.java.JavaKind;
import de.mhus.nimbus.tools.generatets.java.JavaModel;
import de.mhus.nimbus.tools.generatets.java.JavaProperty;
import de.mhus.nimbus.tools.generatets.java.JavaType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Writes a simple set of .java files from the JavaModel.
 *
 * Emits type declarations and basic members derived from TS properties:
 * - Interfaces: getter method signatures for properties (Type getName())
 * - Classes: fields with given visibility (default public)
 */
public class JavaModelWriter {

    private final JavaModel model;
    private final java.util.Map<String, JavaType> indexByName;

    public JavaModelWriter(JavaModel model) {
        this.model = model;
        java.util.Map<String, JavaType> idx = new java.util.HashMap<>();
        if (model != null && model.getTypes() != null) {
            for (JavaType t : model.getTypes()) {
                if (t != null && t.getName() != null) idx.put(t.getName(), t);
            }
        }
        this.indexByName = idx;
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

            File pkgDir = outputDir;
            String pkg = t.getPackageName();
            if (pkg != null && !pkg.isBlank()) {
                String rel = pkg.replace('.', File.separatorChar);
                pkgDir = new File(outputDir, rel);
                if (!pkgDir.exists() && !pkgDir.mkdirs()) {
                    throw new IOException("Could not create package directory: " + pkgDir.getAbsolutePath());
                }
            }

            File javaFile = new File(pkgDir, name + ".java");
            try (FileWriter w = new FileWriter(javaFile, false)) {
                w.write(renderType(t));
            }
        }
    }

    private String renderType(JavaType t) {
        StringBuilder sb = new StringBuilder();
        String pkg = t.getPackageName();
        if (pkg != null && !pkg.isBlank()) {
            sb.append("package ").append(pkg).append(";\n\n");
        }
        String name = t.getName();
        String currentPkg = pkg == null ? "" : pkg;
        if (t.getKind() == JavaKind.ENUM) {
            sb.append("public enum ").append(name).append(" {\n}");
        } else if (t.getKind() == JavaKind.INTERFACE) {
            sb.append("public interface ").append(name);
            String extCsv = renderInterfaceExtendsCsv(t.getExtendsName(), t.getImplementsNames(), currentPkg);
            if (!extCsv.isEmpty()) {
                sb.append(" extends ").append(extCsv);
            }
            sb.append(" {\n");
            // interface members: getters (deduplicate by property name)
            if (t.getProperties() != null) {
                java.util.Set<String> seen = new java.util.HashSet<>();
                for (JavaProperty p : t.getProperties()) {
                    if (p == null || p.getName() == null) continue;
                    if (!seen.add(p.getName())) continue;
                    String methodName = "get" + capitalize(p.getName());
                    String type = p.getType() == null || p.getType().isBlank() ? "Object" : qualifyType(p.getType(), currentPkg);
                    if (isValidJavaIdentifier(methodName)) {
                        sb.append("    ").append(type).append(' ').append(methodName).append("();\n");
                    }
                }
            }
            sb.append("}\n");
        } else if (t.getKind() == JavaKind.CLASS) {
            sb.append("public class ").append(name);
            String ext = renderExtends(t.getExtendsName(), currentPkg);
            if (!ext.isEmpty()) sb.append(" ").append(ext);
            String impls = renderImplements(t.getImplementsNames(), currentPkg);
            if (!impls.isEmpty()) sb.append(" ").append(impls);
            sb.append(" {\n");
            // class members: fields (deduplicate by property name)
            if (t.getProperties() != null) {
                java.util.Set<String> seen = new java.util.HashSet<>();
                for (JavaProperty p : t.getProperties()) {
                    if (p == null || p.getName() == null) continue;
                    if (!isValidJavaIdentifier(p.getName())) continue;
                    if (!seen.add(p.getName())) continue;
                    String vis = p.getVisibility();
                    if (vis == null || vis.isBlank()) vis = "public";
                    String type = p.getType() == null || p.getType().isBlank() ? "Object" : qualifyType(p.getType(), currentPkg);
                    sb.append("    ").append(vis).append(' ').append(type).append(' ').append(p.getName()).append(";\n");
                }
            }
            sb.append("}\n");
        } else if (t.getKind() == JavaKind.TYPE_ALIAS) {
            sb.append("/** Type alias for: ").append(nullToEmpty(t.getAliasTargetName())).append(" */\n");
            sb.append("public class ").append(name).append(" {\n}\n");
        } else {
            sb.append("public class ").append(name).append(" {\n}\n");
        }
        return sb.toString();
    }

    private String renderExtends(String name, String currentPkg) {
        String q = qualifyType(name, currentPkg);
        if (!isValidJavaIdentifier(baseType(q))) return "";
        return "extends " + q;
    }

    private String renderImplements(List<String> names, String currentPkg) {
        if (names == null || names.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String n : names) {
            String q = qualifyType(n, currentPkg);
            if (!isValidJavaIdentifier(baseType(q))) continue;
            if (sb.length() > 0) sb.append(',').append(' ');
            sb.append(q);
        }
        if (sb.length() == 0) return "";
        return "implements " + sb;
    }

    // For interfaces, combine extends and implements lists into a single comma-separated extends list
    private String renderInterfaceExtendsCsv(String extendsName, List<String> implementsNames, String currentPkg) {
        StringBuilder sb = new StringBuilder();
        if (extendsName != null && !extendsName.isBlank()) {
            String e = qualifyType(extendsName, currentPkg);
            if (isValidJavaIdentifier(baseType(e))) sb.append(e);
        }
        if (implementsNames != null) {
            for (String n : implementsNames) {
                if (n == null || n.isBlank()) continue;
                String q = qualifyType(n, currentPkg);
                if (!isValidJavaIdentifier(baseType(q))) continue;
                if (sb.length() > 0) sb.append(", ");
                sb.append(q);
            }
        }
        return sb.toString();
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
        if (isJavaKeyword(s)) return false;
        return true;
    }

    private boolean isJavaKeyword(String s) {
        // Java keywords and literals that cannot be used as identifiers
        switch (s) {
            case "abstract": case "assert": case "boolean": case "break": case "byte":
            case "case": case "catch": case "char": case "class": case "const":
            case "continue": case "default": case "do": case "double": case "else":
            case "enum": case "extends": case "final": case "finally": case "float":
            case "for": case "goto": case "if": case "implements": case "import":
            case "instanceof": case "int": case "interface": case "long": case "native":
            case "new": case "package": case "private": case "protected": case "public":
            case "return": case "short": case "static": case "strictfp": case "super":
            case "switch": case "synchronized": case "this": case "throw": case "throws":
            case "transient": case "try": case "void": case "volatile": case "while":
            case "true": case "false": case "null":
                return true;
            default:
                return false;
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String nullToEmpty(String s) { return s == null ? "" : s; }

    private String qualifyType(String type, String currentPkg) {
        if (type == null || type.isBlank()) return "Object";
        String s = type.trim();
        // Handle generics like A<B,C<D>>
        int lt = s.indexOf('<');
        if (lt >= 0 && s.endsWith(">")) {
            String raw = s.substring(0, lt).trim();
            String args = s.substring(lt + 1, s.length() - 1);
            String qRaw = qualifySimple(raw, currentPkg);
            String[] parts = splitTopLevel(args, ',');
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(qualifyType(parts[i].trim(), currentPkg));
            }
            return qRaw + "<" + sb + ">";
        }
        return qualifySimple(s, currentPkg);
    }

    private String qualifySimple(String name, String currentPkg) {
        if (name == null || name.isBlank()) return "Object";
        String n = name.trim();
        // Already fully qualified or java.*
        if (n.contains(".")) {
            return n;
        }
        // java.lang common types
        switch (n) {
            case "String": case "Integer": case "Long": case "Double": case "Float":
            case "Short": case "Byte": case "Character": case "Boolean": case "Object":
                return n;
        }
        // java.util common raw types
        if ("List".equals(n)) return "java.util.List";
        if ("Map".equals(n)) return "java.util.Map";
        if ("Set".equals(n)) return "java.util.Set";
        // Lookup generated type by simple name
        JavaType t = indexByName.get(n);
        if (t != null) {
            String pkg = t.getPackageName();
            if (pkg == null || pkg.isBlank() || pkg.equals(currentPkg)) return n;
            return pkg + '.' + n;
        }
        return n;
    }

    private String baseType(String type) {
        if (type == null) return null;
        int lt = type.indexOf('<');
        String raw = lt >= 0 ? type.substring(0, lt) : type;
        int dot = raw.lastIndexOf('.');
        return dot >= 0 ? raw.substring(dot + 1) : raw;
    }

    private String[] splitTopLevel(String s, char delimiter) {
        java.util.List<String> parts = new java.util.ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '<') depth++;
            else if (c == '>') depth--;
            else if (c == delimiter && depth == 0) {
                parts.add(s.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(s.substring(start));
        return parts.toArray(new String[0]);
    }
}
