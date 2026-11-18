package com.cnethuka.kegare.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** A mob's goal selector (protected in vanilla; NeoForge's access transformer opened it): Six Eyes reads which attack goals are running. Read only. */
@Mixin(Mob.class)
public interface MobGoalsAccessor {
    @Accessor("goalSelector")
    GoalSelector kegare$goalSelector();
}
