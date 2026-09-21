package dile.ru.api.drag.impl;

import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import dile.ru.utils.repository.staff.StaffUtils;
import dile.ru.utils.render.color.ColorUtil;
import dile.ru.utils.render.ui.Render2D;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class CelkaStaff extends CelkaHudPanel {
    private static final float WIDTH = 110.0F;
    private static final float HEADER_HEIGHT = 13.0F;
    private static final float ITEM_HEIGHT = 16.0F;
    private static final float TOP_BAR_HEIGHT = 2.0F;
    private static final float PADDING = 6.0F;

    private static final Pattern NAME_PATTERN = Pattern.compile("^\\w{3,16}$");
    private static final Pattern PREFIX_PATTERN = Pattern.compile(
            ".*(mod|der|adm|help|wne|мод|хелп|помо|адм|владе|отри|таф|taf|curat|курато|dev|раз|supp|сапп|yt|ютуб).*",
            Pattern.CASE_INSENSITIVE);

    public CelkaStaff() {
        super("staff", "Staff", 10.0F, 200.0F, WIDTH, 21.0F);
    }

    @Override
    public void render() {
        float posX = drag.x();
        float posY = drag.y();

        Map<List<ColoredSegment>, String> staff = staffMap();
        float height = HEADER_HEIGHT + (staff.isEmpty() ? 20.0F : staff.size() * ITEM_HEIGHT);

        int glowStart = style(0);
        int glowEnd = style(90);

        glow(posX, posY, WIDTH, height, 10.0F, glowStart, glowEnd);
        Render2D.rect(posX, posY, WIDTH, height, ColorUtil.rgba(0, 0, 0, 220));
        Render2D.rect(posX, posY, WIDTH, TOP_BAR_HEIGHT, style(0));

        centeredText("Staff Statistics", posX + WIDTH / 2.0F, posY + 6.0F, 9.0F, ColorUtil.WHITE);

        if (staff.isEmpty()) {
            Render2D.image("dile:textures/hud/staff.png", posX + 3.0F, posY + HEADER_HEIGHT, 17.0F, 17.0F, 0.0F, -1);
            centeredText("Список пуст", posX - 4.0F + WIDTH / 2.0F, posY + HEADER_HEIGHT + 2.0F, 8.5F, ColorUtil.WHITE);
            centeredText("Персонал ещё не зашел!", posX + 7.0F + WIDTH / 2.0F, posY + 11.0F + HEADER_HEIGHT, 5.5F, ColorUtil.WHITE);
        } else {
            float yOffset = posY + HEADER_HEIGHT;
            for (Map.Entry<List<ColoredSegment>, String> entry : staff.entrySet()) {
                List<ColoredSegment> prefix = entry.getKey();
                String name = entry.getValue();
                float nameWidth = Render2D.textWidth(CELKA_FONT, name, 7.0F);
                renderColoredText(CELKA_FONT, prefix, posX - 6.0F + PADDING, yOffset + 4.0F, 7.5F, 1.0F);
                Render2D.text(CELKA_FONT, name, posX + 8.0F + WIDTH - PADDING - nameWidth - 8.0F, yOffset + 4.0F, 7.0F, ColorUtil.WHITE);
                yOffset += ITEM_HEIGHT;
            }
        }

        size(WIDTH, height);
    }

    private Map<List<ColoredSegment>, String> staffMap() {
        Map<List<ColoredSegment>, String> result = new LinkedHashMap<>();
        if (mc.level == null || mc.getConnection() == null) {
            return result;
        }

        List<PlayerInfo> online = new ArrayList<>(mc.getConnection().getOnlinePlayers());
        online.sort(Comparator.comparing(info -> info.getProfile().name().toLowerCase(Locale.ROOT)));

        for (PlayerInfo info : online) {
            String name = info.getProfile().name();
            if (name == null || !NAME_PATTERN.matcher(name).matches()) {
                continue;
            }
            Component displayName = info.getTabListDisplayName();
            if (displayName == null) {
                displayName = Component.literal(name);
            }
            if (!prefixMatches(displayName) && !StaffUtils.isStaff(name)) {
                continue;
            }
            result.put(staffPrefix(displayName, name), name);
        }
        return result;
    }

    private static List<ColoredSegment> staffPrefix(Component displayName, String name) {
        List<ColoredSegment> segments = parseComponent(displayName, ColorUtil.WHITE);
        String suffix = name;
        for (int i = segments.size() - 1; i >= 0 && !suffix.isEmpty(); i--) {
            ColoredSegment segment = segments.get(i);
            String text = segment.text();
            if (text.endsWith(suffix)) {
                segments.set(i, new ColoredSegment(text.substring(0, text.length() - suffix.length()), segment.color()));
                suffix = "";
            } else if (suffix.endsWith(text)) {
                suffix = suffix.substring(0, suffix.length() - text.length());
                segments.remove(i);
            } else {
                break;
            }
        }
        return segments;
    }

    private static boolean prefixMatches(Component prefix) {
        String value = prefix.getString().toLowerCase(Locale.ROOT);
        return PREFIX_PATTERN.matcher(value).matches();
    }
}
