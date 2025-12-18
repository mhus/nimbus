package de.mhus.nimbus.tools.generatej2ts;

import de.mhus.nimbus.tools.generatej2ts.model.JavaClassModel;
import de.mhus.nimbus.tools.generatej2ts.model.JavaFieldModel;
import de.mhus.nimbus.tools.generatej2ts.model.JavaKind;
import de.mhus.nimbus.tools.generatej2ts.ts.TypeScriptField;
import de.mhus.nimbus.tools.generatej2ts.ts.TypeScriptKind;
import de.mhus.nimbus.tools.generatej2ts.ts.TypeScriptModel;
import de.mhus.nimbus.tools.generatej2ts.ts.TypeScriptType;
import org.apache.maven.plugin.logging.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Erzeugt ein TypeScriptModel aus einer Liste von JavaClassModel.
 * Minimalversion mit einfacher Typabbildung und Beachtung von Annotation-Overrides.
 */
public class TypeScriptGenerator {

    private final Log log;

    public TypeScriptGenerator(Log log) {
        this.log = log;
    }

    public TypeScriptModel generate(List<JavaClassModel> javaModels) {
        TypeScriptModel tsModel = new TypeScriptModel();
        if (javaModels == null) return tsModel;

        for (JavaClassModel jm : javaModels) {
            if (jm == null) continue;
            TypeScriptType tt = new TypeScriptType();
            tt.setName(jm.getName());
            tt.setSubfolder(jm.getGenerateSubfolder());
            // Übernehme Import-Zeilen
            tt.getImports().addAll(jm.getTypeScriptImports());

            if (jm.getKind() == JavaKind.ENUM) {
                tt.setKind(TypeScriptKind.ENUM);
                tt.getEnumValues().addAll(jm.getEnumConstants());
            } else {
                tt.setKind(TypeScriptKind.INTERFACE);
                for (JavaFieldModel f : jm.getFields()) {
                    if (f == null) continue;
                    if (f.isIgnored()) continue;
                    String tsType = resolveTsType(f);
                    boolean optional = f.isOptional();
                    tt.getFields().add(new TypeScriptField(f.getName(), tsType, optional));
                }
            }

            tsModel.getTypes().add(tt);
        }

        if (log != null) log.info("TypeScriptGenerator: erzeugte Typen: " + tsModel.getTypes().size());
        return tsModel;
    }

    private String resolveTsType(JavaFieldModel f) {
        // Vorrang: expliziter TS-Typ aus Annotation
        if (f.getTsTypeOverride() != null && !f.getTsTypeOverride().isBlank()) {
            return f.getTsTypeOverride();
        }
        String raw = Objects.toString(f.getJavaType(), "");
        if (raw.isBlank()) return "any";

        // Normalisieren (ohne Whitespaces)
        String s = raw.replace("\n"," ").replaceAll("\\s+", " ").trim();

        // Arrays: X[] → X[] (Type extrahieren)
        if (s.endsWith("[]")) {
            String base = s.substring(0, s.length() - 2).trim();
            return mapSimple(base) + "[]";
        }

        // Generics behandeln: z.B. List<Foo>, Set<Bar>, Optional<T>, Map<K,V>
        int lt = s.indexOf('<');
        int gt = s.lastIndexOf('>');
        if (lt > 0 && gt > lt) {
            String rawType = s.substring(0, lt).trim();
            String generics = s.substring(lt + 1, gt).trim();
            String[] parts = splitTopLevel(generics);

            String rt = rawType;
            String rtLow = rt.toLowerCase(Locale.ROOT);

            if (rtLow.endsWith("list") || rtLow.endsWith("set") || rtLow.endsWith("collection")) {
                String elem = parts.length > 0 ? parts[0].trim() : "any";
                return mapSimple(elem) + "[]";
            }
            if (rtLow.endsWith("optional")) {
                String elem = parts.length > 0 ? parts[0].trim() : "any";
                // optionales Feld ist bereits durch f.isOptional() modelliert;
                // in TS-Typ bleibt nur der Elementtyp
                return mapSimple(elem);
            }
            if (rtLow.endsWith("map")) {
                // Map<K,V> → Record<string, V>
                String v = parts.length > 1 ? parts[1].trim() : "any";
                return "Record<string, " + mapSimple(v) + ">";
            }
            // Default: generics entfernen und Simple‑Typ mappen
            String main = rawType.trim();
            return mapSimple(main);
        }

        // Kein Generic/Array, einfacher Typ
        return mapSimple(s);
    }

    private String mapSimple(String javaTypeName) {
        if (javaTypeName == null || javaTypeName.isBlank()) return "any";
        String t = javaTypeName.trim();

        // Entferne vollqualifizierte Namen → SimpleName
        int lastDot = t.lastIndexOf('.');
        if (lastDot >= 0) t = t.substring(lastDot + 1);

        // Entferne generische Reste (Sicherheitsnetz)
        int lt = t.indexOf('<');
        if (lt >= 0) t = t.substring(0, lt).trim();

        switch (t) {
            case "byte":
            case "short":
            case "int":
            case "long":
            case "float":
            case "double":
            case "Byte":
            case "Short":
            case "Integer":
            case "Long":
            case "Float":
            case "Double":
                return "number";
            case "boolean":
            case "Boolean":
                return "boolean";
            case "char":
            case "Character":
            case "String":
                return "string";
            case "Object":
                return "any";
            default:
                // Unbekannter/benutzerdefinierter Typ → identisch übernehmen (wird als eigener TS‑Typ generiert)
                return t;
        }
    }

    private static String[] splitTopLevel(String s) {
        // Teilt Generic-Argumente auf oberster Ebene, z. B. "Map<String, List<Foo>>" → ["String", "List<Foo>"]
        List<String> parts = new ArrayList<>();
        int depth = 0;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '<') { depth++; current.append(c); continue; }
            if (c == '>') { depth--; current.append(c); continue; }
            if (c == ',' && depth == 0) {
                parts.add(current.toString().trim());
                current.setLength(0);
                continue;
            }
            current.append(c);
        }
        if (current.length() > 0) parts.add(current.toString().trim());
        return parts.toArray(new String[0]);
    }
}
