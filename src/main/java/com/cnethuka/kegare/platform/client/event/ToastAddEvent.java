package com.cnethuka.kegare.platform.client.event;

import com.cnethuka.kegare.platform.bus.Event;
import com.cnethuka.kegare.platform.bus.ICancellableEvent;
import net.minecraft.client.gui.components.toasts.Toast;

/** A toast is about to be queued. Cancel to keep it out (and add it later yourself if it should still show). */
public final class ToastAddEvent extends Event implements ICancellableEvent {
    private final Toast toast;

    public ToastAddEvent(Toast toast) {
        this.toast = toast;
    }

    public Toast getToast() {
        return toast;
    }
}
