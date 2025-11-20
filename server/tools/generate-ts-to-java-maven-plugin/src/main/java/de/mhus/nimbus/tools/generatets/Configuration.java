package de.mhus.nimbus.tools.generatets;

import java.util.List;

public class Configuration {

    public List<String> ignoreTsItems;
    public String basePackage;

    /**
     * Optional package mapping rules. If a TS source directory (relative to a configured sourceDir)
     * ends with {@code dirEndsWith}, all types from that directory will be generated into {@code pkg}.
     */
    public List<PackageRule> packageRules;

    public static class PackageRule {
        /** Suffix of the TS source directory to match (e.g. "types"). */
        public String dirEndsWith;
        /** Target Java package to use for matched sources. */
        public String pkg;
    }
}
