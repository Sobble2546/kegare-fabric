package com.cnethuka.kegare.platform.saved;

import com.cnethuka.kegare.Kegare;
import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** 26.x twin of the 1.21.11 file: the id is an Identifier ({@code data/kegare/<name>.dat}). */
public final class SavedDataCompat {
    private SavedDataCompat() {}

    public static <T extends SavedData> SavedDataType<T> type(String name, Supplier<T> constructor, Codec<T> codec) {
        return new SavedDataType<>(Kegare.id(name), constructor, codec, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
    }
}
