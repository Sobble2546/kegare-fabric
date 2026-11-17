package com.cnethuka.kegare.innatetechnique.change;

import com.cnethuka.kegare.api.ability.Ability;
import com.cnethuka.kegare.api.ability.AbilityCategory;
import com.cnethuka.kegare.api.ability.AbilityContext;
import com.cnethuka.kegare.api.ability.CastType;
import com.cnethuka.kegare.api.ability.EndReason;
import com.cnethuka.kegare.api.ability.Requirement;
import com.cnethuka.kegare.api.change.AbilityTweak;
import com.cnethuka.kegare.api.visual.AbilityVisuals;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * An ability as players get it once addons changed it (DESIGN §3.34): the ability itself (or its replacement), with every tweak
 * registered against its ID applied in order, and its special as players get that. Everything else is the ability's own. Same ID,
 * cast type and cooldown key, so cooldowns, loadouts and saved data don't notice.
 */
final class TweakedAbility implements Ability {
    private final Ability ability;
    private final List<AbilityTweak> tweaks;
    @Nullable
    private final Ability special;

    TweakedAbility(Ability ability, List<AbilityTweak> tweaks, @Nullable Ability special) {
        this.ability = ability;
        this.tweaks = List.copyOf(tweaks);
        this.special = special;
    }

    /** The ability underneath (the original or its replacement). */
    Ability ability() {
        return ability;
    }

    @Override public ResourceLocation id() { return ability.id(); }
    @Override public CastType castType() { return ability.castType(); }

    @Override
    public double cost(ServerPlayer player) {
        double cost = ability.cost(player);
        for (AbilityTweak t : tweaks) cost = t.cost(player, cost);
        return cost;
    }

    @Override
    public double shownCost(ServerPlayer player) {
        double cost = ability.shownCost(player);
        for (AbilityTweak t : tweaks) cost = t.cost(player, cost);
        return cost;
    }

    @Override
    public double releaseCost(ServerPlayer player, float charge) {
        double cost = ability.releaseCost(player, charge);
        for (AbilityTweak t : tweaks) cost = t.cost(player, cost);
        return cost;
    }

    @Override
    public int cooldownTicks(ServerPlayer player) {
        int ticks = ability.cooldownTicks(player);
        for (AbilityTweak t : tweaks) ticks = t.cooldownTicks(player, ticks);
        return ticks;
    }

    @Override
    public int shownCooldownTicks(ServerPlayer player) {
        int ticks = ability.shownCooldownTicks(player);
        for (AbilityTweak t : tweaks) ticks = t.cooldownTicks(player, ticks);
        return ticks;
    }

    @Override
    public double upkeepPerSecond(ServerPlayer player) {
        double upkeep = ability.upkeepPerSecond(player);
        for (AbilityTweak t : tweaks) upkeep = t.upkeepPerSecond(player, upkeep);
        return upkeep;
    }

    @Override
    public int minChargeTicks(ServerPlayer player) {
        int ticks = ability.minChargeTicks(player);
        for (AbilityTweak t : tweaks) ticks = t.minChargeTicks(player, ticks);
        return ticks;
    }

    @Override
    public int maxChargeTicks(ServerPlayer player) {
        int ticks = ability.maxChargeTicks(player);
        for (AbilityTweak t : tweaks) ticks = t.maxChargeTicks(player, ticks);
        return ticks;
    }

    @Override
    public int maxChannelTicks(ServerPlayer player) {
        int ticks = ability.maxChannelTicks(player);
        for (AbilityTweak t : tweaks) ticks = t.maxChannelTicks(player, ticks);
        return ticks;
    }

    @Override
    public boolean canCast(AbilityContext ctx) {
        for (AbilityTweak t : tweaks) if (!t.canCast(ctx)) return false;
        return ability.canCast(ctx);
    }

    @Override public boolean canRelease(AbilityContext ctx) { return ability.canRelease(ctx); }
    @Override public Component blockedMessage(ServerPlayer player) { return ability.blockedMessage(player); }

    @Override
    public void execute(AbilityContext ctx) {
        for (AbilityTweak t : tweaks) t.beforeExecute(ctx);
        ability.execute(ctx);
        for (AbilityTweak t : tweaks) t.afterExecute(ctx);
    }

    @Override
    public void tick(AbilityContext ctx) {
        ability.tick(ctx);
        for (AbilityTweak t : tweaks) t.tick(ctx);
    }

    @Override
    public void onEnd(AbilityContext ctx, EndReason reason) {
        ability.onEnd(ctx, reason);
        for (AbilityTweak t : tweaks) t.onEnd(ctx, reason);
    }

    @Override public Optional<Ability> special() { return Optional.ofNullable(special); }
    @Override public Optional<Ability> base() { return ability.base(); }
    @Override public boolean isSpecial() { return ability.isSpecial(); }
    @Override public ResourceLocation cooldownKey() { return ability.cooldownKey(); }
    @Override public List<Requirement> requirements() { return ability.requirements(); }
    @Override public boolean hasNoTarget() { return ability.hasNoTarget(); }
    @Override public AbilityVisuals visuals() { return ability.visuals(); }
    @Override public String descriptionId() { return ability.descriptionId(); }
    @Override public AbilityCategory category() { return ability.category(); }
    @Override public Component unlockHint() { return ability.unlockHint(); }
    @Override public Component displayName() { return ability.displayName(); }
    @Override public Object[] descriptionArgs() { return ability.descriptionArgs(); }
    @Override public String toString() { return ability.toString(); }
}
