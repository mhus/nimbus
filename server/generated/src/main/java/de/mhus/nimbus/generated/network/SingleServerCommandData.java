package de.mhus.nimbus.generated.network;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generated from ServerCommandMessage.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SingleServerCommandData {

    /**
     * cmd
     */
    private String cmd;

    /**
     * args (optional)
     */
    private java.util.List<String> args;
}
