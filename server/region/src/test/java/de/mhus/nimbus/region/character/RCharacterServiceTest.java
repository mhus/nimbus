package de.mhus.nimbus.region.character;

import de.mhus.nimbus.shared.dto.region.RegionItemInfo; // neuer Import
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RCharacterServiceTest {

    @Test
    void simplePojoTest() {
        var item = new RegionItemInfo("sword01","Sword","tex_sword");
        var c = new RCharacter("user1","regionA","hero","Hero Display");
        c.putBackpackItem("slot1", item);
        c.putWearingItem(1, item);
        c.setSkill("strength",10);
        c.incrementSkill("strength",5);
        assertEquals(15, c.getSkills().get("strength"));
        assertEquals("regionA", c.getRegionId());
        assertEquals("sword01", c.getBackpack().get("slot1").getItemId());
        assertTrue(c.getWearing().containsKey(1));
    }
}
