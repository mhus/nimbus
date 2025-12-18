package de.mhus.nimbus.tools.generatej2ts.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JavaClassModel {
    private String packageName;
    private String name;
    private JavaKind kind = JavaKind.CLASS;
    private String generateSubfolder; // from @GenerateTypeScript("subfolder")
    private final List<JavaFieldModel> fields = new ArrayList<>(); // for CLASS only
    private final List<String> enumConstants = new ArrayList<>(); // for ENUM only
    private final Set<String> typeScriptImports = new HashSet<>(); // from @TypeScriptImport

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public JavaKind getKind() { return kind; }
    public void setKind(JavaKind kind) { this.kind = kind; }

    public String getGenerateSubfolder() { return generateSubfolder; }
    public void setGenerateSubfolder(String generateSubfolder) { this.generateSubfolder = generateSubfolder; }

    public List<JavaFieldModel> getFields() { return fields; }
    public List<String> getEnumConstants() { return enumConstants; }
    public Set<String> getTypeScriptImports() { return typeScriptImports; }
}
