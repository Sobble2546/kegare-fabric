package com.cnethuka.kegare.platform.net;

import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.IdMap;
import net.minecraft.core.Registry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * The two codec wrappers the {@code ByteBufCodecsMixin} puts around vanilla's registry codecs (DESIGN §3.5, "Via servers").
 * <ul>
 *   <li>{@link #registry}: entity types and items travel as raw ids. While {@link IdRemap} has a table (the server numbers Kegare's entries
 *       differently), a decoded id is the server's and an encoded one the server's too; otherwise the vanilla codec runs untouched.</li>
 *   <li>{@link #holder}: the server writes Kegare's sound events as a direct holder (the name on the wire), never as a registry id.
 *       Through ViaVersion an id-based sound packet is silently dropped; a direct one plays.</li>
 * </ul>
 */
public final class IdRemapCodecs {
    private static final Identifier ENTITY_TYPE = Identifier.fromNamespaceAndPath("minecraft", "entity_type");
    private static final Identifier ITEM = Identifier.fromNamespaceAndPath("minecraft", "item");
    private static final Identifier SOUND_EVENT = Identifier.fromNamespaceAndPath("minecraft", "sound_event");
    private static final Set<Identifier> REMAPPED = Set.of(ENTITY_TYPE, ITEM);

    private IdRemapCodecs() {}

    /** Wraps {@code ByteBufCodecs.registry(key, extractor)}'s result if it is for entity types or items; otherwise returns it as it is. */
    public static <T, R> StreamCodec<RegistryFriendlyByteBuf, R> registry(ResourceKey<? extends Registry<T>> key,
                                                                         Function<Registry<T>, IdMap<R>> extractor,
                                                                         StreamCodec<RegistryFriendlyByteBuf, R> original) {
        Identifier id = key.identifier();
        if (!REMAPPED.contains(id)) return original;
        IdRemap.noteHooked();
        return new StreamCodec<>() {
            private IdMap<R> map(RegistryFriendlyByteBuf buf) {
                return extractor.apply(buf.registryAccess().lookupOrThrow(key));
            }

            @Override
            public R decode(RegistryFriendlyByteBuf buf) {
                if (!IdRemap.isActive()) return original.decode(buf);
                return map(buf).byIdOrThrow(IdRemap.toLocal(id, buf.readVarInt()));
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, R value) {
                if (!IdRemap.isActive()) {
                    original.encode(buf, value);
                    return;
                }
                buf.writeVarInt(IdRemap.toWire(id, map(buf).getIdOrThrow(value)));
            }
        };
    }

    /** Wraps {@code ByteBufCodecs.holder(key, direct)}'s result if it is for sound events; otherwise returns it as it is. */
    public static <T> StreamCodec<RegistryFriendlyByteBuf, Holder<T>> holder(ResourceKey<? extends Registry<T>> key,
                                                                            StreamCodec<? super RegistryFriendlyByteBuf, T> direct,
                                                                            StreamCodec<RegistryFriendlyByteBuf, Holder<T>> original) {
        if (!SOUND_EVENT.equals(key.identifier())) return original;
        return new StreamCodec<>() {
            @Override
            public Holder<T> decode(RegistryFriendlyByteBuf buf) {
                return original.decode(buf);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, Holder<T> holder) {
                if (isKegare(holder)) {
                    buf.writeVarInt(0); // vanilla's marker for "the value follows"
                    direct.encode(buf, holder.value());
                } else {
                    original.encode(buf, holder);
                }
            }
        };
    }

    private static boolean isKegare(Holder<?> holder) {
        return holder.unwrapKey().map(k -> HelloTable.NAMESPACE.equals(k.identifier().getNamespace())).orElse(false);
    }
}
