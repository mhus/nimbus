package de.mhus.nimbus.shared.service;

import lombok.Getter;
import org.apache.logging.log4j.util.Strings;

@Getter
public class SchemaVersion implements Comparable<SchemaVersion> {

    public static final SchemaVersion NULL = SchemaVersion.of("0");

    private int major = 0;
    private int minor = 0;
    private int patch = 0;

    public SchemaVersion(String version) {
        if (Strings.isNotEmpty(version)) {
            String[] parts = version.trim().split("\\.");
            this.major = parts.length > 0 ? Integer.parseInt(parts[0]) : 0;
            this.minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            this.patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
        }
    }

    public boolean isNull() {
        return major == 0 && minor == 0 && patch == 0;
    }

    public boolean equals(Object other) {
        if (other == null) return false;
        if (other instanceof SchemaVersion schemaVersion) {
            return this.major == schemaVersion.major &&
                    this.minor == schemaVersion.minor &&
                    this.patch == schemaVersion.patch;
        }
        var otherVersion = new SchemaVersion(other.toString());
        return this.major == otherVersion.major &&
                this.minor == otherVersion.minor &&
                this.patch == otherVersion.patch;
    }

    public static SchemaVersion of(String version) {
        return new SchemaVersion(version);
    }

    @Override
    public int compareTo(SchemaVersion o) {
        if (this.major != o.major) {
            return Integer.compare(this.major, o.major);
        }
        if (this.minor != o.minor) {
            return Integer.compare(this.minor, o.minor);
        }
        return Integer.compare(this.patch, o.patch);
    }

    public String toString() {
        return major + "." + minor + "." + patch;
    }

}
