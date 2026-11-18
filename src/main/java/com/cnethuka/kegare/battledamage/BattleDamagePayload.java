package com.cnethuka.kegare.battledamage;

import com.cnethuka.kegare.Kegare;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import com.cnethuka.kegare.platform.net.IPayloadContext;
import com.cnethuka.kegare.platform.net.WireIds;

/**
 * Server to the players near a hit that hit the world hard enough (DESIGN §3.24): where, how hard, and the blocks it broke (or, in
 * COSMETIC mode, would have) with what each was, so the clients can throw their debris and crack what's left. The blocks themselves
 * change through the game's own block updates. Cosmetic: the server has already decided everything.
 *
 * @param power  the hit's power at its centre (as against blast resistance), for how far the cracks reach
 * @param states each block's local state id ({@code Block.getId}), in the same order as {@code blocks}. On the wire a block is its name
 *               (a palette of the distinct blocks, then an index per block), since state ids differ between Minecraft versions; the
 *               reader gets each block's default state in its own numbering.
 */
public record BattleDamagePayload(Vec3 contact, float radius, float power, boolean cosmetic, List<BlockPos> blocks, int[] states)
        implements CustomPacketPayload {
    public static final Type<BattleDamagePayload> TYPE = new Type<>(Kegare.id("battle_damage"));
    public static final StreamCodec<FriendlyByteBuf, BattleDamagePayload> STREAM_CODEC =
            StreamCodec.ofMember(BattleDamagePayload::write, BattleDamagePayload::read);

    /** Set by the client entry point; null on a dedicated server. */
    private static Consumer<BattleDamagePayload> clientHandler;

    public static void setClientHandler(Consumer<BattleDamagePayload> handler) {
        clientHandler = handler;
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeDouble(contact.x);
        buf.writeDouble(contact.y);
        buf.writeDouble(contact.z);
        buf.writeFloat(radius);
        buf.writeFloat(power);
        buf.writeBoolean(cosmetic);
        List<Identifier> palette = new ArrayList<>();
        int[] index = new int[blocks.size()];
        for (int i = 0; i < blocks.size(); i++) {
            Identifier block = WireIds.blockName(states[i]);
            int at = palette.indexOf(block);
            if (at < 0) {
                at = palette.size();
                palette.add(block);
            }
            index[i] = at;
        }
        buf.writeVarInt(palette.size());
        for (Identifier block : palette) buf.writeIdentifier(block);
        buf.writeVarInt(blocks.size());
        for (int i = 0; i < blocks.size(); i++) {
            buf.writeBlockPos(blocks.get(i));
            buf.writeVarInt(index[i]);
        }
    }

    private static BattleDamagePayload read(FriendlyByteBuf buf) {
        Vec3 contact = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        float radius = buf.readFloat(), power = buf.readFloat();
        boolean cosmetic = buf.readBoolean();
        int paletteSize = Math.min(buf.readVarInt(), 4096);
        int[] palette = new int[paletteSize];
        for (int i = 0; i < paletteSize; i++) palette[i] = WireIds.defaultStateId(buf.readIdentifier());
        int n = Math.min(buf.readVarInt(), 4096);
        List<BlockPos> blocks = new ArrayList<>(n);
        int[] states = new int[n];
        for (int i = 0; i < n; i++) {
            blocks.add(buf.readBlockPos());
            int at = buf.readVarInt();
            if (at < 0 || at >= paletteSize) throw new io.netty.handler.codec.DecoderException("Block index " + at + " is outside the palette of " + paletteSize);
            states[i] = palette[at];
        }
        return new BattleDamagePayload(contact, radius, power, cosmetic, blocks, states);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BattleDamagePayload payload, IPayloadContext context) {
        if (clientHandler != null) clientHandler.accept(payload);
    }
}
