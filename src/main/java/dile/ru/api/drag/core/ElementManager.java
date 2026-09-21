package dile.ru.api.drag.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.Minecraft;
import dile.ru.api.config.ConfigManager;
import dile.ru.api.module.impl.visual.ClickGuiModule;
import dile.ru.utils.render.LoadingVisualGuard;
import dile.ru.utils.render.color.ColorUtil;
import dile.ru.utils.render.ui.Render2D;
import dile.ru.utils.render.ui.font.FontType;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class ElementManager {
    private static final ElementManager INSTANCE = new ElementManager();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String FILE_NAME = "drags" + ConfigManager.CONFIG_EXTENSION;
    private static final float HIT_PADDING = 3.0f;
    private static final int MAX_TRAIL_PARTICLES = 200;
    private static final float PARTICLE_SPAWN_RATE = 3.0f;
    private static final Random RANDOM = new Random();

    private final Map<String, ElementComponent> components = new LinkedHashMap<>();
    private final Map<String, SavedElement> pendingStates = new LinkedHashMap<>();
    private final List<ElementComponent> sortedScratch = new ArrayList<>();
    private final List<TrailParticle> trailParticles = new ArrayList<>();
    private ElementComponent active;
    private ElementScreen screen = new ElementScreen(1.0f, 1.0f, 1.0f);
    private int nextOrder;
    private boolean loaded;

    private ElementManager() {
    }

    public static ElementManager getInstance() {
        return INSTANCE;
    }

    public ElementComponent register(String id, String title, float defaultX, float defaultY) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Element id cannot be empty");
        }

        String key = normalize(id);
        ElementComponent existing = components.get(key);
        if (existing != null) {
            return existing;
        }

        ElementComponent component = new ElementComponent(key, title == null || title.isBlank() ? key : title, defaultX, defaultY, nextOrder++);
        SavedElement saved = pendingStates.get(key);
        if (saved != null) {
            component.applyState(saved.x, saved.y, saved.width, saved.height, saved.visible, saved.order);
            nextOrder = Math.max(nextOrder, saved.order + 1);
        }
        components.put(key, component);
        return component;
    }

    public List<ElementComponent> components() {
        return List.copyOf(components.values());
    }

    public void frame(ElementScreen screen) {
        if (screen != null && screen.valid()) {
            this.screen = screen;
        }
        for (ElementComponent component : components.values()) {
            component.clamp(this.screen);
        }
    }

    public boolean handleMouseClicked(MouseButtonEvent event) {
        if (event == null || event.button() != 0 || !canEdit()) {
            return false;
        }

        frame(ElementScreen.current());
        ElementComponent hovered = topmostAt((float) event.x(), (float) event.y());
        if (hovered == null) {
            return false;
        }

        active = hovered;
        active.order(nextOrder++);
        active.beginMove((float) event.x(), (float) event.y());
        return true;
    }

    public boolean handleMouseDragged(MouseButtonEvent event) {
        if (event == null || active == null || !canEdit()) {
            return false;
        }

        active.moveTo((float) event.x(), (float) event.y(), screen);
        return true;
    }

    public void updateActiveElementFromMouse() {
        if (active == null || !canEdit()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client == null || client.mouseHandler == null || client.getWindow() == null) {
            return;
        }

        ElementScreen currentScreen = ElementScreen.current();
        frame(currentScreen);
        float scale = Math.max(0.0001f, currentScreen.coordinateScale());
        float mouseX = (float) client.mouseHandler.getScaledXPos(client.getWindow()) / scale;
        float mouseY = (float) client.mouseHandler.getScaledYPos(client.getWindow()) / scale;
        active.moveTo(mouseX, mouseY, screen);
    }

    public boolean handleMouseReleased(MouseButtonEvent event) {
        if (active == null) {
            return false;
        }

        boolean editing = canEdit();
        active.endMove();
        active.commitClamp(screen);
        active = null;
        clearTrailParticles();
        save();
        return editing;
    }

    public void cancelActiveElement() {
        if (active == null) {
            return;
        }

        active.endMove();
        active.commitClamp(screen);
        active = null;
        clearTrailParticles();
        save();
    }

    public boolean canEditCurrentScreen() {
        return canEdit();
    }

    public void renderEditorOverlay(GuiGraphics graphics, ElementScreen screen) {
        if (!canEdit()) {
            return;
        }

        frame(screen);
        for (ElementComponent component : sortedComponents()) {
            if (!component.visible()) {
                continue;
            }
            renderComponentOverlay(component);
        }
    }

    public void load() {
        loaded = true;
        Path file = configFile();
        if (!Files.exists(file)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root == null || !root.has("components") || !root.get("components").isJsonObject()) {
                return;
            }

            JsonObject componentsObject = root.getAsJsonObject("components");
            for (String id : componentsObject.keySet()) {
                SavedElement saved = GSON.fromJson(componentsObject.get(id), SavedElement.class);
                if (saved == null || !saved.valid()) {
                    continue;
                }

                String key = normalize(id);
                pendingStates.put(key, saved);
                ElementComponent component = components.get(key);
                if (component != null) {
                    component.applyState(saved.x, saved.y, saved.width, saved.height, saved.visible, saved.order);
                    nextOrder = Math.max(nextOrder, saved.order + 1);
                }
            }
        } catch (Exception exception) {
            System.err.println("Failed to load element config: " + file + " (" + exception.getMessage() + ")");
        }
    }

    public void save() {
        if (!loaded) {
            return;
        }

        Path file = configFile();
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                JsonObject root = new JsonObject();
                root.addProperty("version", 1);
                JsonObject componentsObject = new JsonObject();
                for (ElementComponent component : components.values()) {
                    componentsObject.add(component.id(), GSON.toJsonTree(SavedElement.from(component)));
                }
                root.add("components", componentsObject);
                GSON.toJson(root, writer);
            }
        } catch (IOException exception) {
            System.err.println("Failed to save element config: " + file + " (" + exception.getMessage() + ")");
        }
    }

    private ElementComponent topmostAt(float mouseX, float mouseY) {
        ElementComponent topmost = null;
        int topmostOrder = Integer.MIN_VALUE;
        for (ElementComponent component : components.values()) {
            int order = component.order();
            if (order > topmostOrder && component.hit(mouseX, mouseY, HIT_PADDING)) {
                topmost = component;
                topmostOrder = order;
            }
        }
        return topmost;
    }

    private List<ElementComponent> sortedComponents() {
        sortedScratch.clear();
        sortedScratch.addAll(components.values());
        sortedScratch.sort(Comparator.comparingInt(ElementComponent::order));
        return sortedScratch;
    }

    private boolean canEdit() {
        Minecraft client = Minecraft.getInstance();
        return client != null
                && client.player != null
                && client.level != null
                && client.screen instanceof ChatScreen
                && !LoadingVisualGuard.shouldSuppressHud(client);
    }

    private void renderComponentOverlay(ElementComponent component) {
        if (!component.moving()) {
            return;
        }

        float cx = component.x();
        float cy = component.y();
        float w = component.width();
        float h = component.height();
        float dx = cx - component.prevX();
        float dy = cy - component.prevY();
        float speed = (float) Math.sqrt(dx * dx + dy * dy);
        if (speed < 0.1f) {
            return;
        }

        float intensity = Math.min(1.0f, speed / 5.0f);
        float nx = speed > 0.001f ? -dx / speed : 0.0f;
        float ny = speed > 0.001f ? -dy / speed : 0.0f;

        int spawnCount = Math.round(PARTICLE_SPAWN_RATE * intensity);
        for (int s = 0; s < spawnCount && trailParticles.size() < MAX_TRAIL_PARTICLES; s++) {
            float spawnX = cx + w * RANDOM.nextFloat();
            float spawnY = cy + h * RANDOM.nextFloat();
            float scatter = 1.5f + RANDOM.nextFloat() * 3.0f;
            float vx = nx * scatter + (RANDOM.nextFloat() - 0.5f) * 2.0f;
            float vy = ny * scatter + (RANDOM.nextFloat() - 0.5f) * 2.0f;
            float lifetime = 0.4f + RANDOM.nextFloat() * 0.6f;
            float size = 2.0f + RANDOM.nextFloat() * 4.0f;

            int baseColor = ClickGuiModule.getInstance().getColor();
            int r = Math.min(255, ColorUtil.getRed(baseColor) + RANDOM.nextInt(30));
            int g = Math.min(255, ColorUtil.getGreen(baseColor) + RANDOM.nextInt(35));
            int b = Math.min(255, ColorUtil.getBlue(baseColor) + RANDOM.nextInt(15));
            int alpha = 180 + RANDOM.nextInt(75);
            int color = ColorUtil.rgba(r, g, b, alpha);

            trailParticles.add(new TrailParticle(spawnX, spawnY, vx, vy, lifetime, size, color));
        }

        Iterator<TrailParticle> it = trailParticles.iterator();
        while (it.hasNext()) {
            TrailParticle p = it.next();
            p.update(0.016f);
            if (!p.alive()) {
                it.remove();
                continue;
            }

            float progress = 1.0f - p.lifetime / p.maxLifetime;
            float fade = progress < 0.2f ? progress / 0.2f : (1.0f - progress) / 0.8f;
            fade = Math.max(0.0f, Math.min(1.0f, fade));
            float currentSize = p.size * (1.0f - progress * 0.6f);

            int a = Math.round(((p.color >> 24) & 0xFF) / 255.0f * fade * 255.0f);
            int r = (p.color >> 16) & 0xFF;
            int g = (p.color >> 8) & 0xFF;
            int b = p.color & 0xFF;
            int particleColor = ColorUtil.rgba(r, g, b, a);

            Render2D.blur(p.x - currentSize, p.y - currentSize, currentSize * 2, currentSize * 2,
                    currentSize, 6.0f, 1.0f, particleColor);
        }

        float glowSize = 5.0f + 3.0f * intensity;
        int glowAlpha = Math.round(180.0f * intensity);
        int glowColor = ClickGuiModule.getInstance().getColor(glowAlpha / 255.0F);

        if (Math.abs(dx) >= Math.abs(dy)) {
            if (dx > 0) {
                Render2D.blur(cx - glowSize, cy, glowSize + 2, h, 4.0f, 10.0f, 1.0f, glowColor);
            } else {
                Render2D.blur(cx + w - 2, cy, glowSize + 2, h, 4.0f, 10.0f, 1.0f, glowColor);
            }
        } else {
            if (dy > 0) {
                Render2D.blur(cx, cy - glowSize, w, glowSize + 2, 4.0f, 10.0f, 1.0f, glowColor);
            } else {
                Render2D.blur(cx, cy + h - 2, w, glowSize + 2, 4.0f, 10.0f, 1.0f, glowColor);
            }
        }
    }

    private void clearTrailParticles() {
        trailParticles.clear();
    }

    private Path configFile() {
        return ConfigManager.systemDirectory().resolve(FILE_NAME);
    }

    private static String normalize(String id) {
        return id.trim().toLowerCase().replace(' ', '_');
    }

    private static final class TrailParticle {
        float x, y;
        float vx, vy;
        float lifetime;
        float maxLifetime;
        float size;
        int color;

        TrailParticle(float x, float y, float vx, float vy, float lifetime, float size, int color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.lifetime = lifetime;
            this.maxLifetime = lifetime;
            this.size = size;
            this.color = color;
        }

        boolean alive() {
            return lifetime > 0;
        }

        void update(float dt) {
            x += vx * dt;
            y += vy * dt;
            vx *= 0.96f;
            vy *= 0.96f;
            lifetime -= dt;
        }
    }

    private static final class SavedElement {
        private float x;
        private float y;
        private float width;
        private float height;
        private boolean visible = true;
        private int order;

        private static SavedElement from(ElementComponent component) {
            SavedElement saved = new SavedElement();
            saved.x = component.targetX();
            saved.y = component.targetY();
            saved.width = component.width();
            saved.height = component.height();
            saved.visible = component.visible();
            saved.order = component.order();
            return saved;
        }

        private boolean valid() {
            return Float.isFinite(x)
                    && Float.isFinite(y)
                    && Float.isFinite(width)
                    && Float.isFinite(height)
                    && width > 0.0f
                    && height > 0.0f;
        }
    }
}
