package de.mhus.nimbus.shared.types;

import de.mhus.nimbus.generated.configs.PlayerBackpack;
import de.mhus.nimbus.generated.types.PlayerInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerCharacter {

    private PlayerInfo publicData;
    private PlayerBackpack backpack;

}
