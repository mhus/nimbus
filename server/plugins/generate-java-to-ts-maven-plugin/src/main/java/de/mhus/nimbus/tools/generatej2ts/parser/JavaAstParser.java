package de.mhus.nimbus.tools.generatej2ts.parser;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.type.Type;
import de.mhus.nimbus.tools.generatej2ts.model.JavaClassModel;
import de.mhus.nimbus.tools.generatej2ts.model.JavaFieldModel;
import de.mhus.nimbus.tools.generatej2ts.model.JavaKind;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;

public class JavaAstParser {

    public static CompilationUnit parseCu(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return StaticJavaParser.parse(fis);
        }
    }

    public static boolean hasGenerateTypeScriptAnnotation(TypeDeclaration<?> type) {
        return type.getAnnotations().stream().anyMatch(a -> simpleName(a.getNameAsString()).equals("GenerateTypeScript"));
    }

    public static JavaClassModel toModel(CompilationUnit cu, TypeDeclaration<?> typeDecl) {
        JavaClassModel model = new JavaClassModel();
        model.setPackageName(cu.getPackageDeclaration().map(pd -> pd.getName().toString()).orElse(null));
        model.setName(typeDecl.getNameAsString());

        // @GenerateTypeScript("subfolder") or value="subfolder"
        extractGenerateTypeScriptSubfolder(typeDecl).ifPresent(model::setGenerateSubfolder);

        if (typeDecl instanceof EnumDeclaration enumDecl) {
            model.setKind(JavaKind.ENUM);
            for (EnumConstantDeclaration c : enumDecl.getEntries()) {
                model.getEnumConstants().add(c.getNameAsString());
            }
            // class-level imports for TS (optional)
            extractTypeScriptImport(typeDecl).ifPresent(s -> {
                for (String line : s.split("\n")) {
                    model.getTypeScriptImports().add(line);
                }
            });
            return model;
        }

        if (typeDecl instanceof ClassOrInterfaceDeclaration clazz) {
            model.setKind(JavaKind.CLASS);

            // @TypeScriptImport on class
            extractTypeScriptImport(typeDecl).ifPresent(s -> {
                for (String line : s.split("\n")) {
                    model.getTypeScriptImports().add(line);
                }
            });

            for (BodyDeclaration<?> bd : clazz.getMembers()) {
                if (!(bd instanceof FieldDeclaration fd)) continue;
                // only fields considered
                NodeList<com.github.javaparser.ast.body.VariableDeclarator> vars = fd.getVariables();
                if (vars == null) continue;
                for (var v : vars) {
                    JavaFieldModel f = new JavaFieldModel();
                    f.setName(v.getNameAsString());
                    Type t = v.getType();
                    f.setJavaType(t.asString());
                    // analyze annotations on the field
                    for (AnnotationExpr an : fd.getAnnotations()) {
                        String n = simpleName(an.getNameAsString());
                        if (n.equals("TypeScript")) {
                            // read attributes: follow, type, import, ignore, optional
                            f.setFollow(getBooleanAttribute(an, "follow").orElse(false));
                            getStringAttribute(an, "type").ifPresent(f::setTsTypeOverride);
                            // "import" ist als Attributname in Java nicht zulässig. Prüfe alternative Namen.
                            getStringAttribute(an, "import").ifPresent(f::setImportOverride);
                            if (f.getImportOverride() == null || f.getImportOverride().isBlank()) {
                                getStringAttribute(an, "tsImport").ifPresent(f::setImportOverride);
                            }
                            if (f.getImportOverride() == null || f.getImportOverride().isBlank()) {
                                getStringAttribute(an, "import_").ifPresent(f::setImportOverride);
                            }
                            if (f.getImportOverride() == null || f.getImportOverride().isBlank()) {
                                getStringAttribute(an, "importPath").ifPresent(f::setImportOverride);
                            }
                            if (f.getImportOverride() == null || f.getImportOverride().isBlank()) {
                                getStringAttribute(an, "importValue").ifPresent(f::setImportOverride);
                            }
                            if (f.getImportOverride() == null || f.getImportOverride().isBlank()) {
                                getStringAttribute(an, "importAs").ifPresent(f::setImportOverride);
                            }
                            f.setIgnored(getBooleanAttribute(an, "ignore").orElse(false));
                            f.setOptional(getBooleanAttribute(an, "optional").orElse(false));
                            getStringAttribute(an, "description").ifPresent(f::setDescription);
                        }
                    }
                    // try to collect referenced types from the raw java type (for follow)
                    TypeNameExtractor.extractReferencedSimpleTypes(f.getJavaType()).forEach(rt -> f.getReferencedTypes().add(rt));
                    model.getFields().add(f);
                }
            }
        }

        return model;
    }

    private static Optional<String> extractGenerateTypeScriptSubfolder(TypeDeclaration<?> type) {
        for (AnnotationExpr an : type.getAnnotations()) {
            String n = simpleName(an.getNameAsString());
            if (n.equals("GenerateTypeScript")) {
                // Single value case
                if (an instanceof SingleMemberAnnotationExpr sm) {
                    return Optional.of(stripQuotes(sm.getMemberValue().toString()));
                }
                if (an instanceof NormalAnnotationExpr nn) {
                    for (MemberValuePair p : nn.getPairs()) {
                        if (p.getNameAsString().equals("value")) {
                            return Optional.of(stripQuotes(p.getValue().toString()));
                        }
                    }
                }
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private static Optional<String> extractTypeScriptImport(TypeDeclaration<?> type) {
        for (AnnotationExpr an : type.getAnnotations()) {
            String n = simpleName(an.getNameAsString());
            if (n.equals("TypeScriptImport")) {
                if (an instanceof SingleMemberAnnotationExpr sm) {
                    Expression val = sm.getMemberValue();
                    if (val instanceof ArrayInitializerExpr arr) {
                        StringBuilder sb = new StringBuilder();
                        for (Expression e : arr.getValues()) {
                            if (!sb.isEmpty()) sb.append('\n');
                            sb.append(stripQuotes(e.toString()));
                        }
                        return Optional.of(sb.toString());
                    } else {
                        return Optional.of(stripQuotes(val.toString()));
                    }
                }
                if (an instanceof NormalAnnotationExpr nn) {
                    for (MemberValuePair p : nn.getPairs()) {
                        if (p.getNameAsString().equals("value")) {
                            Expression val = p.getValue();
                            if (val instanceof ArrayInitializerExpr arr) {
                                StringBuilder sb = new StringBuilder();
                                for (Expression e : arr.getValues()) {
                                    if (!sb.isEmpty()) sb.append('\n');
                                    sb.append(stripQuotes(e.toString()));
                                }
                                return Optional.of(sb.toString());
                            } else {
                                return Optional.of(stripQuotes(val.toString()));
                            }
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<Boolean> getBooleanAttribute(AnnotationExpr an, String name) {
        if (an instanceof NormalAnnotationExpr nn) {
            for (MemberValuePair p : nn.getPairs()) {
                if (p.getNameAsString().equals(name)) {
                    String s = p.getValue().toString();
                    if ("true".equalsIgnoreCase(s)) return Optional.of(true);
                    if ("false".equalsIgnoreCase(s)) return Optional.of(false);
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<String> getStringAttribute(AnnotationExpr an, String name) {
        if (an instanceof NormalAnnotationExpr nn) {
            for (MemberValuePair p : nn.getPairs()) {
                if (p.getNameAsString().equals(name)) {
                    return Optional.of(stripQuotes(p.getValue().toString()));
                }
            }
        }
        return Optional.empty();
    }

    private static String simpleName(String name) {
        int i = name.lastIndexOf('.');
        return i >= 0 ? name.substring(i + 1) : name;
    }

    private static String stripQuotes(String s) {
        if (s == null) return null;
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }
}
