package com.cnethuka.kegare.platform.saved;

import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Builds the {@link SavedDataType} for a Kegare world-data file. 1.21.11 file: the id is the file name ({@code data/<name>.dat}); 26.x
 * takes an Identifier there, so the twin in targets/26.3/java builds one. The data fixer type is a vanilla one whose fixers never touch
 * our tags: 1.21.11 does not accept null here (reading an existing file would throw).
 */
public final class SavedDataCompat {
    private SavedDataCompat() {}

    public static <T extends SavedData> SavedDataType<T> type(String name, Supplier<T> constructor, Codec<T> codec) {
        return new SavedDataType<>(name, constructor, codec, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
    }
}
