package dile.ru.api.drag.impl;

import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import dile.ru.api.drag.core.ElementScreen;
import dile.ru.utils.render.animation.Easings;
import dile.ru.api.module.impl.visual.ClickGuiModule;
import dile.ru.utils.render.animation.SmoothAnimation;
import dile.ru.utils.render.color.ColorUtil;
import dile.ru.utils.render.ui.Render2D;

import java.util.ArrayList;
import java.util.List;

public final class GlassScoreBoard extends HudPanel {
    private static final float LINE_HEIGHT = 8.0F;
    private static final float HEADER_HEIGHT = 10.0F;
    private static final float PANEL_PADDING = 5.0F;
    private static final float TEXT_SIZE = 6.0F;
    private static final float HEADER_TEXT_SIZE = 7.0F;
    private static final float SIDE_MARGIN = 2.0F;

    private static GlassScoreBoard instance;

    private final SmoothAnimation panelAnimation = new SmoothAnimation();

    public GlassScoreBoard() {
        super("glassScoreboard", "Glass ScoreBoard", 760.0F, 10.0F, 90.0F, 60.0F);
        instance = this;
        drag.locked(true);
    }

    public static GlassScoreBoard getInstance() {
        return instance;
    }

    public void resetToVanilla() {
        float width = drag.width();
        positionToVanilla(width);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event) {
        return false;
    }

    @Override
    public void render() {
        ScoreBoardState state = logics();
        if (state == null) {
            return;
        }
        renderScoreboard(state);
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

        List<ScoreLine> lines = new ArrayList<>();
        String header;
        float width = 90.0F;

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
        float headerWidth = coloredTextWidth(TEXT_FONT, headerSegments, HEADER_TEXT_SIZE);
        width = Math.max(width, headerWidth + 30.0F);

        for (ScoreLine line : lines) {
            List<ColoredSegment> nameSegments = parseComponent(line.nameComponent, 0xFFFFFF);
            float nameWidth = coloredTextWidth(TEXT_FONT, nameSegments, TEXT_SIZE);
            float scoreWidth = Render2D.textWidth(TEXT_FONT, line.score, TEXT_SIZE);
            width = Math.max(width, nameWidth + scoreWidth + 25.0F);
        }

        float contentHeight = HEADER_HEIGHT + lines.size() * LINE_HEIGHT + PANEL_PADDING * 2.0F;
        size(width, contentHeight);
        if (drag.locked()) {
            positionToVanilla(drag.width());
        }

        return new ScoreBoardState(headerSegments, lines, panelAlpha, drag.x(), drag.y(), drag.width(), drag.height());
    }

    private void renderScoreboard(ScoreBoardState state) {
        float a = state.alpha;

        renderBackground(state.x, state.y, state.width, state.height, a);

        float headerTextX = state.x + (state.width - coloredTextWidth(TEXT_FONT, state.headerSegments, HEADER_TEXT_SIZE)) * 0.5F;
        renderColoredText(TEXT_FONT, state.headerSegments, headerTextX, state.y + PANEL_PADDING, HEADER_TEXT_SIZE, a);

        float separatorY = state.y + PANEL_PADDING + HEADER_HEIGHT - 2.0F;
        Render2D.rect(state.x + PANEL_PADDING, separatorY, state.width - PANEL_PADDING * 2.0F, 0.15F, ColorUtil.rgba(255, 255, 255, Math.round(60.0F * a)));

        float textY = state.y + PANEL_PADDING + HEADER_HEIGHT;
        for (ScoreLine line : state.lines) {
            float nameAlpha = clamp(a * line.alpha, 0.0F, 1.0F);
            if (nameAlpha <= 0.01F) {
                continue;
            }
            renderColoredText(TEXT_FONT, line.nameSegments, state.x + PANEL_PADDING, textY, TEXT_SIZE, nameAlpha);
            int scoreColor = ClickGuiModule.getInstance().getColor(nameAlpha * 0.902F);
            float scoreWidth = Render2D.textWidth(TEXT_FONT, line.score, TEXT_SIZE);
            Render2D.text(TEXT_FONT, line.score, state.x + state.width - PANEL_PADDING - scoreWidth, textY, TEXT_SIZE, scoreColor);
            textY += LINE_HEIGHT;
        }
    }

    private void renderBackground(float x, float y, float w, float h, float a) {
        int glassColor = ColorUtil.rgba(255, 255, 255, Math.round(255.0F * a));
        Render2D.liquidGlass(x, y, w, h, 2.0F, 0.075F, 4.0F, glassColor);
        Render2D.rect(x, y, w, h, 4.0F, ColorUtil.rgba(0, 0, 0, Math.round(75.0F * a)));
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
                                   float x, float y, float width, float height) {
    }

    private record ScoreboardStateRaw(String header, List<ScoreLine> lines, Component headerComponent) {
    }
}
