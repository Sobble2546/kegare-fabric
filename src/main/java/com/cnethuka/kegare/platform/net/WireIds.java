package com.cnethuka.kegare.platform.net;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Raw registry ids never go on the wire (DESIGN §3.5): they differ between Minecraft versions, and across a version bridge they arrive
 * untranslated. A payload's in-memory record keeps the local raw id (its consumers look it up in their own registry); the codec writes
 * the entry's name and turns it back into the reader's own id.
 */
public final class WireIds {
    private WireIds() {}

    /** The name of the entity type with this local raw id (the default type's, pig, for an id nothing has). */
    public static Identifier entityTypeName(int localId) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(BuiltInRegistries.ENTITY_TYPE.byId(localId));
    }

    /** The local raw id of the entity type with this name (the default type's, pig, if this game has no such type). */
    public static int entityTypeId(Identifier name) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(name);
        return BuiltInRegistries.ENTITY_TYPE.getId(type);
    }

    /** The name of the block a block state id (Block.getId) belongs to. */
    public static Identifier blockName(int localStateId) {
        return BuiltInRegistries.BLOCK.getKey(Block.stateById(localStateId).getBlock());
    }

    /** The local state id of a block's default state (air for a block this game does not have). */
    public static int defaultStateId(Identifier block) {
        BlockState state = BuiltInRegistries.BLOCK.getValue(block).defaultBlockState();
        return Block.getId(state);
    }
}
