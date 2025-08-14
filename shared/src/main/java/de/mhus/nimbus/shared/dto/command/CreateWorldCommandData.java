package de.mhus.nimbus.shared.dto.command;

import de.mhus.nimbus.shared.dto.world.WorldDto;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateWorldCommandData {
    private WorldDto world;
}
