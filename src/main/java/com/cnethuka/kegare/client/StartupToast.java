package com.cnethuka.kegare.client;

import com.cnethuka.kegare.Kegare;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import com.cnethuka.kegare.client.gui.GuiDraw;

/**
 * "Kegare initialized" with a green check, shown once per game launch when the main menu first appears (the way Essential
 * announces itself). It is client-only and has nothing to do with any server: every client with the mod gets it. The check is a
 * sprite because the default font draws U+2705 as a plain glyph or a box.
 */
public final class StartupToast implements Toast {
    private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("toast/advancement");
    private static final Identifier CHECK = Kegare.id("toast_check");
    private static final long SHOWN_MS = 5000;
    private static final int W = DEFAULT_WIDTH;
    private static final int H = SLOT_HEIGHT;

    private static boolean titleSeen;
    private static boolean shown;

    private Visibility wanted = Visibility.SHOW;

    private StartupToast() {
    }

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof TitleScreen) titleSeen = true;
        });
        // Wait for the loading overlay to be gone, so the toast does not slide in under its fade.
        ClientTickEvents.END_CLIENT_TICK.register(StartupToast::tick);
    }

    private static void tick(Minecraft mc) {
        if (shown || !titleSeen || mc.getOverlay() != null || !(mc.screen instanceof TitleScreen)) return;
        shown = true;
        mc.getToastManager().addToast(new StartupToast());
    }

    @Override
    public int width() {
        return W;
    }

    @Override
    public int height() {
        return H;
    }

    @Override
    public void update(ToastManager toasts, long fullyVisibleForMs) {
        wanted = fullyVisibleForMs >= SHOWN_MS * toasts.getNotificationDisplayTimeMultiplier() ? Visibility.HIDE : Visibility.SHOW;
    }

    @Override
    public Visibility getWantedVisibility() {
        return wanted;
    }

    @Override
    public void render(GuiGraphics g, Font font, long fullyVisibleForMs) {
        GuiDraw.sprite(g, BACKGROUND, 0, 0, W, H);
        GuiDraw.sprite(g, CHECK, 8, 8, 16, 16);
        g.drawString(font, Component.translatable("toast.kegare.initialized"), 30, 12, 0xFFFFFFFF, true);
    }
}
