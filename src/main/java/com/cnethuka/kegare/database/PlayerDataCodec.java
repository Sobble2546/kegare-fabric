package com.cnethuka.kegare.database;

import com.cnethuka.kegare.Kegare;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import com.cnethuka.kegare.platform.attachment.AttachmentHolder;
import com.cnethuka.kegare.platform.attachment.AttachmentType;
import com.cnethuka.kegare.platform.registry.NeoForgeRegistries;

/**
 * Kegare's per-player data as JSON, one document per attachment (DESIGN §3.33). Driven from the attachment registry: any Kegare attachment that is
 * saved is stored, with no new code. Encoding runs the attachment's codec to NBT and turns that into JSON; decoding builds the NBT again,
 * keeping whole numbers whole (a long past 2^53 would not survive being a double), and hands it to the same codec. The shape is the one in a
 * player's file under {@code fabric:attachments} (and {@code neoforge:attachments} in a NeoForge world), so rows are the same on either loader.
 */
public final class PlayerDataCodec {
    /** A row couldn't be decoded. Whoever gets this keeps the player out: a fresh roll over real data is the worst failure there is. */
    public static final class DecodeException extends Exception {
        DecodeException(String attachment, Throwable cause) {
            super("Couldn't decode " + attachment, cause);
        }
    }

    private PlayerDataCodec() {}

    /** Every attachment that's stored: Kegare's own, with a serializer. */
    public static List<Identifier> savedAttachments() {
        List<Identifier> out = new ArrayList<>();
        for (Map.Entry<net.minecraft.resources.ResourceKey<AttachmentType<?>>, AttachmentType<?>> entry : NeoForgeRegistries.ATTACHMENT_TYPES.entrySet()) {
            Identifier id = entry.getKey().identifier();
            if (ours(id.toString()) && entry.getValue().isSaved()) out.add(id);
        }
        return out;
    }

    /** Is this attachment key Kegare's? (Curios and other mods keep theirs in the player's file, with their items.) */
    static boolean ours(String key) {
        return key.startsWith(Kegare.MODID + ":");
    }

    /** What this player's saved attachments encode to now: attachment id to JSON. Server thread. */
    @SuppressWarnings("unchecked")
    public static Map<String, String> snapshot(ServerPlayer player, HolderLookup.Provider registries) {
        Map<String, String> out = new LinkedHashMap<>();
        for (Identifier id : savedAttachments()) {
            AttachmentType<Object> type = (AttachmentType<Object>) NeoForgeRegistries.ATTACHMENT_TYPES.get(id);
            if (!player.hasData(type)) continue;
            Tag tag = type.encode(player.getData(type), registries);
            if (tag != null) out.put(id.toString(), toJson(tag));
        }
        return out;
    }

    /** Kegare's attachments out of a player file's own tag, as JSON (what the file holds from before storage was switched on). */
    public static Map<String, String> fromPlayerFile(CompoundTag file) {
        Map<String, String> out = new LinkedHashMap<>();
        // A file the NeoForge build wrote has its attachments under the other key; the fabric one wins where both have an entry.
        for (String fileKey : new String[] {AttachmentHolder.NEOFORGE_NBT_KEY, AttachmentHolder.ATTACHMENTS_NBT_KEY}) {
            CompoundTag all = file.getCompoundOrEmpty(fileKey);
            for (String key : all.keySet()) {
                if (ours(key)) out.put(key, toJson(all.get(key)));
            }
        }
        return out;
    }

    /** Puts rows on a player, replacing all their stored attachments. Decodes everything first, so a failure changes nothing. */
    @SuppressWarnings("unchecked")
    public static void apply(ServerPlayer player, Map<String, String> rows, HolderLookup.Provider registries) throws DecodeException {
        Map<AttachmentType<?>, Object> decoded = new HashMap<>();
        for (Map.Entry<String, String> row : rows.entrySet()) {
            Identifier id = Identifier.tryParse(row.getKey());
            AttachmentType<?> type = id == null ? null : NeoForgeRegistries.ATTACHMENT_TYPES.get(id);
            if (type == null || !type.isSaved()) {
                Kegare.LOGGER.warn("Kegare's database has data for {}, which this version doesn't know. Leaving it there.", row.getKey());
                continue;
            }
            try {
                Tag tag = fromJson(JsonParser.parseString(row.getValue()));
                decoded.put(type, type.decode(tag, registries));
            } catch (RuntimeException e) {
                throw new DecodeException(row.getKey(), e);
            }
        }
        for (Identifier id : savedAttachments()) {
            AttachmentType<Object> type = (AttachmentType<Object>) NeoForgeRegistries.ATTACHMENT_TYPES.get(id);
            if (player.hasData(type)) player.removeData(type);
        }
        decoded.forEach((type, value) -> player.setData((AttachmentType<Object>) type, value));
    }

    /** Puts rows into a player's file, in place of whatever of Kegare's it has: the way back from the database. */
    public static void toPlayerFile(CompoundTag file, Map<String, String> rows) {
        stripFromPlayerFile(file);
        if (rows.isEmpty()) return;
        CompoundTag all = file.getCompoundOrEmpty(AttachmentHolder.ATTACHMENTS_NBT_KEY);
        rows.forEach((attachment, json) -> all.put(attachment, fromJson(JsonParser.parseString(json))));
        file.put(AttachmentHolder.ATTACHMENTS_NBT_KEY, all);
    }

    /** Takes Kegare's attachments out of a tag about to be written as a player's file. Returns what it took. */
    public static int stripFromPlayerFile(CompoundTag file) {
        int removed = 0;
        for (String fileKey : new String[] {AttachmentHolder.ATTACHMENTS_NBT_KEY, AttachmentHolder.NEOFORGE_NBT_KEY}) {
            if (!file.contains(fileKey)) continue;
            CompoundTag all = file.getCompoundOrEmpty(fileKey);
            for (String key : List.copyOf(all.keySet())) {
                if (ours(key)) {
                    all.remove(key);
                    removed++;
                }
            }
            if (all.isEmpty()) file.remove(fileKey);
        }
        return removed;
    }

    // ---------------------------------------------------------------- NBT and JSON

    static String toJson(Tag tag) {
        return clean(NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, tag)).toString();
    }

    /** JSON has no NaN or infinity, and a database refuses them. Nothing should make one, but a save must never fail over it. */
    private static JsonElement clean(JsonElement json) {
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
            double value = json.getAsDouble();
            if (Double.isNaN(value) || Double.isInfinite(value)) return new JsonPrimitive(0);
            return json;
        }
        if (json.isJsonArray()) {
            JsonArray out = new JsonArray();
            json.getAsJsonArray().forEach(e -> out.add(clean(e)));
            return out;
        }
        if (json.isJsonObject()) {
            JsonObject out = new JsonObject();
            json.getAsJsonObject().entrySet().forEach(e -> out.add(e.getKey(), clean(e.getValue())));
            return out;
        }
        return json;
    }

    static Tag fromJson(JsonElement json) {
        if (json.isJsonObject()) {
            CompoundTag out = new CompoundTag();
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()) out.put(entry.getKey(), fromJson(entry.getValue()));
            return out;
        }
        if (json.isJsonArray()) return list(json.getAsJsonArray());
        JsonPrimitive primitive = json.getAsJsonPrimitive();
        if (primitive.isBoolean()) return ByteTag.valueOf(primitive.getAsBoolean());
        if (primitive.isString()) return StringTag.valueOf(primitive.getAsString());
        return number(new BigDecimal(primitive.getAsString()), Width.NARROWEST);
    }

    private enum Width { NARROWEST, LONG, DOUBLE }

    private static Tag number(BigDecimal value, Width width) {
        if (width != Width.DOUBLE && value.stripTrailingZeros().scale() <= 0) {
            try {
                long whole = value.longValueExact();
                if (width == Width.NARROWEST && whole >= Integer.MIN_VALUE && whole <= Integer.MAX_VALUE) return IntTag.valueOf((int) whole);
                return LongTag.valueOf(whole);
            } catch (ArithmeticException tooBig) {
                return DoubleTag.valueOf(value.doubleValue());
            }
        }
        return DoubleTag.valueOf(value.doubleValue());
    }

    /** An NBT list holds one type, so numbers take the widest any of them needs; anything mixed is left to NbtOps. */
    private static Tag list(JsonArray array) {
        ListTag out = new ListTag();
        boolean allNumbers = !array.isEmpty();
        Width width = Width.NARROWEST;
        for (JsonElement e : array) {
            if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) {
                allNumbers = false;
                break;
            }
            BigDecimal value = new BigDecimal(e.getAsString());
            if (value.stripTrailingZeros().scale() > 0) width = Width.DOUBLE;
            else if (width == Width.NARROWEST && value.abs().compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) width = Width.LONG;
        }
        if (allNumbers) {
            for (JsonElement e : array) out.add(number(new BigDecimal(e.getAsString()), width == Width.NARROWEST ? Width.LONG : width));
            if (width == Width.NARROWEST) {
                ListTag ints = new ListTag();
                for (JsonElement e : array) ints.add(IntTag.valueOf(e.getAsInt()));
                return ints;
            }
            return out;
        }
        for (JsonElement e : array) out.add(fromJson(e));
        return out;
    }
}
