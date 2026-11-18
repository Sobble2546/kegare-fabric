package com.cnethuka.kegare.client.visual;

import com.cnethuka.kegare.platform.client.event.RenderNameTagEvent;
import com.cnethuka.kegare.platform.NeoForge;
import com.cnethuka.kegare.platform.TriState;

/**
 * Marks a body being drawn again where it isn't (an afterimage, a Severed player's sensed outline). A copy has no name (a name drawn
 * into one shows as a grey box), and it is not the body: whatever a render hook or a layer records about "the player as drawn" (a fist's
 * position, the plates a shell is drawn with) must not take a copy for it. Wrap the extraction and the submitting in {@link #draw}; a
 * recording hook returns early while {@link #active} is true.
 */
public final class BodyCopies {
    private static int drawing;

    private BodyCopies() {}

    public static void init() {
        NeoForge.EVENT_BUS.addListener((RenderNameTagEvent e) -> {
            if (drawing > 0) e.setCanRender(TriState.FALSE);
        });
    }

    /** Whether a copy of a body is being extracted or submitted right now (nothing may be remembered of what is drawn). */
    public static boolean active() {
        return drawing > 0;
    }

    public static void draw(Runnable draw) {
        drawing++;
        try {
            draw.run();
        } finally {
            drawing--;
        }
    }
}
