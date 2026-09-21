package dile.ru.screens.modernui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;
import dile.ru.api.module.ModuleManager;
import dile.ru.api.module.impl.visual.ClickGuiModule;
import dile.ru.screens.modernui.impl.*;
import dile.ru.utils.math.MathUtils;
import dile.ru.utils.render.animation.modernfx.Decelerate;
import dile.ru.utils.render.animation.modernfx.Direction;
import dile.ru.utils.render.ui.Render2D;
import dile.ru.utils.render.ui.font.FontType;
import dile.ru.manager.Manager;

import dile.ru.screens.modernui.shader.ClickGuiCloudsRenderer;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class ClickGuiScreen extends Screen {
    private static final int OPEN_ANIMATION_MS = 250;
    private static final float OPEN_SCALE_START = 0.92F;
    private static final float BACKGROUND_DIM_MAX_ALPHA = 100.0F;
    private static final String GLOW_TEXTURE = "dile:textures/particles/glow.png";
    private static final float RIPPLE_DURATION_MS = 1500.0F;
    private static final float PARTICLE_DURATION_MS = 600.0F;
    private static final int PARTICLES_PER_CLICK = 25;


    private final PanelBackground background = new PanelBackground();
    private final Header header = new Header();
    private final CategoryPanel categoryPanel = new CategoryPanel();
    private final SearchHandler searchHandler = new SearchHandler();
    private final LeftPanel leftPanel = new LeftPanel();
    private final Decelerate openAnimation = MathUtils.createAnimation(OPEN_ANIMATION_MS, Direction.FORWARDS);
    private final List<Ripple> ripples = new ArrayList<>();
    private final List<ScatterParticle> particles = new ArrayList<>();
    private final Random random = new Random();

    private boolean closing;

    public ClickGuiScreen() {
        super(Component.literal("ClickGui"));
    }

    @Override
    protected void init() {
        super.init();
        closing = false;
        background.resetScroll();
        WorldAnimation.stop();
        openAnimation.setDirection(Direction.FORWARDS);
        openAnimation.reset();
        ripples.clear();
        particles.clear();
        ClickGuiCloudsRenderer.clearClicks();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        float progress = openProgress();
        Render2D.beginFrame(graphics);
        renderBackgroundDim(progress, width, height);
        Render2D.flush();

        ClickGuiModule cgMod = ClickGuiModule.getInstance();
        if (cgMod != null && cgMod.isCustomBackgroundEnabled()) {
            ClickGuiCloudsRenderer.render();
        }

        graphics.nextStratum();

        Render2D.beginFrame(graphics);
        float scale = OPEN_SCALE_START + (1.0F - OPEN_SCALE_START) * progress;
        float centerX = width * 0.5F;
        float centerY = height * 0.5F;
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, centerY);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-centerX, -centerY);
        renderContent(graphics, mouseX, mouseY);
        Render2D.flush();
        graphics.pose().popMatrix();
    }

    public void renderDetachedBackground(GuiGraphics graphics, float progress, float screenWidth, float screenHeight) {
        Render2D.beginFrame(graphics);
        renderBackgroundDim(progress, screenWidth, screenHeight);
        Render2D.flush();
    }

    public void renderDetached(GuiGraphics graphics, float partialTick) {
        graphics.nextStratum();
        Render2D.beginFrame(graphics);
        renderContent(graphics, Integer.MIN_VALUE, Integer.MIN_VALUE);
        Render2D.flush();
    }

    private void renderBackgroundDim(float progress, float screenWidth, float screenHeight) {
        float alpha = BACKGROUND_DIM_MAX_ALPHA * clamp(progress, 0.0F, 1.0F);
        if (alpha <= 0.5F) {
            return;
        }

        Render2D.rect(
                -5.0F,
                -5.0F,
                screenWidth + 10.0F,
                screenHeight + 10.0F,
                0.0F,
                new Color(0, 0, 0, Math.round(alpha)).getRGB()
        );

        long now = System.currentTimeMillis();
        ClickGuiModule mod = ClickGuiModule.getInstance();
        boolean customBg = mod != null && mod.isCustomBackgroundEnabled();

        if (customBg) {
            Render2D.image(
                    GLOW_TEXTURE,
                    -5.0F, -5.0F,
                    screenWidth + 10.0F, screenHeight + 10.0F,
                    0.0F,
                    new Color(255, 255, 255, Math.round(alpha * 0.35f)).getRGB()
            );

            Iterator<Ripple> rit = ripples.iterator();
            while (rit.hasNext()) {
                Ripple r = rit.next();
                float age = (now - r.startTime) / RIPPLE_DURATION_MS;
                if (age >= 1.0f) {
                    rit.remove();
                    continue;
                }
                float radius = age * Math.max(screenWidth, screenHeight) * 0.6f;
                float ringAlpha = (1.0f - age) * 0.5f;
                int ringColor = new Color(255, 255, 255, Math.round(ringAlpha * 255)).getRGB();
                Render2D.outline360(
                        r.x - radius, r.y - radius,
                        radius * 2, radius * 2,
                        radius,
                        1.5f,
                        ringColor,
                        Render2D.outline360Range(0.0F, 360.0F, ringColor)
                );
            }

            Iterator<ScatterParticle> pit = particles.iterator();
            while (pit.hasNext()) {
                ScatterParticle p = pit.next();
                float age = (now - p.startTime) / PARTICLE_DURATION_MS;
                if (age >= 1.0f) {
                    pit.remove();
                    continue;
                }
                float px = p.x + p.vx * age;
                float py = p.y + p.vy * age;
                float partAlpha = (1.0f - age) * 0.6f;
                float partSize = 20.0f * (1.0f - age * 0.5f);
                Render2D.image(
                        GLOW_TEXTURE,
                        px - partSize * 0.5f, py - partSize * 0.5f,
                        partSize, partSize,
                        0.0F,
                        new Color(255, 255, 255, Math.round(partAlpha * 255)).getRGB()
                );
            }
        }
    }

    private void renderContent(GuiGraphics graphics, int mouseX, int mouseY) {
        float panelWidth = 380.0F;
        float panelHeight = 240.0F;
        float leftPanelWidth = 130.0F;
        float gap = 12.0F;
        float totalWidth = leftPanelWidth + gap + panelWidth;
        float ox = (width - totalWidth) * 0.5F;
        float y = (height - panelHeight) * 0.5F;
        float leftX = ox;
        float x = ox + leftPanelWidth + gap;

        searchHandler.updateAnimations();
        background.render(graphics, x, y, panelWidth, panelHeight, categoryPanel.selectedCategory(), !normalizeSearch(searchHandler.getSearchText()).isEmpty(), currentModules(), mouseX, mouseY);
        leftPanel.render(graphics, leftX, y, mouseX, mouseY);
        Render2D.beginFrame(graphics);
        header.render(graphics, x, y, panelWidth, categoryPanel.previousCategory(), categoryPanel.selectedCategory(), categoryPanel.headerProgress(), searchHandler);
        categoryPanel.render(x, y);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (closing) {
            return false;
        }
        if (searchHandler.isSearchActive() && searchHandler.handleSearchKey(event)) {
            return true;
        }
        if (background.keyPressed(event)) {
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_RIGHT_CONTROL || event.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (closing) {
            return false;
        }
        if (searchHandler.isSearchActive() && searchHandler.handleSearchChar(event)) {
            return true;
        }
        if (background.charTyped(event)) {
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (closing) {
            return false;
        }
        float panelWidth = 380.0F;
        float panelHeight = 240.0F;
        float leftPanelWidth = 130.0F;
        float gap = 12.0F;
        float totalWidth = leftPanelWidth + gap + panelWidth;
        float ox = (width - totalWidth) * 0.5F;
        float y = (height - panelHeight) * 0.5F;
        float leftX = ox;
        float x = ox + leftPanelWidth + gap;

        if (leftPanel.mouseClicked(event, leftX, y)) {
            return true;
        }

        boolean searchHovered = header.isSearchBoxHovered(event.x(), event.y(), x, y);
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && searchHovered) {
            searchHandler.setSearchActive(true);
            return true;
        }
        if (searchHandler.isSearchActive() && !searchHovered) {
            searchHandler.setSearchActive(false);
        }
        if (background.mouseClicked(event, doubled)) {
            return true;
        }
        if (categoryPanel.mouseClicked(event, x, y)) {
            return true;
        }

        ClickGuiModule mod = ClickGuiModule.getInstance();
        if (mod != null && mod.isCustomBackgroundEnabled()) {
            spawnRipple((float) event.x(), (float) event.y());
        }

        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (closing) {
            return false;
        }
        if (leftPanel.mouseReleased(event)) {
            return true;
        }
        if (background.mouseReleased(event)) {
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (closing) {
            return false;
        }
        float panelWidth = 380.0F;
        float panelHeight = 240.0F;
        float leftPanelWidth = 130.0F;
        float gap = 12.0F;
        float totalWidth = leftPanelWidth + gap + panelWidth;
        float ox = (width - totalWidth) * 0.5F;
        float y = (height - panelHeight) * 0.5F;
        float leftX = ox;
        if (leftPanel.mouseDragged(event, dragX, dragY, leftX, y)) {
            return true;
        }
        if (background.mouseDragged(event, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (closing) {
            return false;
        }
        if (background.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (closing) {
            return;
        }
        closing = true;
        background.resetScroll();
        float progress = openProgress();
        WorldAnimation.start(this, progress, progress);
        minecraft.setScreen(null);
    }

    private void spawnRipple(float cx, float cy) {
        long now = System.currentTimeMillis();
        ripples.add(new Ripple(cx, cy, now));

        for (int i = 0; i < PARTICLES_PER_CLICK; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 80.0 + random.nextDouble() * 250.0;
            particles.add(new ScatterParticle(
                    cx, cy,
                    (float) (Math.cos(angle) * speed),
                    (float) (Math.sin(angle) * speed),
                    now
            ));
        }

        ClickGuiCloudsRenderer.addClick(cx, cy);
    }

    private List<Module> currentModules() {
        String query = normalizeSearch(searchHandler.getSearchText());
        if (query.isEmpty()) {
            return modulesForCategory(categoryPanel.selectedCategory());
        }

        ModuleManager manager = Manager.getModules();
        if (manager == null) {
            return List.of();
        }

        List<Module> filtered = new ArrayList<>();
        for (Module module : manager.getModules()) {
            if (normalizeSearch(module.getName()).contains(query)) {
                filtered.add(module);
            }
        }
        return filtered;
    }

    private List<Module> modulesForCategory(ModuleCategory category) {
        ModuleManager manager = Manager.getModules();
        if (manager == null || category == null) {
            return List.of();
        }
        return manager.getByCategory(category);
    }


    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private String normalizeSearch(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private float openProgress() {
        return clamp(openAnimation.getOutput().floatValue(), 0.0F, 1.0F);
    }

    private record Ripple(float x, float y, long startTime) {}

    private record ScatterParticle(float x, float y, float vx, float vy, long startTime) {}
}
