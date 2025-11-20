package de.mhus.nimbus.tools.generatets.java;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class JavaType {

    private String name;
    private JavaKind kind;
    private String sourcePath; // originating TS source file path
    private String packageName; // optional, from configuration
    private String originalTsKind; // e.g. interface, class, enum, type

    // Members
    private List<JavaProperty> properties = new ArrayList<>();

    // Reference names captured during initial pass
    private String extendsName;
    private List<String> implementsNames = new ArrayList<>();
    private String aliasTargetName; // for type alias

    // Resolved links (filled in second pass)
    @JsonIgnore
    private JavaType extendsType;
    @JsonIgnore
    private List<JavaType> implementsTypes = new ArrayList<>();
    @JsonIgnore
    private JavaType aliasTargetType;

    public JavaType() {}

    public JavaType(String name, JavaKind kind, String sourcePath) {
        this.name = name;
        this.kind = kind;
        this.sourcePath = sourcePath;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public JavaKind getKind() { return kind; }
    public void setKind(JavaKind kind) { this.kind = kind; }

    public String getSourcePath() { return sourcePath; }
    public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public String getOriginalTsKind() { return originalTsKind; }
    public void setOriginalTsKind(String originalTsKind) { this.originalTsKind = originalTsKind; }

    public List<JavaProperty> getProperties() { return properties; }

    public String getExtendsName() { return extendsName; }
    public void setExtendsName(String extendsName) { this.extendsName = extendsName; }

    public List<String> getImplementsNames() { return implementsNames; }

    public String getAliasTargetName() { return aliasTargetName; }
    public void setAliasTargetName(String aliasTargetName) { this.aliasTargetName = aliasTargetName; }

    public JavaType getExtendsType() { return extendsType; }
    public void setExtendsType(JavaType extendsType) { this.extendsType = extendsType; }

    public List<JavaType> getImplementsTypes() { return implementsTypes; }

    public JavaType getAliasTargetType() { return aliasTargetType; }
    public void setAliasTargetType(JavaType aliasTargetType) { this.aliasTargetType = aliasTargetType; }
}
