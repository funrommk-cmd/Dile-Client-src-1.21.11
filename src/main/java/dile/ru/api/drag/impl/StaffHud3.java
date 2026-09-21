package dile.ru.api.drag.impl;

import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import dile.ru.api.module.impl.visual.ClickGuiModule;
import dile.ru.utils.render.animation.Easings;
import dile.ru.utils.render.animation.SmoothAnimation;
import dile.ru.utils.render.color.ColorUtil;
import dile.ru.utils.render.ui.Render2D;
import dile.ru.utils.render.ui.font.FontType;
import dile.ru.utils.repository.staff.StaffUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StaffHud3 extends HudPanel {
    private static final float HEADER_H = 15.0F;
    private static final float ITEM_SPACING = 11.0F;
    private static final float PANEL_RADIUS = 5.0F;
    private static final float MIN_WIDTH = 60.0F;
    private static final float ICON_SIZE = 8.0F;
    private static final float TITLE_SIZE = 7.0F;
    private static final float ITEM_TEXT_SIZE = 6.0F;
    private static final float ANIM_S = 0.24F;
    private static final float ROW_ANIM = 0.22F;

    private static final int BG_COL = ColorUtil.rgba(30, 25, 40, 255);
    private static final int HEADER_COL = ColorUtil.rgba(30, 25, 40, 255);
    private static final int BORDER_COL = ColorUtil.rgba(120, 80, 160, 255);
    private static final int GLOW_COL = ColorUtil.rgba(120, 80, 160, 100);

    private final List<RowEntry> rowEntries = new ArrayList<>();
    private final List<StaffEntry> cachedStaffEntries = new ArrayList<>();
    private final List<RowState> rowStates = new ArrayList<>();
    private final Map<String, PlayerInfo> tabPlayers = new HashMap<>();
    private final Map<String, Player> worldPlayers = new HashMap<>();
    private final SmoothAnimation panelAlpha = new SmoothAnimation();
    private final SmoothAnimation heightAnim = new SmoothAnimation();
    private final SmoothAnimation iconAlphaAnimation = new SmoothAnimation();
    private boolean iconAlphaForward = true;
    private static final Comparator<RowState> ROW_STATE_COMPARATOR = Comparator.comparingDouble(RowState::offset);

    public StaffHud3() {
        super("staff3", "Staff", 140.0F, 70.0F, MIN_WIDTH, HEADER_H + ITEM_SPACING);
    }

    @Override
    public void render() {
        StaffState state = logics();
        if (state == null) {
            return;
        }
        renderStaff(state);
    }

    private StaffState logics() {
        List<StaffEntry> entries = staffEntries();
        boolean preview = entries.isEmpty() && editPreview();
        boolean targetVisible = !entries.isEmpty() || preview;

        panelAlpha.update();
        panelAlpha.run(targetVisible ? 1.0 : 0.0, ANIM_S, targetVisible ? Easings.EXPO_OUT : Easings.EXPO_IN, true);
        float alpha = panelAlpha.get();

        iconAlphaAnimation.update();
        if (!iconAlphaAnimation.isAlive()) {
            iconAlphaAnimation.run(iconAlphaForward ? 1.0 : 0.0, 1.55, Easings.EXPO_IN_OUT);
            iconAlphaForward = !iconAlphaForward;
        }

        for (RowEntry entry : rowEntries) {
            entry.active = false;
            entry.alpha.update();
            entry.y.update();
        }

        int targetRows = 0;
        if (preview) {
            RowEntry entry = row("__preview", new StaffEntry("Moderator", "Active", defaultSkin("Moderator"), ColorUtil.rgba(85, 255, 140, 235)), 0.0F);
            entry.active = true;
            entry.alpha.run(1.0, ROW_ANIM, Easings.EXPO_OUT, true);
            entry.y.run(0.0F, ROW_ANIM, Easings.EXPO_OUT, true);
            targetRows = 1;
        } else {
            for (StaffEntry staff : entries) {
                float targetY = targetRows * ITEM_SPACING;
                RowEntry entry = row(staff.name.toLowerCase(), staff, targetY);
                entry.active = true;
                entry.alpha.run(1.0, ROW_ANIM, Easings.EXPO_OUT, true);
                entry.y.run(targetY, ROW_ANIM, Easings.EXPO_OUT, true);
                targetRows++;
            }
        }

        for (RowEntry entry : rowEntries) {
            if (!entry.active) {
                entry.alpha.run(0.0, ROW_ANIM, Easings.EXPO_IN, true);
            }
        }
        rowEntries.removeIf(entry -> !entry.active && entry.alpha.get() <= 0.01F && !entry.alpha.isAlive());

        float targetHeight = Math.max(20.0F, HEADER_H + Math.max(1, targetRows) * ITEM_SPACING);
        heightAnim.update();
        heightAnim.run(targetHeight, ANIM_S, Easings.CUBIC_OUT, true);
        float animatedH = heightAnim.get();

        float width = MIN_WIDTH;
        for (RowEntry entry : rowEntries) {
            if (entry.alpha.get() > 0.01F || entry.active) {
                float nameW = Render2D.textWidth(TEXT_FONT, entry.name, ITEM_TEXT_SIZE);
                width = Math.max(width, nameW + 46.0F);
            }
        }

        size(width, animatedH + 2.3F);

        boolean visible = targetVisible || alpha > 0.01F || !rowEntries.isEmpty();
        contentVisible(visible);
        if (!visible) return null;

        rowStates.clear();
        for (RowEntry entry : rowEntries) {
            float a = entry.alpha.get();
            if (a > 0.01F || entry.active) {
                rowStates.add(new RowState(entry.name, entry.status, entry.skin, entry.dotColor, entry.y.get(), a));
            }
        }
        if (rowStates.size() > 1) {
            rowStates.sort(ROW_STATE_COMPARATOR);
        }

        return new StaffState(rowStates, iconAlphaAnimation.get(), alpha, drag.x(), drag.y(), drag.width(), drag.height());
    }

    private void renderStaff(StaffState state) {
        int themeCol = ClickGuiModule.getInstance().getColor();
        float alpha = state.alpha;
        int bgA = Math.round(255.0F * alpha);
        int titleCol = ColorUtil.withAlpha(themeCol, bgA);
        int iconCol = ColorUtil.withAlpha(themeCol, bgA);

        float x = state.x;
        float y = state.y;
        float w = state.width;
        float h = state.height;

        int blurBg = ColorUtil.rgba(0, 0, 0, Math.round(bgA * 0.45F));
        int blurHeader = ColorUtil.rgba(0, 0, 0, bgA);

        HudRenderCompat.background(Render2D.blurBuilder()
                .rectangle(x, y, w, h + 2.3F)
                .radius(PANEL_RADIUS, PANEL_RADIUS, PANEL_RADIUS, PANEL_RADIUS)
                .blurRadius(15.0F)
                .smoothness(1.2F)
                .color(blurBg)
                .build());

        HudRenderCompat.background(Render2D.blurBuilder()
                .rectangle(x, y + HEADER_H, w, h - HEADER_H + 2.3F)
                .radius(0, 0, PANEL_RADIUS, PANEL_RADIUS)
                .blurRadius(15.0F)
                .smoothness(1.2F)
                .color(blurBg)
                .build());

        HudRenderCompat.background(Render2D.blurBuilder()
                .rectangle(x, y, w, HEADER_H)
                .radius(PANEL_RADIUS, PANEL_RADIUS, 0.0F, 0.0F)
                .blurRadius(15.0F)
                .smoothness(1.2F)
                .color(blurHeader)
                .build());

        String headerText = "Staff";
        Render2D.text(TEXT_FONT, headerText, x + 4.5F, y + (HEADER_H - TITLE_SIZE) * 0.5F - 0.5F, TITLE_SIZE, titleCol);

        String iconChar = "O";
        float iconWidth = Render2D.textWidth(FontType.ICONS_NURIK, iconChar, ICON_SIZE);
        Render2D.text(FontType.ICONS_NURIK, iconChar,
                x + w - iconWidth - 4.5F,
                y + (HEADER_H - ICON_SIZE) * 0.5F + 0.1F,
                ICON_SIZE, iconCol);

        float rowY = y + HEADER_H;
        for (RowState row : state.rows) {
            float rowAlpha = alpha * row.alpha;
            if (rowAlpha <= 0.01F) continue;

            float currentY = rowY + row.offset;
            float headSize = 7.0F;
            float headX = x + 2.5F;
            float headY = currentY + 2.0F;
            float avatarOffset = 12.0F;
            float textX = x + avatarOffset;
            float statusSize = 5.0F;
            float statusX = x + w - statusSize - 2.5F;
            float statusY = currentY + 3.0F;
            float separatorX = statusX - 2.0F;
            float separatorY = currentY + 3.0F;
            float separatorH = 5.0F;

            int textCol = ColorUtil.rgba(255, 255, 255, Math.round(255.0F * rowAlpha));

            renderHead(row, headX, headY, headSize, rowAlpha);

            int sepCol = ColorUtil.rgba(128, 128, 128, Math.round(128.0F * rowAlpha));
            Render2D.rect(separatorX, separatorY, 0.5F, separatorH, sepCol);

            String visibleName = trimToWidth(row.name, TEXT_FONT, ITEM_TEXT_SIZE, statusX - textX - 5.0F);
            Render2D.text(TEXT_FONT, visibleName, textX, currentY + 4.0F, ITEM_TEXT_SIZE, textCol);

            Render2D.rect(statusX, statusY, statusSize, statusSize, 1.5F, ColorUtil.multAlpha(row.dotColor, rowAlpha));
        }
    }

    private void renderHead(RowState row, float x, float y, float size, float alpha) {
        int color = ColorUtil.rgba(255, 255, 255, Math.round(255.0F * alpha));
        String texture = row.skin == null || row.skin.isBlank() ? defaultSkin(row.name) : row.skin;
        boolean base = renderSkinPart(texture, x, y, size, 8.0F / 64.0F, 8.0F / 64.0F, 16.0F / 64.0F, 16.0F / 64.0F, color);
        boolean overlay = renderSkinPart(texture, x, y, size, 40.0F / 64.0F, 8.0F / 64.0F, 48.0F / 64.0F, 16.0F / 64.0F, color);
        if (base || overlay) {
            return;
        }
        Render2D.image(texture, x, y, size, 2.0F, color);
    }

    private boolean renderSkinPart(String texture, float x, float y, float size, float u0, float v0, float u1, float v1, int color) {
        if (texture == null || texture.isBlank() || color >>> 24 == 0) {
            return false;
        }
        Render2D.imageUvNearest(texture, x, y, size, size, 2.0F, 1.0F, u0, v0, u1, v1, color);
        return true;
    }

    private RowEntry row(String key, StaffEntry staff, float targetY) {
        for (RowEntry entry : rowEntries) {
            if (entry.key.equals(key)) {
                entry.name = staff.name;
                entry.status = staff.status;
                entry.skin = staff.skin;
                entry.dotColor = staff.dotColor;
                return entry;
            }
        }
        RowEntry entry = new RowEntry(key, staff);
        entry.alpha.set(0.0);
        entry.y.set(targetY + 4.0F);
        rowEntries.add(entry);
        return entry;
    }

    private List<StaffEntry> staffEntries() {
        cachedStaffEntries.clear();
        if (mc.level == null || mc.getConnection() == null) {
            return cachedStaffEntries;
        }

        tabPlayers.clear();
        for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
            tabPlayers.put(StaffUtils.normalizeName(info.getProfile().name()).toLowerCase(), info);
        }

        worldPlayers.clear();
        for (Player player : mc.level.players()) {
            worldPlayers.put(StaffUtils.normalizeName(player.getName().getString()).toLowerCase(), player);
        }

        for (String staff : StaffUtils.getStaffNames()) {
            String key = StaffUtils.normalizeName(staff).toLowerCase();
            if (key.isEmpty()) {
                continue;
            }

            Player player = worldPlayers.get(key);
            PlayerInfo info = tabPlayers.get(key);
            if (player != null) {
                cachedStaffEntries.add(new StaffEntry(staff, "Near", skin(player, info, staff), ColorUtil.rgba(255, 198, 76, 235)));
            } else if (info != null) {
                cachedStaffEntries.add(new StaffEntry(staff, "Active", skin(null, info, staff), ColorUtil.rgba(85, 255, 140, 235)));
            } else {
                cachedStaffEntries.add(new StaffEntry(staff, "Spec", defaultSkin(staff), ColorUtil.rgba(255, 92, 92, 235)));
            }
        }
        return cachedStaffEntries;
    }

    private String skin(Player player, PlayerInfo info, String name) {
        try {
            if (player instanceof AbstractClientPlayer clientPlayer) {
                return clientPlayer.getSkin().body().texturePath().toString();
            }
            if (info != null) {
                return info.getSkin().body().texturePath().toString();
            }
        } catch (RuntimeException ignored) {
        }
        return defaultSkin(name);
    }

    private String defaultSkin(String name) {
        return ((name == null ? 0 : name.hashCode()) & 1) == 0
                ? "minecraft:textures/entity/player/wide/steve.png"
                : "minecraft:textures/entity/player/slim/alex.png";
    }

    private static final class RowEntry {
        private final String key;
        private final SmoothAnimation alpha = new SmoothAnimation();
        private final SmoothAnimation y = new SmoothAnimation();
        private float xOffset = 0.0F;
        private String name;
        private String status;
        private String skin;
        private int dotColor;
        private boolean active;

        private RowEntry(String key, StaffEntry staff) {
            this.key = key;
            this.name = staff.name;
            this.status = staff.status;
            this.skin = staff.skin;
            this.dotColor = staff.dotColor;
        }
    }

    private record StaffEntry(String name, String status, String skin, int dotColor) {
    }

    private record RowState(String name, String status, String skin, int dotColor, float offset, float alpha) {
    }

    private record StaffState(List<RowState> rows, float iconAlpha, float alpha, float x, float y, float width, float height) {
    }
}
