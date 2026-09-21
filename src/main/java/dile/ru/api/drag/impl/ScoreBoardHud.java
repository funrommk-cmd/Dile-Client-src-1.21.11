package dile.ru.api.drag.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import dile.ru.api.config.ConfigManager;
import dile.ru.api.drag.core.ElementScreen;
import dile.ru.api.module.impl.visual.ClickGuiModule;
import dile.ru.utils.render.animation.Easings;
import dile.ru.utils.render.animation.SmoothAnimation;
import dile.ru.utils.render.color.ColorUtil;
import dile.ru.utils.render.ui.Render2D;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ScoreBoardHud extends HudPanel {
    private static final float LINE_HEIGHT = 8.0F;
    private static final float HEADER_HEIGHT = 10.0F;
    private static final float PANEL_PADDING = 5.0F;
    private static final float TEXT_SIZE = 6.0F;
    private static final float HEADER_TEXT_SIZE = 7.0F;

    private static final int BG_NONE = 0;
    private static final int BG_GLASS = 1;
    private static final int BG_BLUR = 2;
    private static final int BG_NORMAL = 3;
    private static final String[] BG_NAMES = {"Нету", "Стекло", "Blur", "Обычный"};

    private static final float POPUP_WIDTH = 105.0F;
    private static final float POPUP_ROW_HEIGHT = 12.0F;
    private static final float POPUP_PADDING = 5.0F;
    private static final float TOGGLE_WIDTH = 20.0F;
    private static final float TOGGLE_HEIGHT = 10.0F;
    private static final float TOGGLE_CIRCLE_SIZE = 7.0F;
    private static final float TOGGLE_PADDING = 1.5F;
    private static final float TOGGLE_CORNER = TOGGLE_HEIGHT * 0.5F;
    private static final float SLIDER_WIDTH = 55.0F;
    private static final float SLIDER_HEIGHT = 4.0F;
    private static final float SIDE_MARGIN = 2.0F;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE = ConfigManager.systemDirectory().resolve("scoreboard" + ConfigManager.CONFIG_EXTENSION);

    private static ScoreBoardHud instance;

    private final SmoothAnimation panelAnimation = new SmoothAnimation();
    private final SmoothAnimation toggleAnimation = new SmoothAnimation();
    private final SmoothAnimation popupAnimation = new SmoothAnimation();

    private boolean settingsOpen;
    private int backgroundMode = BG_GLASS;
    private boolean sizeEnabled;
    private float sizeMultiplier = 1.0F;
    private boolean draggingSlider;
    private boolean draggingPopup;

    public ScoreBoardHud() {
        super("scoreboard", "ScoreBoard", 760.0F, 10.0F, 90.0F, 60.0F);
        instance = this;
        drag.locked(true);
        loadSettings();
    }

    public static ScoreBoardHud getInstance() {
        return instance;
    }

    public boolean isSettingsOpen() {
        return settingsOpen;
    }

    public void closeSettings() {
        settingsOpen = false;
        draggingSlider = false;
    }

    public void resetToVanilla() {
        float width = drag.width();
        positionToVanilla(width);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (event == null || !editPreview()) {
            return false;
        }
        float mx = (float) event.x();
        float my = (float) event.y();

        if (event.button() == 1) {
            if (settingsOpen) {
                if (handlePopupClick(mx, my)) {
                    return true;
                }
                settingsOpen = false;
                draggingSlider = false;
                return false;
            }
            float sx = drag.x();
            float sy = drag.y();
            float sw = drag.width();
            float sh = drag.height();
            if (mx >= sx && mx <= sx + sw && my >= sy && my <= sy + sh) {
                settingsOpen = !settingsOpen;
                toggleAnimation.set(sizeEnabled ? 1.0F : 0.0F);
                popupAnimation.set(settingsOpen ? 1.0F : 0.0F);
                return true;
            }
        }

        if (event.button() == 0 && settingsOpen) {
            if (handlePopupClick(mx, my)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event == null) {
            return false;
        }
        if (event.button() == 0) {
            draggingSlider = false;
            draggingPopup = false;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event) {
        if (event == null || !settingsOpen || !draggingSlider) {
            return false;
        }
        float mx = (float) event.x();
        float popupX = drag.x() - POPUP_WIDTH - 5.0F;
        if (popupX < 0) popupX = drag.x() + drag.width() + 5.0F;
        float sliderStartX = popupX + 48.0F;
        float sliderEndX = sliderStartX + SLIDER_WIDTH;
        float t = clamp((mx - sliderStartX) / (sliderEndX - sliderStartX), 0.0F, 1.0F);
        sizeMultiplier = 0.5F + t * 1.5F;
        return true;
    }

    @Override
    public void render() {
        ScoreBoardState state = logics();
        if (state == null) {
            return;
        }
        renderScoreboard(state);
        if (settingsOpen && editPreview()) {
            renderPopup();
        }
    }

    private void positionToVanilla(float panelWidth) {
        ElementScreen screen = ElementScreen.current();
        float screenW = screen.width();
        drag.position(screenW - panelWidth - SIDE_MARGIN, 1.0F);
    }

    private ScoreBoardState logics() {
        ScoreboardStateRaw raw = collectScoreboard();
        boolean hasContent = raw != null;
        boolean preview = !hasContent && editPreview();
        boolean targetVisible = hasContent || preview;

        panelAnimation.update();
        panelAnimation.run(targetVisible ? 1.0 : 0.0, 0.24, targetVisible ? Easings.EXPO_OUT : Easings.EXPO_IN, true);

        float panelAlpha = panelAnimation.get();
        boolean visible = targetVisible || panelAlpha > 0.01F;
        contentVisible(visible);
        if (!visible) {
            return null;
        }

        float scale = sizeEnabled ? sizeMultiplier : 1.0F;

        List<ScoreLine> lines = new ArrayList<>();
        String header;
        float width = 90.0F * scale;

        if (preview) {
            header = "Scoreboard";
            lines.add(new ScoreLine("Player1", "100", 1.0F, new ArrayList<>(), Component.literal("Player1")));
            lines.add(new ScoreLine("Player2", "75", 1.0F, new ArrayList<>(), Component.literal("Player2")));
            lines.add(new ScoreLine("Player3", "50", 1.0F, new ArrayList<>(), Component.literal("Player3")));
        } else if (raw != null) {
            header = raw.header;
            for (ScoreLine line : raw.lines) {
                lines.add(line);
            }
        } else {
            return null;
        }

        List<ColoredSegment> headerSegments = parseComponent(
                raw != null ? raw.headerComponent : Component.literal(header), 0xFFFFFF);
        float headerWidth = coloredTextWidth(TEXT_FONT, headerSegments, HEADER_TEXT_SIZE * scale);
        width = Math.max(width, headerWidth + 30.0F * scale);

        for (ScoreLine line : lines) {
            List<ColoredSegment> nameSegments = parseComponent(line.nameComponent, 0xFFFFFF);
            float nameWidth = coloredTextWidth(TEXT_FONT, nameSegments, TEXT_SIZE * scale);
            float scoreWidth = Render2D.textWidth(TEXT_FONT, line.score, TEXT_SIZE * scale);
            width = Math.max(width, nameWidth + scoreWidth + 25.0F * scale);
        }

        float contentHeight = (HEADER_HEIGHT + lines.size() * LINE_HEIGHT + PANEL_PADDING * 2.0F) * scale;
        size(width, contentHeight);
        if (drag.locked()) {
            positionToVanilla(drag.width());
        }

        return new ScoreBoardState(headerSegments, lines, panelAlpha, drag.x(), drag.y(), drag.width(), drag.height(), scale);
    }

    private void renderScoreboard(ScoreBoardState state) {
        float a = state.alpha;
        float s = state.scale;

        renderBackground(state.x, state.y, state.width, state.height, a);

        float headerTextX = state.x + (state.width - coloredTextWidth(TEXT_FONT, state.headerSegments, HEADER_TEXT_SIZE * s)) * 0.5F;
        renderColoredText(TEXT_FONT, state.headerSegments, headerTextX, state.y + PANEL_PADDING * s, HEADER_TEXT_SIZE * s, a);

        float separatorY = state.y + PANEL_PADDING * s + HEADER_HEIGHT * s - 2.0F * s;
        Render2D.rect(state.x + PANEL_PADDING * s, separatorY, state.width - PANEL_PADDING * 2.0F * s, 0.15F * s, ColorUtil.rgba(255, 255, 255, Math.round(60.0F * a)));

        float textY = state.y + PANEL_PADDING * s + HEADER_HEIGHT * s;
        for (ScoreLine line : state.lines) {
            float nameAlpha = clamp(a * line.alpha, 0.0F, 1.0F);
            if (nameAlpha <= 0.01F) {
                continue;
            }
            renderColoredText(TEXT_FONT, line.nameSegments, state.x + PANEL_PADDING * s, textY, TEXT_SIZE * s, nameAlpha);
            int scoreColor = ColorUtil.withAlpha(ClickGuiModule.getInstance().getColor(), Math.round(230.0F * nameAlpha));
            float scoreWidth = Render2D.textWidth(TEXT_FONT, line.score, TEXT_SIZE * s);
            Render2D.text(TEXT_FONT, line.score, state.x + state.width - PANEL_PADDING * s - scoreWidth, textY, TEXT_SIZE * s, scoreColor);
            textY += LINE_HEIGHT * s;
        }
    }

    private void renderBackground(float x, float y, float w, float h, float a) {
        switch (backgroundMode) {
            case BG_NONE -> {
            }
            case BG_GLASS -> {
                int glassColor = ColorUtil.rgba(255, 255, 255, Math.round(255.0F * a));
                Render2D.liquidGlass(x, y, w, h, 2.0F, 0.075F, 4.0F, glassColor);
                Render2D.rect(x, y, w, h, 4.0F, ColorUtil.rgba(0, 0, 0, Math.round(75.0F * a)));
            }
            case BG_BLUR -> {
                HudRenderCompat.background(x, y, w, h, 4.0F, 12.0F, 1.0F, ColorUtil.rgba(0, 0, 0, Math.round(255.0F * a)));
            }
            case BG_NORMAL -> {
                HudRenderCompat.background(x, y, w, h, 4.0F, 12.0F, 1.0F, ColorUtil.rgba(0, 0, 0, Math.round(255.0F * a)));
            }
        }
    }

    private void renderPopup() {
        popupAnimation.update();
        popupAnimation.run(1.0, 0.2, Easings.CUBIC_OUT, true);
        float pa = popupAnimation.get();
        if (pa <= 0.01F) return;

        float sx = drag.x();
        float sy = drag.y();
        float popupX = sx - POPUP_WIDTH - 5.0F;
        if (popupX < 0) popupX = sx + drag.width() + 5.0F;
        float popupY = sy;
        float rows = sizeEnabled ? 3.0F : 2.0F;
        float popupH = rows * POPUP_ROW_HEIGHT + POPUP_PADDING * 2.0F;
        popupY = popupY + (drag.height() - popupH) * 0.5F;

        int bgColor = ColorUtil.rgba(15, 15, 15, Math.round(230.0F * pa));
        int borderColor = ColorUtil.withAlpha(ClickGuiModule.getInstance().getColor(), Math.round(40.0F * pa));
        int textColor = ColorUtil.rgba(255, 255, 255, Math.round(220.0F * pa));
        int valueColor = ColorUtil.withAlpha(ClickGuiModule.getInstance().getColor(), Math.round(220.0F * pa));
        int mutedColor = ColorUtil.rgba(180, 180, 180, Math.round(150.0F * pa));

        Render2D.rect(popupX - 1, popupY - 1, POPUP_WIDTH + 2, popupH + 2, 6.0F, borderColor);
        Render2D.rect(popupX, popupY, POPUP_WIDTH, popupH, 5.0F, bgColor);

        float rowY = popupY + POPUP_PADDING;

        Render2D.text(TEXT_FONT, "Фон:", popupX + POPUP_PADDING, rowY + 1.0F, 5.5F, textColor);
        float btnX = popupX + 28.0F;
        float btnW = 65.0F;
        float btnH = 8.0F;
        int btnBg = ColorUtil.rgba(40, 40, 45, Math.round(255.0F * pa));
        Render2D.rect(btnX, rowY, btnW, btnH, 3.0F, btnBg);
        String bgName = BG_NAMES[backgroundMode];
        float bgTextW = Render2D.textWidth(TEXT_FONT, bgName, 5.5F);
        Render2D.text(TEXT_FONT, bgName, btnX + (btnW - bgTextW) * 0.5F, rowY + 0.5F, 5.5F, valueColor);
        float arrowX = btnX + btnW - 8.0F;
        Render2D.text(TEXT_FONT, ">", arrowX, rowY + 0.5F, 5.5F, mutedColor);

        rowY += POPUP_ROW_HEIGHT;

        Render2D.text(TEXT_FONT, "Размер:", popupX + POPUP_PADDING, rowY + 1.0F, 5.5F, textColor);
        toggleAnimation.update();
        toggleAnimation.run(sizeEnabled ? 1.0F : 0.0F, 0.2F, Easings.CUBIC_OUT, true);
        float toggleProgress = clamp(toggleAnimation.get(), 0.0F, 1.0F);
        float toggleX = popupX + 48.0F;
        float toggleY = rowY + (TOGGLE_HEIGHT - 8.0F) * 0.5F;

        int toggleBg = ColorUtil.interpolateColor(
                ColorUtil.rgba(50, 50, 55, Math.round(255.0F * pa)),
                ColorUtil.withAlpha(ClickGuiModule.getInstance().getColor(), Math.round(255.0F * pa)),
                toggleProgress);
        Render2D.rect(toggleX, toggleY, TOGGLE_WIDTH, TOGGLE_HEIGHT, TOGGLE_CORNER, toggleBg);

        float circleX = toggleX + TOGGLE_PADDING
                + (TOGGLE_WIDTH - TOGGLE_PADDING * 2 - TOGGLE_CIRCLE_SIZE) * toggleProgress;
        float circleY = toggleY + (TOGGLE_HEIGHT - TOGGLE_CIRCLE_SIZE) * 0.5F;
        int circleColor = ColorUtil.rgba(255, 255, 255, Math.round(255.0F * pa));
        Render2D.rect(circleX, circleY, TOGGLE_CIRCLE_SIZE, TOGGLE_CIRCLE_SIZE,
                TOGGLE_CIRCLE_SIZE * 0.5F, circleColor);

        rowY += POPUP_ROW_HEIGHT;

        if (sizeEnabled) {
            Render2D.text(TEXT_FONT, "Масштаб:", popupX + POPUP_PADDING, rowY + 1.0F, 5.5F, textColor);
            float sliderX = popupX + 48.0F;
            float sliderY = rowY + (POPUP_ROW_HEIGHT - SLIDER_HEIGHT) * 0.5F;
            int trackBg = ColorUtil.rgba(50, 50, 55, Math.round(255.0F * pa));
            Render2D.rect(sliderX, sliderY, SLIDER_WIDTH, SLIDER_HEIGHT, SLIDER_HEIGHT * 0.5F, trackBg);

            float t = clamp((sizeMultiplier - 0.5F) / 1.5F, 0.0F, 1.0F);
            float fillW = SLIDER_WIDTH * t;
            int fillColor = ColorUtil.withAlpha(ClickGuiModule.getInstance().getColor(), Math.round(200.0F * pa));
            if (fillW > 1.0F) {
                Render2D.rect(sliderX, sliderY, fillW, SLIDER_HEIGHT, SLIDER_HEIGHT * 0.5F, fillColor);
            }
            float thumbX = sliderX + fillW - 4.0F;
            float thumbY = sliderY - 2.0F;
            int thumbColor = ColorUtil.rgba(255, 255, 255, Math.round(255.0F * pa));
            Render2D.rect(thumbX, thumbY, 8.0F, SLIDER_HEIGHT + 4.0F, 4.0F, thumbColor);

            String sizeText = String.format("%.1fx", sizeMultiplier);
            float sizeTextW = Render2D.textWidth(TEXT_FONT, sizeText, 5.0F);
            Render2D.text(TEXT_FONT, sizeText, popupX + POPUP_WIDTH - POPUP_PADDING - sizeTextW, rowY + 2.0F, 5.0F, valueColor);
        }
    }

    private boolean handlePopupClick(float mx, float my) {
        float sx = drag.x();
        float sy = drag.y();
        float popupX = sx - POPUP_WIDTH - 5.0F;
        if (popupX < 0) popupX = sx + drag.width() + 5.0F;
        float popupY = sy;
        float rows = sizeEnabled ? 3.0F : 2.0F;
        float popupH = rows * POPUP_ROW_HEIGHT + POPUP_PADDING * 2.0F;
        popupY = popupY + (drag.height() - popupH) * 0.5F;

        if (mx < popupX || mx > popupX + POPUP_WIDTH || my < popupY || my > popupY + popupH) {
            return false;
        }

        float rowY = popupY + POPUP_PADDING;

        float btnX = popupX + 28.0F;
        float btnW = 65.0F;
        float btnH = 8.0F;
        if (mx >= btnX && mx <= btnX + btnW && my >= rowY && my <= rowY + btnH) {
            backgroundMode = (backgroundMode + 1) % BG_NAMES.length;
            saveSettings();
            return true;
        }

        rowY += POPUP_ROW_HEIGHT;

        float toggleX = popupX + 48.0F;
        if (mx >= toggleX && mx <= toggleX + TOGGLE_WIDTH && my >= rowY - 1.0F && my <= rowY + TOGGLE_HEIGHT + 1.0F) {
            sizeEnabled = !sizeEnabled;
            saveSettings();
            return true;
        }

        rowY += POPUP_ROW_HEIGHT;

        if (sizeEnabled) {
            float sliderX = popupX + 48.0F;
            float sliderY = rowY + (POPUP_ROW_HEIGHT - SLIDER_HEIGHT) * 0.5F;
            if (mx >= sliderX - 4.0F && mx <= sliderX + SLIDER_WIDTH + 4.0F
                    && my >= sliderY - 4.0F && my <= sliderY + SLIDER_HEIGHT + 4.0F) {
                draggingSlider = true;
                float t = clamp((mx - sliderX) / SLIDER_WIDTH, 0.0F, 1.0F);
                sizeMultiplier = 0.5F + t * 1.5F;
                saveSettings();
                return true;
            }
        }

        return true;
    }

    private void saveSettings() {
        try {
            SavedSettings saved = new SavedSettings();
            saved.backgroundMode = backgroundMode;
            saved.sizeEnabled = sizeEnabled;
            saved.sizeMultiplier = sizeMultiplier;
            Files.createDirectories(CONFIG_FILE.getParent());
            try (Writer w = Files.newBufferedWriter(CONFIG_FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(saved, w);
            }
        } catch (IOException e) {
            System.err.println("Failed to save scoreboard config: " + e.getMessage());
        }
    }

    private void loadSettings() {
        if (!Files.exists(CONFIG_FILE)) return;
        try (Reader r = Files.newBufferedReader(CONFIG_FILE, StandardCharsets.UTF_8)) {
            SavedSettings saved = GSON.fromJson(r, SavedSettings.class);
            if (saved != null) {
                backgroundMode = Math.max(0, Math.min(saved.backgroundMode, BG_NAMES.length - 1));
                sizeEnabled = saved.sizeEnabled;
                sizeMultiplier = clamp(saved.sizeMultiplier, 0.5F, 2.0F);
            }
        } catch (Exception e) {
            System.err.println("Failed to load scoreboard config: " + e.getMessage());
        }
    }

    private ScoreboardStateRaw collectScoreboard() {
        if (mc.level == null || mc.player == null) {
            return null;
        }

        Scoreboard scoreboard = mc.level.getScoreboard();
        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (objective == null) {
            return null;
        }

        Component headerComponent = objective.getDisplayName();
        String header = headerComponent.getString();
        if (header.isEmpty()) {
            return null;
        }

        List<ScoreLine> lines = new ArrayList<>();
        for (PlayerScoreEntry entry : scoreboard.listPlayerScores(objective)) {
            if (entry.isHidden()) {
                continue;
            }
            PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
            Component nameComponent;
            if (team != null) {
                nameComponent = PlayerTeam.formatNameForTeam(team, entry.ownerName());
            } else {
                nameComponent = entry.ownerName();
            }
            String name = nameComponent.getString();
            if (name.isEmpty()) {
                continue;
            }
            String score = String.valueOf(entry.value());
            List<ColoredSegment> nameSegments = parseComponent(nameComponent, 0xFFFFFF);
            lines.add(new ScoreLine(name, score, 1.0F, nameSegments, nameComponent));
        }

        lines.sort((a, b) -> {
            try {
                return Integer.compare(Integer.parseInt(b.score), Integer.parseInt(a.score));
            } catch (NumberFormatException e) {
                return b.score.compareTo(a.score);
            }
        });

        if (lines.isEmpty() && header.isEmpty()) {
            return null;
        }

        return new ScoreboardStateRaw(header, lines, headerComponent);
    }

    private record ScoreLine(String name, String score, float alpha, List<ColoredSegment> nameSegments, Component nameComponent) {
    }

    private record ScoreBoardState(List<ColoredSegment> headerSegments, List<ScoreLine> lines, float alpha,
                                   float x, float y, float width, float height, float scale) {
    }

    private record ScoreboardStateRaw(String header, List<ScoreLine> lines, Component headerComponent) {
    }

    private static final class SavedSettings {
        int backgroundMode = 1;
        boolean sizeEnabled = false;
        float sizeMultiplier = 1.0f;
    }
}
