package de.mhus.nimbus.tools.generatets;

import de.mhus.nimbus.tools.generatets.java.JavaKind;
import de.mhus.nimbus.tools.generatets.java.JavaModel;
import de.mhus.nimbus.tools.generatets.java.JavaType;
import de.mhus.nimbus.tools.generatets.ts.TsDeclarations;
import de.mhus.nimbus.tools.generatets.ts.TsModel;
import de.mhus.nimbus.tools.generatets.ts.TsSourceFile;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Creates a JavaModel from the parsed TypeScript model in two steps:
 * 1) Create items (types) from TS declarations, including raw reference names
 * 2) Link references between the created items (extends/implements/alias target)
 */
public class JavaGenerator {

    public JavaModel generate(TsModel tsModel) {
        JavaModel jm = new JavaModel();
        if (tsModel == null || tsModel.getFiles() == null) return jm;

        // Step 1: create items and capture raw reference names
        for (TsSourceFile f : tsModel.getFiles()) {
            String srcPath = f.getPath();
            // Interfaces
            if (f.getInterfaces() != null) {
                for (TsDeclarations.TsInterface i : f.getInterfaces()) {
                    if (i == null || i.name == null) continue;
                    JavaType t = new JavaType(i.name, JavaKind.INTERFACE, srcPath);
                    // Interfaces in TS can extend multiple other interfaces.
                    // Our JavaType supports one extendsName; map the first to extendsName
                    // and keep the remaining in implementsNames to not lose information.
                    if (i.extendsList != null && !i.extendsList.isEmpty()) {
                        t.setExtendsName(i.extendsList.get(0));
                        for (int k = 1; k < i.extendsList.size(); k++) {
                            String n = i.extendsList.get(k);
                            if (n != null) t.getImplementsNames().add(n);
                        }
                    }
                    jm.addType(t);
                }
            }
            // Enums
            if (f.getEnums() != null) {
                for (TsDeclarations.TsEnum e : f.getEnums()) {
                    if (e == null || e.name == null) continue;
                    JavaType t = new JavaType(e.name, JavaKind.ENUM, srcPath);
                    jm.addType(t);
                }
            }
            // Classes
            if (f.getClasses() != null) {
                for (TsDeclarations.TsClass c : f.getClasses()) {
                    if (c == null || c.name == null) continue;
                    JavaType t = new JavaType(c.name, JavaKind.CLASS, srcPath);
                    if (c.extendsClass != null && !c.extendsClass.isEmpty()) {
                        t.setExtendsName(c.extendsClass);
                    }
                    if (c.implementsList != null) {
                        for (String n : c.implementsList) {
                            if (n != null && !n.isEmpty()) t.getImplementsNames().add(n);
                        }
                    }
                    jm.addType(t);
                }
            }
            // Type Aliases
            if (f.getTypeAliases() != null) {
                for (TsDeclarations.TsTypeAlias a : f.getTypeAliases()) {
                    if (a == null || a.name == null) continue;
                    JavaType t = new JavaType(a.name, JavaKind.TYPE_ALIAS, srcPath);
                    if (a.target != null && !a.target.isEmpty()) {
                        t.setAliasTargetName(a.target);
                    }
                    jm.addType(t);
                }
            }
        }

        // Step 2: link references (resolve names to JavaType references)
        Map<String, JavaType> idx = jm.getIndexByName();
        for (JavaType t : jm.getTypes()) {
            // extends
            if (t.getExtendsName() != null) {
                JavaType ref = idx.get(t.getExtendsName());
                if (ref != null) t.setExtendsType(ref);
            }
            // implements
            if (t.getImplementsNames() != null && !t.getImplementsNames().isEmpty()) {
                // de-dup
                Set<String> seen = new HashSet<>();
                for (String n : t.getImplementsNames()) {
                    if (n == null || n.isEmpty()) continue;
                    if (!seen.add(n)) continue;
                    JavaType ref = idx.get(n);
                    if (ref != null) t.getImplementsTypes().add(ref);
                }
            }
            // alias
            if (t.getAliasTargetName() != null) {
                JavaType ref = idx.get(t.getAliasTargetName());
                if (ref != null) t.setAliasTargetType(ref);
            }
        }

        return jm;
    }
}
