package de.mhus.nimbus.shared.dto.command;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetWorldCommandData {
    private String worldId;
}
