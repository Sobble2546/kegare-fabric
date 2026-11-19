package com.cnethuka.kegare.loadout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cnethuka.kegare.fists.FistsIds;
import com.cnethuka.kegare.api.ability.Ability;
import com.cnethuka.kegare.api.technique.InnateTechnique;
import com.cnethuka.kegare.registry.KegareRegistries;
import com.cnethuka.kegare.rmt.RmtIds;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import com.cnethuka.kegare.test.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.cnethuka.kegare.api.ability.AbilityCategory;

/** The ability categories (DESIGN §3.16): Innate, Malice and Melee, every ability in exactly one, the right ones under each tab. */
@ExtendWith(EphemeralTestServerProvider.class)
class AbilityCategoriesTest {
    @Test
    void theTabsAndTheirOrder(MinecraftServer server) {
        assertEquals(List.of(AbilityCategory.INNATE, AbilityCategory.MALICE, AbilityCategory.MELEE), AbilityTabs.tabsFor(false));
        assertTrue(AbilityTabs.tabsFor(true).isEmpty(), "a Severed player sees no tabs");
    }

    @Test
    void everyAbilityIsInExactlyOne(MinecraftServer server) {
        for (InnateTechnique technique : KegareRegistries.INNATE_TECHNIQUE) {
            for (Ability a : technique.everyAbility()) assertEquals(AbilityCategory.INNATE, a.category(), a.id() + ": its technique's");
            assertEquals(technique.abilities(), AbilityTabs.abilities(AbilityCategory.INNATE, technique), "the Innate tab is the technique's own");
        }
        for (Ability a : SharedAbilities.all()) {
            assertTrue(a.category() == AbilityCategory.MALICE || a.category() == AbilityCategory.MELEE, a.id() + ": a shared one is Malice or Melee");
            long under = AbilityTabs.tabsFor(false).stream().filter(c -> AbilityTabs.abilities(c, null).contains(a)).count();
            assertEquals(a.category() == AbilityCategory.INNATE ? 0 : 1, under, a.id() + " is under exactly one tab");
            assertEquals(a.category(), AbilityLookup.category(a.id()));
        }
    }

    @Test
    void theRightAbilitiesUnderEachTab(MinecraftServer server) {
        Set<Identifier> malice = AbilityTabs.abilities(AbilityCategory.MALICE, null).stream().map(Ability::id).collect(Collectors.toSet());
        Set<Identifier> melee = AbilityTabs.abilities(AbilityCategory.MELEE, null).stream().map(Ability::id).collect(Collectors.toSet());
        assertTrue(malice.contains(RmtIds.ABILITY), "Reverse Malice Technique is Malice");
        assertTrue(melee.contains(FistsIds.ABILITY), "Reinforced Fists is Melee");
        assertTrue(java.util.Collections.disjoint(malice, melee));
    }
}
