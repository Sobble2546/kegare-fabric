package com.cnethuka.kegare.platform.damage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cnethuka.kegare.platform.damage.DamageContainer.Reduction;
import org.junit.jupiter.api.Test;

class DamageContainerTest {
    @Test
    void startsAtTheOriginalAmountWithANormalCooldown() {
        DamageContainer c = new DamageContainer(null, 8f);
        assertEquals(8f, c.getOriginalDamage());
        assertEquals(8f, c.getNewDamage());
        assertEquals(20, c.getPostAttackInvulnerabilityTicks());
        assertTrue(Float.isNaN(c.getAmountAtActuallyHurt()));
        assertEquals(0f, c.getReduction(Reduction.ARMOR));
    }

    @Test
    void defencesAreReportedAsWhatEnteredActuallyHurtMinusWhatIsLeft() {
        DamageContainer c = new DamageContainer(null, 10f);
        c.enteredActuallyHurt(10f);
        c.defencesDone(6f);
        float beforeDefences = 6f + c.getReduction(Reduction.ARMOR) + c.getReduction(Reduction.ENCHANTMENTS) + c.getReduction(Reduction.MOB_EFFECTS);
        assertEquals(10f, beforeDefences, 1e-6, "MeleeHits.beforeDefences: new damage plus the three reductions is what entered actuallyHurt");
    }

    @Test
    void aSecondActuallyHurtStartsFromScratch() {
        DamageContainer c = new DamageContainer(null, 10f);
        c.enteredActuallyHurt(10f);
        c.defencesDone(4f);
        c.enteredActuallyHurt(3f);
        c.defencesDone(3f);
        assertEquals(0f, c.getReduction(Reduction.ARMOR));
        assertEquals(3f, c.getAmountAtActuallyHurt());
    }

    @Test
    void defencesCanNotAddDamage() {
        DamageContainer c = new DamageContainer(null, 5f);
        c.enteredActuallyHurt(5f);
        c.defencesDone(7f);
        assertEquals(0f, c.getReduction(Reduction.ARMOR));
    }
}
