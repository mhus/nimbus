package de.mhus.nimbus.tools.generatej2ts.model;

import java.util.HashSet;
import java.util.Set;

public class JavaFieldModel {
    private String name;
    private String javaType; // raw Java type from source
    private String tsTypeOverride; // from @TypeScript(type="...") optional
    private boolean optional;
    private boolean ignored;
    private boolean follow;
    private String importOverride; // from @TypeScript(import="...")

    private final Set<String> referencedTypes = new HashSet<>();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getJavaType() { return javaType; }
    public void setJavaType(String javaType) { this.javaType = javaType; }

    public String getTsTypeOverride() { return tsTypeOverride; }
    public void setTsTypeOverride(String tsTypeOverride) { this.tsTypeOverride = tsTypeOverride; }

    public boolean isOptional() { return optional; }
    public void setOptional(boolean optional) { this.optional = optional; }

    public boolean isIgnored() { return ignored; }
    public void setIgnored(boolean ignored) { this.ignored = ignored; }

    public boolean isFollow() { return follow; }
    public void setFollow(boolean follow) { this.follow = follow; }

    public String getImportOverride() { return importOverride; }
    public void setImportOverride(String importOverride) { this.importOverride = importOverride; }

    public Set<String> getReferencedTypes() { return referencedTypes; }
}
