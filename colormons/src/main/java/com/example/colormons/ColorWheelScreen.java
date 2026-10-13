package com.example.colormons;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.awt.Color;
import java.util.UUID;

public class ColorWheelScreen extends Screen {
    private static final int SIZE = 128;
    private static ResourceLocation wheelTex;

    private final UUID monId;
    private final int[] colors = {0xFFCC33, 0xCC3333, 0x222222};
    private final String[] names = {"Primary", "Secondary", "Tertiary"};
    private int selected = 0;
    private float brightness = 1f;
    private int wx, wy;

    public ColorWheelScreen(PokemonEntity mon) {
        super(Component.literal(mon.getPokemon().getSpecies().getName() + " Colours"));
        this.monId = mon.getPokemon().getUuid();
        int[] existing = ColorStore.get(monId);
        if (existing != null) System.arraycopy(existing, 0, colors, 0, 3);
    }

    @Override
    protected void init() {
        wx = width / 2 - SIZE - 10;
        wy = height / 2 - SIZE / 2;
        if (wheelTex == null) buildWheel();
        int bx = width / 2 + 20;
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            addRenderableWidget(Button.builder(Component.literal(names[i]), b -> selected = idx)
                    .bounds(bx + 24, wy + i * 26, 90, 20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Apply"), b -> ColorStore.set(monId, colors))
                .bounds(bx, wy + 90, 55, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Reset"), b -> ColorStore.clear(monId))
                .bounds(bx + 59, wy + 90, 55, 20).build());
    }

    private void buildWheel() {
        NativeImage img = new NativeImage(SIZE, SIZE, true);
        float r = SIZE / 2f;
        for (int y = 0; y < SIZE; y++) for (int x = 0; x < SIZE; x++) {
            float dx = x - r, dy = y - r, d = (float) Math.sqrt(dx * dx + dy * dy) / r;
            if (d > 1f) { img.setPixelRGBA(x, y, 0); continue; }
            float hue = (float) ((Math.atan2(dy, dx) / (2 * Math.PI) + 1) % 1);
            int rgb = Color.HSBtoRGB(hue, d, 1f);
            int R = (rgb >> 16) & 0xFF, G = (rgb >> 8) & 0xFF, B = rgb & 0xFF;
            img.setPixelRGBA(x, y, 0xFF000000 | (B << 16) | (G << 8) | R);
        }
        wheelTex = minecraft.getTextureManager().register("colormons_wheel", new DynamicTexture(img));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        g.blit(wheelTex, wx, wy, 0, 0, SIZE, SIZE, SIZE, SIZE);
        int sx = wx + SIZE + 6;
        for (int i = 0; i < SIZE; i++) {
            int v = 255 - i * 255 / SIZE;
            g.fill(sx, wy + i, sx + 10, wy + i + 1, 0xFF000000 | (v << 16) | (v << 8) | v);
        }
        int knob = wy + (int) ((1 - brightness) * SIZE);
        g.fill(sx - 2, knob - 1, sx + 12, knob + 1, 0xFFFF0000);
        int bx = width / 2 + 20;
        for (int i = 0; i < 3; i++) {
            int y = wy + i * 26;
            if (i == selected) g.fill(bx - 2, y - 2, bx + 22, y + 22, 0xFFFFFF00);
            g.fill(bx, y, bx + 20, y + 20, 0xFF000000 | colors[i]);
        }
        g.drawCenteredString(font, title, width / 2, wy - 20, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) { return pick(mx, my) || super.mouseClicked(mx, my, b); }

    @Override
    public boolean mouseDragged(double mx, double my, int b, double dx, double dy) {
        return pick(mx, my) || super.mouseDragged(mx, my, b, dx, dy);
    }

    private boolean pick(double mx, double my) {
        float r = SIZE / 2f;
        int sx = wx + SIZE + 6;
        if (mx >= sx && mx <= sx + 10 && my >= wy && my <= wy + SIZE) {
            brightness = 1f - (float) ((my - wy) / SIZE);
            int c = colors[selected];
            float[] hsb = Color.RGBtoHSB((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, null);
            colors[selected] = Color.HSBtoRGB(hsb[0], hsb[1], brightness) & 0xFFFFFF;
            return true;
        }
        double dx = mx - (wx + r), dy = my - (wy + r), d = Math.sqrt(dx * dx + dy * dy) / r;
        if (d <= 1) {
            float hue = (float) ((Math.atan2(dy, dx) / (2 * Math.PI) + 1) % 1);
            colors[selected] = Color.HSBtoRGB(hue, (float) d, brightness) & 0xFFFFFF;
            return true;
        }
        return false;
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
