package dile.ru.api.drag.impl;

import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.effect.MobEffects;
import dile.ru.utils.render.color.ColorUtil;
import dile.ru.utils.render.ui.Render2D;
import dile.ru.utils.repository.staff.StaffUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class StaffActive4 extends HudPanel {
    private static final float OFFSET = 9.0F;
    private static final float WIDTH = 100.0F;

    private int activeStaff;

    public StaffActive4() {
        super("staffactive4", "Active Staff", 140.0F, 240.0F, WIDTH, 20.5F);
    }

    @Override
    public void render() {
        float x = drag.x();
        float y = drag.y();

        float height2 = 20.5F;
        height2 += OFFSET * activeStaff - 4.0F;
        float xStaff = x + (WIDTH - Render2D.textWidth(TEXT_FONT, "Staff Active", 6.5F)) / 2.0F;
        if (activeStaff == 0) {
            height2 -= 1.5F;
        }

        Render2D.rect(x, y, WIDTH, height2, 3.0F, ColorUtil.rgba(0, 0, 0, 195));
        Render2D.text(TEXT_FONT, "Staff Active", xStaff, y + 4.0F, 6.5F, ColorUtil.rgba(255, 255, 255, 255));

        int count = 0;
        for (StaffEntry entry : staffEntries()) {
            String name = entry.name.substring(0, Math.min(entry.name.length(), 10));
            float yText = y + 16.7F + count * OFFSET;

            Render2D.text(TEXT_FONT, name, x + 4.0F, yText, 6.0F, ColorUtil.rgba(255, 255, 255, 255));
            if (entry.spectator) {
                Render2D.text(TEXT_FONT, "Spectator", x + 63.5F, yText, 5.5F, ColorUtil.rgba(231, 52, 52, 255));
            } else if (entry.online) {
                Render2D.text(TEXT_FONT, "Online", x + 74.0F, yText, 5.5F, ColorUtil.rgba(116, 236, 114, 255));
            } else if (entry.near) {
                int circleColor = entry.nearInvisible
                        ? ColorUtil.rgba(231, 52, 52, 255)
                        : ColorUtil.rgba(116, 236, 114, 255);
                float circleSize = 5.0F;
                Render2D.rect(x + 85.3F, yText + 1.0F, circleSize, circleSize, circleSize * 0.5F, circleColor);
            }
            count++;
        }
        activeStaff = count;
        size(WIDTH, height2);
    }

    private List<StaffEntry> staffEntries() {
        List<StaffEntry> result = new ArrayList<>();
        if (mc.level == null || mc.getConnection() == null) {
            return result;
        }

        Map<String, AbstractClientPlayer> nearPlayers = new HashMap<>();
        for (AbstractClientPlayer player : mc.level.players()) {
            nearPlayers.put(StaffUtils.normalizeName(player.getName().getString()).toLowerCase(), player);
        }
        Set<String> tabNames = new HashSet<>();
        for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
            tabNames.add(StaffUtils.normalizeName(info.getProfile().name()).toLowerCase());
        }

        for (String staff : StaffUtils.getStaffNames()) {
            String key = StaffUtils.normalizeName(staff).toLowerCase();
            if (key.isEmpty()) {
                continue;
            }
            AbstractClientPlayer nearPlayer = nearPlayers.get(key);
            boolean near = nearPlayer != null;
            boolean inTab = tabNames.contains(key);
            boolean nearInvisible = near && nearPlayer.hasEffect(MobEffects.INVISIBILITY);
            result.add(new StaffEntry(staff, !inTab, inTab && !near, near, nearInvisible));
        }
        return result;
    }

    private record StaffEntry(String name, boolean spectator, boolean online, boolean near, boolean nearInvisible) {
    }
}
