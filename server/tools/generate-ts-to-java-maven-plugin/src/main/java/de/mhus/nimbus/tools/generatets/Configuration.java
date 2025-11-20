package de.mhus.nimbus.tools.generatets;

import java.util.List;
import java.util.Map;

public class Configuration {

    public List<String> ignoreTsItems;
    public String basePackage;

    /**
     * Optional: exclude entire TS subdirectories by suffix. Any source file whose
     * relative directory (to its configured sourceDir) ends with one of these suffixes
     * will be ignored completely. Example entries: "errors", "logger", "network/messages".
     */
    public List<String> excludeDirSuffixes;

    /**
     * Optional package mapping rules. If a TS source directory (relative to a configured sourceDir)
     * ends with {@code dirEndsWith}, all types from that directory will be generated into {@code pkg}.
     */
    public List<PackageRule> packageRules;

    /**
     * Optional mapping from simple type names (e.g. ClientType) to fully-qualified Java types
     * (e.g. de.mhus.nimbus.types.ClientType). When provided, the generator will rewrite
     * occurrences of these simple names in property types and references (extends/implements/alias)
     * to the configured fully-qualified names. Generic arguments are also processed.
     */
    public Map<String, String> typeMappings;

    public static class PackageRule {
        /** Suffix of the TS source directory to match (e.g. "types"). */
        public String dirEndsWith;
        /** Target Java package to use for matched sources. */
        public String pkg;
    }
}
