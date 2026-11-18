package com.cnethuka.kegare.platform.client.event.sound;

import com.cnethuka.kegare.platform.bus.Event;
import javax.annotation.Nullable;
import net.minecraft.client.resources.sounds.SoundInstance;

/** A sound is about to start. {@code setSound(null)} silences it; another instance plays that one instead. */
public final class PlaySoundEvent extends Event {
    private final SoundInstance original;
    @Nullable
    private SoundInstance sound;

    public PlaySoundEvent(SoundInstance sound) {
        this.original = sound;
        this.sound = sound;
    }

    public SoundInstance getOriginalSound() {
        return original;
    }

    /** The sound that will play; null once a listener silenced it. */
    @Nullable
    public SoundInstance getSound() {
        return sound;
    }

    public void setSound(@Nullable SoundInstance sound) {
        this.sound = sound;
    }
}
