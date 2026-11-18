package com.cnethuka.kegare.client.gui;

import com.cnethuka.kegare.Kegare;
import com.cnethuka.kegare.innatetechnique.Ability;
import com.cnethuka.kegare.innatetechnique.InnateTechnique;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * Shared drawing helpers for Kegare screens and the HUD. Where the art lives:
 * <ul>
 *   <li>Sprites in {@code textures/gui/sprites/} (nine-slice where a {@code .mcmeta} says so; edges and centres TILE).</li>
 *   <li>Innate technique emblems: {@code <namespace>:textures/gui/emblem/<path>.png} (48x48).</li>
 *   <li>Ability icons: {@code <namespace>:textures/gui/ability/<path>.png} (16x16).</li>
 * </ul>
 * Emblems and icons are optional: content without art falls back to its advancement tab item or a slot number.
 */
public final class GuiDraw {
    public static final Identifier PANEL = Kegare.id("panel");
    public static final Identifier TAB = Kegare.id("tab");
    public static final Identifier TAB_SELECTED = Kegare.id("tab_selected");
    public static final Identifier SPARKLE = Kegare.id("sparkle");
    public static final Identifier SEVERED_EMBLEM = Kegare.id("severed_emblem");
    public static final Identifier DIVIDER = Kegare.id("divider");
    public static final Identifier ENV_COLD = Kegare.id("env_cold");
    public static final Identifier ENV_HOT = Kegare.id("env_hot");
    public static final Identifier LOCK = Kegare.id("lock");
    public static final Identifier FREEZE_PIP = Kegare.id("freeze_pip");
    public static final Identifier FREEZE_PIP_EMPTY = Kegare.id("freeze_pip_empty");

    private static final Identifier INK_BACKGROUND = Kegare.id("textures/gui/reveal_bg.png");
    private static final int INK_BACKGROUND_SIZE = 128;

    /**
     * Malice bar texture: 32x30. Rows 0-9 the empty bar, 10-19 the fill's shading in greyscale (tinted with the
     * innate technique's Malice color), 20-29 a white shine drawn untinted on top. 5px caps, a 22px middle that tiles.
     */
    private static final Identifier MALICE_BAR = Kegare.id("textures/gui/malice_bar.png");
    public static final int BAR_HEIGHT = 10;
    private static final int BAR_TEXTURE_W = 32;
    private static final int BAR_TEXTURE_H = 30;
    private static final int BAR_CAP = 5;
    private static final int BAR_MIDDLE = BAR_TEXTURE_W - 2 * BAR_CAP;

    /** The ability categories' 16x16 tab icons on the Sorcerer screen (DESIGN §3.16), by {@code AbilityCategory} order. */
    public static final Identifier[] CATEGORY_ICONS = {Kegare.id("category_innate"), Kegare.id("category_malice"), Kegare.id("category_melee")};
    public static final Identifier BADGE_SPECIAL = Kegare.id("badge_special");
    public static final Identifier BADGE_SPECIAL_NONE = Kegare.id("badge_special_none");

    private static final int EMBLEM_TEXTURE_SIZE = 48;
    private static final int DIVIDER_HEIGHT = 7;

    public static final int WHITE = 0xFFFFFFFF;
    public static final int GRAY = 0xFFAAAAAA;
    public static final int DARK_GRAY = 0xFF777777;
    public static final int MALICE = 0xFFD9B3FF;
    public static final int GOLD = 0xFFFFD24A;
    public static final int AQUA = 0xFF7FE6FF;
    public static final int RED = 0xFFFF6B6B;
    /** The near-black outline every piece of art uses. */
    public static final int INK = 0xFF0B0810;
    private static final int WELL = 0xFF1E1625;
    /** Cooldown overlay (combat panel): darken the slot, then a violet sweep for the remaining fraction. */
    public static final int COOLDOWN_DIM = 0xA0000000;
    public static final int COOLDOWN_SWEEP = 0x559D4EDD;

    /** Whether optional art exists, by texture location. Cleared on resource reload (see KegareClient). */
    private static final Map<Identifier, Boolean> ART_EXISTS = new ConcurrentHashMap<>();

    private GuiDraw() {}

    // ---------------------------------------------------------------- optional art lookup

    @Nullable
    private static Identifier art(String folder, Identifier id) {
        Identifier texture = id.withPath(path -> "textures/gui/" + folder + "/" + path + ".png");
        boolean exists = ART_EXISTS.computeIfAbsent(texture,
                t -> Minecraft.getInstance().getResourceManager().getResource(t).isPresent());
        return exists ? texture : null;
    }

    public static void clearArtCache() {
        ART_EXISTS.clear();
    }

    // ---------------------------------------------------------------- primitives

    public static void sprite(GuiGraphics g, Identifier sprite, int x, int y, int w, int h) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, w, h);
    }

    /** Packs an RGB color and an alpha (0..1) into the ARGB int the 1.21.11 GUI draw calls take as their tint. */
    public static int argb(int rgb, float alpha) {
        return (Mth.clamp(Math.round(alpha * 255f), 0, 255) << 24) | (rgb & 0xFFFFFF);
    }

    /** White at an alpha: the tint for fading a sprite or texture. */
    public static int fade(float alpha) {
        return argb(0xFFFFFF, alpha);
    }

    /** A whole-texture or region blit with the GUI's textured pipeline (alpha-blended), at 1 texel per GUI pixel. */
    public static void blit(GuiGraphics g, Identifier texture, int x, int y, float u, float v, int w, int h, int texW, int texH) {
        g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, w, h, texW, texH);
    }

    /** {@link #blit} multiplied by an ARGB tint. */
    public static void blit(GuiGraphics g, Identifier texture, int x, int y, float u, float v, int w, int h, int texW, int texH, int argb) {
        g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, w, h, texW, texH, argb);
    }

    /** A region {@code (u, v, regionW x regionH)} of a texture stretched to {@code w x h}. */
    public static void blitScaled(GuiGraphics g, Identifier texture, int x, int y, int w, int h, float u, float v, int regionW, int regionH, int texW, int texH) {
        g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, w, h, regionW, regionH, texW, texH);
    }

    /** Sprite multiplied by an RGB color (the sparkle is white so it can take any color). */
    public static void tinted(GuiGraphics g, Identifier sprite, int x, int y, int w, int h, int rgb, float alpha) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, w, h, argb(rgb, alpha));
    }

    /** {@link #tinted} blended additively (adds light instead of covering): glows, rays, flames' halos. */
    public static void tintedAdditive(GuiGraphics g, Identifier sprite, int x, int y, int w, int h, int rgb, float alpha) {
        g.blitSprite(GuiPipelines.GUI_ADDITIVE, sprite, x, y, w, h, argb(rgb, alpha));
    }

    /** Full-screen, opaque smoky ink (tiled) that darkens in steps toward the edges. */
    public static void inkBackground(GuiGraphics g, int w, int h) {
        // opaque, tiled (a draw call no longer repeats its UVs, so lay the tiles out)
        for (int ty = 0; ty < h; ty += INK_BACKGROUND_SIZE) {
            for (int tx = 0; tx < w; tx += INK_BACKGROUND_SIZE) {
                blit(g, INK_BACKGROUND, tx, ty, 0f, 0f, Math.min(INK_BACKGROUND_SIZE, w - tx), Math.min(INK_BACKGROUND_SIZE, h - ty),
                        INK_BACKGROUND_SIZE, INK_BACKGROUND_SIZE);
            }
        }
        int steps = 8;
        int band = Math.max(2, Math.min(w, h) / 20);
        for (int i = 0; i < steps; i++) {
            float f = 1f - i / (float) steps;
            int color = ((int) (150 * f * f) << 24) | 0x030206;
            int o = i * band;
            if (o * 2 + band * 2 >= Math.min(w, h)) break;
            g.fill(o, o, w - o, o + band, color);
            g.fill(o, h - o - band, w - o, h - o, color);
            g.fill(o, o + band, o + band, h - o - band, color);
            g.fill(w - o - band, o + band, w - o, h - o - band, color);
        }
    }

    /** A brush-stroke divider centred on {@code centerX}. */
    public static void divider(GuiGraphics g, int centerX, int y, int w) {
        sprite(g, DIVIDER, centerX - w / 2, y, Math.max(24, w), DIVIDER_HEIGHT);
    }

    /** Section heading: text at a scale with a brush-stroke divider under it. Returns the height used. */
    public static int heading(GuiGraphics g, Font font, Component text, int x, int y, float scale, int maxWidth, int color) {
        int textHeight = text(g, font, text, x, y, scale, color);
        int w = Mth.clamp((int) (font.width(text) * scale) + 28, 40, Math.max(40, maxWidth));
        sprite(g, DIVIDER, x - 2, y + textHeight, w, DIVIDER_HEIGHT);
        return textHeight + DIVIDER_HEIGHT + 2;
    }

    /** Thin progress bar in the same inked style as the art: outline, dark well, colored fill with a highlight. */
    public static void progress(GuiGraphics g, int x, int y, int w, int h, float fraction, int color) {
        g.fill(x, y, x + w, y + h, INK);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, WELL);
        int fill = Math.round((w - 2) * Mth.clamp(fraction, 0f, 1f));
        if (fill > 0) {
            g.fill(x + 1, y + 1, x + 1 + fill, y + h - 1, 0xFF000000 | color);
            g.fill(x + 1, y + 1, x + 1 + fill, y + 2, 0x60FFFFFF);
        }
    }

    // ---------------------------------------------------------------- Malice bar

    /** The Malice bar in the default purple, no ghost. */
    public static void maliceBar(GuiGraphics g, int x, int y, int w, float fraction) {
        maliceBar(g, x, y, w, fraction, InnateTechnique.DEFAULT_MALICE_COLOR, -1f);
    }

    /**
     * The Malice bar (HUD and Sorcerer screen): {@link #BAR_HEIGHT} tall, any width >= 10, in the innate technique's
     * Malice color: a soft glow round the filled part, the fill, and a paler "ghost" from the fill up to
     * {@code ghost} (Malice just spent, draining away; below 0 = none).
     */
    public static void maliceBar(GuiGraphics g, int x, int y, int w, float fraction, int rgb, float ghost) {
        w = Math.max(2 * BAR_CAP, w);
        int fill = Mth.clamp(Math.round(w * fraction), 0, w);
        int ghostFill = ghost < 0 ? fill : Mth.clamp(Math.round(w * ghost), fill, w);
        if (fill > 0) { // glow
            g.fill(x - 1, y - 1, x + fill + 1, y + BAR_HEIGHT + 1, 0x38000000 | (rgb & 0xFFFFFF));
            g.fill(x - 2, y, x + fill + 2, y + BAR_HEIGHT, 0x20000000 | (rgb & 0xFFFFFF));
        }
        barRow(g, x, y, w, 0, WHITE);
        if (ghostFill > fill) {
            g.enableScissor(x + fill, y, x + ghostFill, y + BAR_HEIGHT);
            barRow(g, x, y, w, BAR_HEIGHT, argb(mix(rgb, 0xFFFFFF, 0.45f), 0.55f));
            g.disableScissor();
        }
        if (fill > 0) {
            g.enableScissor(x, y, x + fill, y + BAR_HEIGHT); // intersects with any enclosing scissor
            barRow(g, x, y, w, BAR_HEIGHT, argb(rgb, 1f));
            barRow(g, x, y, w, 2 * BAR_HEIGHT, WHITE); // shine, untinted
            g.disableScissor();
        }
    }

    /** Blends two RGB colors ({@code t} = 0: a, 1: b). */
    public static int mix(int a, int b, float t) {
        int r = (int) Mth.lerp(t, (a >> 16) & 0xFF, (b >> 16) & 0xFF);
        int gr = (int) Mth.lerp(t, (a >> 8) & 0xFF, (b >> 8) & 0xFF);
        int bl = (int) Mth.lerp(t, a & 0xFF, b & 0xFF);
        return (r << 16) | (gr << 8) | bl;
    }

    private static void barRow(GuiGraphics g, int x, int y, int w, int v, int argb) {
        blit(g, MALICE_BAR, x, y, 0f, v, BAR_CAP, BAR_HEIGHT, BAR_TEXTURE_W, BAR_TEXTURE_H, argb);
        for (int dx = BAR_CAP; dx < w - BAR_CAP; dx += BAR_MIDDLE) {
            int segment = Math.min(BAR_MIDDLE, w - BAR_CAP - dx);
            blit(g, MALICE_BAR, x + dx, y, BAR_CAP, v, segment, BAR_HEIGHT, BAR_TEXTURE_W, BAR_TEXTURE_H, argb);
        }
        blit(g, MALICE_BAR, x + w - BAR_CAP, y, BAR_TEXTURE_W - BAR_CAP, v, BAR_CAP, BAR_HEIGHT, BAR_TEXTURE_W, BAR_TEXTURE_H, argb);
    }

    // ---------------------------------------------------------------- abilities

    /** The small "R" badge on a slot: lit if the ability has a special, greyed out if not. 9x9. */
    public static void specialBadge(GuiGraphics g, int x, int y, boolean hasSpecial) {
        // drawn after the slot's icon, so it lands on top (there is no z to raise any more: overlapping GUI draws stack in call order)
        sprite(g, hasSpecial ? BADGE_SPECIAL : BADGE_SPECIAL_NONE, x, y, 9, 9);
    }

    /** The ability's 16x16 icon at 1:1. Returns false (drawing nothing) if it has no icon art. */
    public static boolean abilityIcon(GuiGraphics g, Ability ability, int x, int y) {
        return abilityIcon(g, ability.id(), x, y);
    }

    /** A locked ability's icon, greyed out. */
    public static boolean greyedAbilityIcon(GuiGraphics g, Ability ability, int x, int y) {
        return abilityIcon(g, ability.id(), x, y, 0xFF6B6B6B);
    }

    public static boolean abilityIcon(GuiGraphics g, Identifier ability, int x, int y) {
        return abilityIcon(g, ability, x, y, WHITE);
    }

    /** The ability's icon faded to {@code alpha}. */
    public static boolean abilityIcon(GuiGraphics g, Ability ability, int x, int y, float alpha) {
        return abilityIcon(g, ability.id(), x, y, fade(alpha));
    }

    private static boolean abilityIcon(GuiGraphics g, Identifier ability, int x, int y, int argb) {
        Identifier texture = art("ability", ability);
        if (texture == null) return false;
        blit(g, texture, x, y, 0f, 0f, 16, 16, 16, 16, argb);
        return true;
    }

    /** "0.85", "1.5": a cost multiplier with at most two decimals. */
    public static String multiplier(float value) {
        String s = String.format(java.util.Locale.ROOT, "%.2f", value);
        return s.endsWith("0") ? s.substring(0, s.length() - 1) : s;
    }

    // ---------------------------------------------------------------- innate techniques

    /**
     * The innate technique's emblem in an ink frame. Art is 48x48; sizes that are a multiple of 48 stay crisp.
     * Without art: its advancement tab item on a dark well.
     */
    public static void emblem(GuiGraphics g, InnateTechnique innateTechnique, int x, int y, int size) {
        emblem(g, innateTechnique, x, y, size, 1f);
    }

    /** {@link #emblem} faded to {@code alpha} (the item stand-in, drawn when there is no art, cannot be faded and stays solid). */
    public static void emblem(GuiGraphics g, InnateTechnique innateTechnique, int x, int y, int size, float alpha) {
        g.fill(x - 1, y - 1, x + size + 1, y + size + 1, scaleAlpha(INK, alpha));
        Identifier texture = art("emblem", innateTechnique.id());
        if (texture != null) {
            g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0f, 0f, size, size, EMBLEM_TEXTURE_SIZE, EMBLEM_TEXTURE_SIZE,
                    EMBLEM_TEXTURE_SIZE, EMBLEM_TEXTURE_SIZE, fade(alpha));
            return;
        }
        g.fill(x, y, x + size, y + size, scaleAlpha(WELL, alpha));
        float scale = Math.max(0.5f, size / 16f * 0.6f);
        g.pose().pushMatrix();
        g.pose().translate(x + size / 2f - 8 * scale, y + size / 2f - 8 * scale);
        g.pose().scale(scale, scale);
        g.renderItem(new ItemStack(innateTechnique.tabIcon()), 0, 0);
        g.pose().popMatrix();
    }

    /** An ARGB color with its alpha multiplied by {@code alpha}. */
    public static int scaleAlpha(int argb, float alpha) {
        return (Mth.clamp(Math.round((argb >>> 24) * alpha), 0, 255) << 24) | (argb & 0xFFFFFF);
    }

    /** The emblem plus a 1px rim in the rarity color. */
    public static void framedEmblem(GuiGraphics g, InnateTechnique innateTechnique, int x, int y, int size) {
        framedEmblem(g, innateTechnique, x, y, size, 1f);
    }

    public static void framedEmblem(GuiGraphics g, InnateTechnique innateTechnique, int x, int y, int size, float alpha) {
        g.renderOutline(x - 2, y - 2, size + 4, size + 4, scaleAlpha(0xFF000000 | innateTechnique.rarity().color(), alpha));
        emblem(g, innateTechnique, x, y, size, alpha);
    }

    // ---------------------------------------------------------------- text

    /** Centered text at a scale (1 = normal). */
    public static void centered(GuiGraphics g, Font font, FormattedCharSequence text, int centerX, int y, float scale, int color) {
        g.pose().pushMatrix(); // text submitted after an icon draws over it (no z to raise)
        g.pose().scale(scale, scale);
        g.drawString(font, text, (int) (centerX / scale - font.width(text) / 2f), (int) (y / scale), color, true);
        g.pose().popMatrix();
    }

    public static void centered(GuiGraphics g, Font font, Component text, int centerX, int y, float scale, int color) {
        centered(g, font, text.getVisualOrderText(), centerX, y, scale, color);
    }

    /** Left-aligned text at a scale. Returns the scaled line height. */
    public static int text(GuiGraphics g, Font font, Component text, int x, int y, float scale, int color) {
        g.pose().pushMatrix();
        g.pose().scale(scale, scale);
        g.drawString(font, text, (int) (x / scale), (int) (y / scale), color, true);
        g.pose().popMatrix();
        return (int) Math.ceil(font.lineHeight * scale);
    }

    /** Word-wrapped, left-aligned. Returns the height used. */
    public static int wrapped(GuiGraphics g, Font font, Component text, int x, int y, int width, int color) {
        int dy = 0;
        for (FormattedCharSequence line : font.split(text, Math.max(20, width))) {
            g.drawString(font, line, x, y + dy, color, true);
            dy += font.lineHeight + 1;
        }
        return dy;
    }

    /** Word-wrapped, centered. Returns the height used. */
    public static int wrappedCentered(GuiGraphics g, Font font, Component text, int centerX, int y, int width, int color) {
        int dy = 0;
        for (FormattedCharSequence line : font.split(text, Math.max(20, width))) {
            g.drawString(font, line, centerX - font.width(line) / 2, y + dy, color, true);
            dy += font.lineHeight + 1;
        }
        return dy;
    }

    /** Lang key for an innate technique's or ability's one-line description. */
    public static Component description(String descriptionId) {
        return Component.translatable(descriptionId + ".desc");
    }

    /** An ability's description, with the configured numbers it states filled in. */
    public static Component description(com.cnethuka.kegare.innatetechnique.Ability ability) {
        return Component.translatable(ability.descriptionId() + ".desc", ability.descriptionArgs());
    }

    public static String seconds(int ticks) {
        return String.format(java.util.Locale.ROOT, "%.1f", ticks / 20.0);
    }

    /** Formats a Malice amount: whole numbers stay whole, fractions keep one decimal (e.g. upkeep 2.5/s). */
    public static String amount(float value) {
        return value == Math.floor(value) ? Integer.toString((int) value) : String.format(java.util.Locale.ROOT, "%.1f", value);
    }
}
