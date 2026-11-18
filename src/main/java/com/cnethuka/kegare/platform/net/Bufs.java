package com.cnethuka.kegare.platform.net;

import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Length-prefixed lists and maps on a buffer. {@code FriendlyByteBuf} had these as methods up to 1.21.11 and lost them in 26.x, so the
 * payload codecs call these on every target. Same bytes as the old methods: a VarInt count, then each element.
 */
public final class Bufs {
    private Bufs() {}

    public static <B extends FriendlyByteBuf, T> void writeCollection(B buf, Collection<T> values, BiConsumer<? super B, T> writer) {
        buf.writeVarInt(values.size());
        for (T value : values) writer.accept(buf, value);
    }

    public static <B extends FriendlyByteBuf, T> List<T> readList(B buf, Function<? super B, T> reader) {
        int size = buf.readVarInt();
        List<T> out = new ArrayList<>(Math.min(size, 1024));
        for (int i = 0; i < size; i++) out.add(reader.apply(buf));
        return out;
    }

    public static <B extends FriendlyByteBuf, K, V> void writeMap(B buf, Map<K, V> map, BiConsumer<? super B, K> keys, BiConsumer<? super B, V> values) {
        buf.writeVarInt(map.size());
        map.forEach((k, v) -> {
            keys.accept(buf, k);
            values.accept(buf, v);
        });
    }

    public static <B extends FriendlyByteBuf, K, V> Map<K, V> readMap(B buf, Function<? super B, K> keys, Function<? super B, V> values) {
        int size = buf.readVarInt();
        Map<K, V> out = new HashMap<>(Math.min(size, 1024));
        for (int i = 0; i < size; i++) {
            K key = keys.apply(buf);
            out.put(key, values.apply(buf));
        }
        return out;
    }
}
