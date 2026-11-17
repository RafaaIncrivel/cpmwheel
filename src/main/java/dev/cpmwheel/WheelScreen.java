package dev.cpmwheel;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.systems.RenderSystem;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

public class WheelScreen extends Screen {
    private static final int PAGE_SIZE = 7;
    private static final double TAU = Math.PI * 2.0;

    private final KeyBinding openKey;
    private final List<WheelItem> root;
    private final WheelItem resetItem = WheelItem.reset();
    private final WheelItem backItem = WheelItem.back();

    private List<WheelItem> current;
    private WheelItem subOwner = null;

    private int page = 0;
    private int pages = 1;
    private boolean holding = true;
    private int ticksOpen = 0;
    private int hoveredSlot = -1;

    private List<WheelItem> pageItems = new ArrayList<>();
    private int slices = 4;
    private int bottomSlot = 2;

    private double cx;
    private double cy;
    private double outerR;
    private double innerR;

    public WheelScreen(KeyBinding openKey, List<WheelItem> items) {
        super(Text.literal("CPM Wheel"));
        this.openKey = openKey;
        this.root = items;
        this.current = items;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // ---------- layout ----------

    private void layout() {
        cx = this.width / 2.0;
        cy = this.height / 2.0;
        outerR = Math.min(Math.min(this.width, this.height) * 0.40, 150.0);
        innerR = outerR * 0.38;

        int total = current.size();
        pages = Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        if (page >= pages) {
            page = pages - 1;
        }
        if (page < 0) {
            page = 0;
        }
        int from = page * PAGE_SIZE;
        int to = Math.min(total, from + PAGE_SIZE);
        pageItems = current.subList(from, to);

        slices = Math.max(4, pageItems.size() + 1);
        if (slices % 2 != 0) {
            slices++;
        }
        bottomSlot = slices / 2; // fica sempre embaixo
    }

    private WheelItem bottomItem() {
        return subOwner == null ? resetItem : backItem;
    }

    private WheelItem itemAtSlot(int slot) {
        if (slot < 0 || slot >= slices) {
            return null;
        }
        if (slot == bottomSlot) {
            return bottomItem();
        }
        int j = slot < bottomSlot ? slot : slot - 1;
        return j < pageItems.size() ? pageItems.get(j) : null;
    }

    private int slotAt(double mx, double my) {
        double dx = mx - cx;
        double dy = my - cy;
        if (Math.sqrt(dx * dx + dy * dy) < innerR * 0.85) {
            return -1;
        }
        double a = Math.atan2(dx, -dy); // 0 = em cima, sentido horario
        if (a < 0) {
            a += TAU;
        }
        double step = TAU / slices;
        return (int) Math.floor((a + step / 2.0) / step) % slices;
    }

    // ---------- input ----------

    private boolean isOpenKeyHeld() {
        InputUtil.Key key = KeyBindingHelper.getBoundKeyOf(openKey);
        long handle = this.client.getWindow().getHandle();
        if (key.getCategory() == InputUtil.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(handle, key.getCode()) == GLFW.GLFW_PRESS;
        }
        return InputUtil.isKeyPressed(handle, key.getCode());
    }

    @Override
    public void tick() {
        ticksOpen++;
        if (holding && !isOpenKeyHeld()) {
            holding = false;
            WheelItem it = itemAtSlot(hoveredSlot);
            if (it != null) {
                activate(it);
            } else if (ticksOpen >= 10) {
                this.close(); // segurou e soltou sem escolher nada
            }
            // toque rapido: fica aberta em "modo clique"
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1) {
            this.close();
            return true;
        }
        if (button == 0) {
            WheelItem it = itemAtSlot(slotAt(mouseX, mouseY));
            if (it != null) {
                holding = false;
                activate(it);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (pages > 1) {
            page = (page + (amount < 0 ? 1 : -1) + pages) % pages;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    private void activate(WheelItem it) {
        try {
            switch (it.kind) {
                case DROPDOWN -> {
                    subOwner = it;
                    current = it.children();
                    page = 0;
                    holding = false;
                }
                case BACK -> {
                    subOwner = null;
                    current = root;
                    page = 0;
                    holding = false;
                }
                case RESET -> {
                    CpmBridge.resetAll();
                    this.close();
                }
                default -> {
                    it.run();
                    this.close();
                }
            }
        } catch (Throwable t) {
            CpmWheelClient.LOG.error("Erro ao executar '{}'", it.label, t);
            this.close();
        }
    }

    // ---------- desenho ----------

    private static void drawSector(Matrix4f m, double cx, double cy, double r0, double r1,
                                   double a0, double a1, int argb) {
        int al = (argb >>> 24) & 255;
        int r = (argb >> 16) & 255;
        int g = (argb >> 8) & 255;
        int b = argb & 255;
        int steps = 14;
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder buf = Tessellator.getInstance().getBuffer();
        buf.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= steps; i++) {
            double a = a0 + (a1 - a0) * i / steps;
            float s = (float) Math.sin(a);
            float c = (float) Math.cos(a);
            buf.vertex(m, (float) (cx + s * r1), (float) (cy - c * r1), 0f).color(r, g, b, al).next();
            buf.vertex(m, (float) (cx + s * r0), (float) (cy - c * r0), 0f).color(r, g, b, al).next();
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private static void drawDisc(Matrix4f m, double cx, double cy, double radius, int argb) {
        int al = (argb >>> 24) & 255;
        int r = (argb >> 16) & 255;
        int g = (argb >> 8) & 255;
        int b = argb & 255;
        int steps = 40;
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder buf = Tessellator.getInstance().getBuffer();
        buf.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        buf.vertex(m, (float) cx, (float) cy, 0f).color(r, g, b, al).next();
        for (int i = 0; i <= steps; i++) {
            double a = TAU * i / steps;
            buf.vertex(m, (float) (cx + Math.sin(a) * radius), (float) (cy - Math.cos(a) * radius), 0f)
                    .color(r, g, b, al).next();
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private void drawIcon(DrawContext ctx, IconCache.Icon icon, int centerX, int centerY, int size) {
        double scale = (double) size / Math.max(icon.w, icon.h);
        int dw = Math.max(1, (int) Math.round(icon.w * scale));
        int dh = Math.max(1, (int) Math.round(icon.h * scale));
        ctx.drawTexture(icon.id, centerX - dw / 2, centerY - dh / 2, dw, dh, 0f, 0f,
                icon.w, icon.h, icon.w, icon.h);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        layout();
        ctx.fill(0, 0, this.width, this.height, 0x78000000);
        hoveredSlot = slotAt(mouseX, mouseY);

        double step = TAU / slices;
        double gap = 0.03;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();

        for (int s = 0; s < slices; s++) {
            WheelItem it = itemAtSlot(s);
            int color;
            if (it == null) {
                color = 0x40101010;
            } else {
                boolean hov = s == hoveredSlot;
                boolean act = it.isActive();
                boolean special = it.kind == WheelItem.Kind.RESET || it.kind == WheelItem.Kind.BACK;
                if (act) {
                    color = hov ? 0xDDC83030 : 0xCC9B1C1C;
                } else if (hov) {
                    color = 0xCC5A5A5A;
                } else {
                    color = special ? 0x99283040 : 0x99151515;
                }
            }
            double a = s * step;
            drawSector(m, cx, cy, innerR + 2, outerR, a - step / 2 + gap, a + step / 2 - gap, color);
        }
        drawDisc(m, cx, cy, innerR - 4, 0xAA101010);

        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        // ícones e nomes
        double rm = (innerR + outerR) / 2.0;
        for (int s = 0; s < slices; s++) {
            WheelItem it = itemAtSlot(s);
            if (it == null) {
                continue;
            }
            double a = s * step;
            int px = (int) Math.round(cx + Math.sin(a) * rm);
            int py = (int) Math.round(cy - Math.cos(a) * rm);
            int textColor = s == hoveredSlot ? 0xFFFF55 : 0xFFFFFF;
            String raw = it.kind == WheelItem.Kind.DROPDOWN ? it.label + " >" : it.label;
            String text = this.textRenderer.trimToWidth(raw, 64);
            IconCache.Icon icon = IconCache.get(it.iconKey);
            if (icon != null) {
                drawIcon(ctx, icon, px, py - 8, 24);
                ctx.drawCenteredTextWithShadow(this.textRenderer, text, px, py + 8, textColor);
            } else {
                ctx.drawCenteredTextWithShadow(this.textRenderer, text, px, py - 4, textColor);
            }
        }

        // centro: informações do item sob o cursor
        WheelItem hov = itemAtSlot(hoveredSlot);
        int maxW = (int) (innerR * 1.7);
        if (hov != null) {
            ctx.drawCenteredTextWithShadow(this.textRenderer,
                    this.textRenderer.trimToWidth(hov.label, maxW), (int) cx, (int) cy - 8, 0xFFFFFF);
            String sub = hov.subtitle();
            if (!sub.isEmpty()) {
                ctx.drawCenteredTextWithShadow(this.textRenderer,
                        this.textRenderer.trimToWidth(sub, maxW), (int) cx, (int) cy + 4, 0xAAAAAA);
            }
        } else if (subOwner != null) {
            ctx.drawCenteredTextWithShadow(this.textRenderer,
                    this.textRenderer.trimToWidth(subOwner.label, maxW), (int) cx, (int) cy - 4, 0xFFFFFF);
        }

        // dicas e página
        int baseY = (int) (cy + outerR) + 10;
        String hint = Text.translatable(holding ? "cpm_wheel.hint_hold" : "cpm_wheel.hint_click").getString();
        ctx.drawCenteredTextWithShadow(this.textRenderer, hint, (int) cx, baseY, 0xAAAAAA);
        if (pages > 1) {
            String pg = Text.translatable("cpm_wheel.page", page + 1, pages).getString();
            ctx.drawCenteredTextWithShadow(this.textRenderer, pg, (int) cx, baseY + 12, 0xFFFFFF);
        }
    }
}
