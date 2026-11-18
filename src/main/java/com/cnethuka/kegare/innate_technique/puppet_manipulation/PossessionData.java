package com.cnethuka.kegare.innate_technique.puppet_manipulation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.cnethuka.kegare.platform.attachment.IAttachmentHolder;
import com.cnethuka.kegare.platform.attachment.IAttachmentSerializer;

/**
 * On a player while they possess a puppet ({@code kegare:possession}, saved): which puppet and shell, and everything
 * returning restores (health, where the body stands, the stashed inventory). Saved so that if the server stops
 * mid-possession, the next login puts it all back ({@link Possession#restoreOnLogin}).
 */
public final class PossessionData {
    @Nullable
    UUID puppet;
    @Nullable
    UUID shell;
    float realHealth;
    float realMaxHealth = 20f;
    Vec3 bodyPos = Vec3.ZERO;
    float bodyYaw;
    float bodyPitch;
    @Nullable
    ResourceKey<Level> dimension;
    /** Everything the body carried (hotbar, backpack, armor, offhand), by container slot; the player holds the puppet's empty hands meanwhile. */
    List<ItemStackWithSlot> inventory = new ArrayList<>();
    int selectedSlot;
    PuppetTier tier = PuppetTier.TAP;

    public boolean active() {
        return puppet != null;
    }

    @Nullable
    public UUID puppet() {
        return puppet;
    }

    @Nullable
    public UUID shell() {
        return shell;
    }

    public PuppetTier tier() {
        return tier;
    }

    void clear() {
        puppet = null;
        shell = null;
        inventory = new ArrayList<>();
        dimension = null;
    }

    public static final IAttachmentSerializer<CompoundTag, PossessionData> SERIALIZER = new IAttachmentSerializer<>() {
        @Override
        public PossessionData read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
            PossessionData d = new PossessionData();
            Optional<UUID> puppet = tag.read("Puppet", UUIDUtil.CODEC);
            if (puppet.isEmpty()) return d;
            d.puppet = puppet.get();
            d.shell = tag.read("Shell", UUIDUtil.CODEC).orElse(null);
            d.realHealth = tag.getFloatOr("RealHealth", 0f);
            d.realMaxHealth = tag.getFloatOr("RealMaxHealth", 20f);
            d.bodyPos = new Vec3(tag.getDoubleOr("X", 0), tag.getDoubleOr("Y", 0), tag.getDoubleOr("Z", 0));
            d.bodyYaw = tag.getFloatOr("Yaw", 0f);
            d.bodyPitch = tag.getFloatOr("Pitch", 0f);
            Identifier dim = Identifier.tryParse(tag.getStringOr("Dimension", ""));
            d.dimension = dim == null ? null : ResourceKey.create(Registries.DIMENSION, dim);
            d.inventory = new ArrayList<>(tag.read("Inventory", ItemStackWithSlot.CODEC.listOf(), provider.createSerializationContext(NbtOps.INSTANCE))
                    .orElse(List.of()));
            d.selectedSlot = tag.getIntOr("Selected", 0);
            d.tier = PuppetTier.byId(tag.getIntOr("Tier", 0));
            return d;
        }

        @Override
        @Nullable
        public CompoundTag write(PossessionData d, HolderLookup.Provider provider) {
            if (!d.active()) return null;
            CompoundTag tag = new CompoundTag();
            tag.store("Puppet", UUIDUtil.CODEC, d.puppet);
            if (d.shell != null) tag.store("Shell", UUIDUtil.CODEC, d.shell);
            tag.putFloat("RealHealth", d.realHealth);
            tag.putFloat("RealMaxHealth", d.realMaxHealth);
            tag.putDouble("X", d.bodyPos.x);
            tag.putDouble("Y", d.bodyPos.y);
            tag.putDouble("Z", d.bodyPos.z);
            tag.putFloat("Yaw", d.bodyYaw);
            tag.putFloat("Pitch", d.bodyPitch);
            if (d.dimension != null) tag.putString("Dimension", d.dimension.identifier().toString());
            tag.store("Inventory", ItemStackWithSlot.CODEC.listOf(), provider.createSerializationContext(NbtOps.INSTANCE), d.inventory);
            tag.putInt("Selected", d.selectedSlot);
            tag.putInt("Tier", d.tier.ordinal());
            return tag;
        }
    };
}
