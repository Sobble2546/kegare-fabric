package com.cnethuka.kegare.platform.client.event;

import com.cnethuka.kegare.platform.bus.Event;
import java.util.function.Consumer;
import net.minecraft.client.KeyMapping;

/** Key bindings are being registered (once, during client initialisation). */
public final class RegisterKeyMappingsEvent extends Event {
    private final Consumer<KeyMapping> registrar;

    public RegisterKeyMappingsEvent(Consumer<KeyMapping> registrar) {
        this.registrar = registrar;
    }

    public void register(KeyMapping mapping) {
        registrar.accept(mapping);
    }
}
