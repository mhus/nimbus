package de.mhus.nimbus.shared.types;

import lombok.Getter;
import org.apache.logging.log4j.util.Strings;

import java.util.Optional;

/**
 * PlayerId represents a unique identifier for a player in the format "userId:charachterId".
 * Each part is a string 'a-zA-Z0-9_-' from 3 to 64 characters.
 */
public class PlayerId {
    @Getter
    private String id;

    public PlayerId(String id) {
        this.id = id;
    }

    public static Optional<PlayerId> of(String id) {
        if (!validate(id)) return Optional.empty();
        return Optional.of(new PlayerId(id));
    }

    public static boolean validate(String id) {
        if (Strings.isBlank(id)) return false;
        if (id.length() < 3) return false;
        return id.matches("[a-zA-Z0-9_\\-]{3,64}:[a-zA-Z0-9_\\-]{3,64}");
    }
}
