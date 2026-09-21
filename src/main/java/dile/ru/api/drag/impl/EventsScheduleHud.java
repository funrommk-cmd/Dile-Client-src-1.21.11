package dile.ru.api.drag.impl;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import dile.ru.api.module.impl.visual.ClickGuiModule;
import dile.ru.utils.render.color.ColorUtil;
import dile.ru.utils.render.ui.Render2D;
import dile.ru.utils.render.ui.font.FontType;

public final class EventsScheduleHud extends HudPanel {
    private static final float HEADER_HEIGHT = 15.0F;
    private static final float ROW_HEIGHT = 11.0F;
    private static final float PAD_X = 4.5F;
    private static final float PANEL_RADIUS = 5.0F;
    private static final float BORDER_THICKNESS = 1.0F;
    private static final float GLOW_ALPHA_SCALE = 0.55F;
    private static final float GLOW_SIZE = 10.0F;

    private String airDropTime = "";
    private String mascotTime = "";
    private String chestTime = "";

    public EventsScheduleHud() {
        super("eventsschedule", "EventsSchedule", 150.0F, 220.0F, 88.0F, 50.0F);
    }

    @Override
    public void render() {
        boolean visible = mc.player != null;
        float alpha = contentAlpha(visible);
        if (alpha <= 0.0F) return;

        updateSchedule();

        float width = 88.0F;
        float itemSpacing = ROW_HEIGHT;
        float height = HEADER_HEIGHT + 3.0F * itemSpacing + 5.0F;
        size(width, height);

        float x = drag.x();
        float y = drag.y();
        int themeColor = ClickGuiModule.getInstance().getColor();
        int bgColor = ColorUtil.rgba(30, 25, 40, Math.round(255.0F * alpha));
        int headerBg = ColorUtil.rgba(30, 25, 40, Math.round(240.0F * alpha));
        int outlineColor = ColorUtil.rgba(30, 25, 40, Math.round(120.0F * alpha));
        int glowColor = ColorUtil.multAlpha(themeColor, GLOW_ALPHA_SCALE * alpha);
        int textColor = ColorUtil.rgba(255, 255, 255, Math.round(220.0F * alpha));
        int timeColor = ColorUtil.multAlpha(themeColor, alpha);
        int headerTextColor = ColorUtil.multAlpha(themeColor, alpha);
        int brightAccent = ColorUtil.lerpColor(themeColor, ColorUtil.rgba(255, 255, 255, 255), 0.3F);
        int headerGradientEnd = ColorUtil.multAlpha(brightAccent, alpha);

        int blurBg = ColorUtil.rgba(0, 0, 0, Math.round(255.0F * alpha * 0.45F));
        int blurHeader = ColorUtil.rgba(0, 0, 0, Math.round(255.0F * alpha));

        HudRenderCompat.background(Render2D.blurBuilder()
                .rectangle(x, y, width, height)
                .radius(PANEL_RADIUS, PANEL_RADIUS, PANEL_RADIUS, PANEL_RADIUS)
                .blurRadius(15.0F)
                .smoothness(1.2F)
                .color(blurBg)
                .build());

        HudRenderCompat.background(Render2D.blurBuilder()
                .rectangle(x, y + HEADER_HEIGHT, width, height - HEADER_HEIGHT)
                .radius(0, 0, PANEL_RADIUS, PANEL_RADIUS)
                .blurRadius(15.0F)
                .smoothness(1.2F)
                .color(blurBg)
                .build());

        HudRenderCompat.background(Render2D.blurBuilder()
                .rectangle(x, y, width, HEADER_HEIGHT)
                .radius(PANEL_RADIUS, PANEL_RADIUS, 0.0F, 0.0F)
                .blurRadius(15.0F)
                .smoothness(1.2F)
                .color(blurHeader)
                .build());

        Render2D.text(TEXT_FONT, "Events", x + PAD_X, y + (HEADER_HEIGHT - 7.0F) * 0.5F - 0.5F, 7.0F, headerTextColor);
        Render2D.text(FontType.ICONS_NURIK, "Q", x + width - PAD_X - 10.0F, y + 4.5F, 8.0F, headerGradientEnd);

        float baseY = y + HEADER_HEIGHT + 4.0F;
        int separatorColor = ColorUtil.multAlpha(themeColor, 0.3F * alpha);

        Render2D.rect(x + PAD_X, baseY - 2.0F, width - PAD_X * 2, 0.5F, 0.5F, separatorColor);

        String[] names = {"AirDrop", "Mascot", "Chest"};
        String[] times = {airDropTime, mascotTime, chestTime};
        for (int i = 0; i < names.length; i++) {
            float rowY = baseY + (float) i * itemSpacing;
            Render2D.text(TEXT_FONT, names[i], x + PAD_X, rowY + 1.0F, 6.0F, textColor);
            float timeW = Render2D.textWidth(TEXT_FONT, times[i], 6.0F);
            Render2D.text(TEXT_FONT, times[i], x + width - PAD_X - timeW, rowY + 1.0F, 6.0F, timeColor);
        }
    }

    private void updateSchedule() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Europe/Moscow"));

        var airDropSchedule = java.util.Arrays.asList(
                now.withHour(9).withMinute(0).withSecond(0),
                now.withHour(11).withMinute(0).withSecond(0),
                now.withHour(13).withMinute(0).withSecond(0),
                now.withHour(15).withMinute(0).withSecond(0),
                now.withHour(17).withMinute(0).withSecond(0),
                now.withHour(19).withMinute(0).withSecond(0),
                now.withHour(21).withMinute(0).withSecond(0),
                now.withHour(23).withMinute(0).withSecond(0)
        );
        LocalDateTime nextAirDrop = airDropSchedule.stream()
                .filter(t -> t.isAfter(now))
                .findFirst()
                .orElse(now.plusDays(1).withHour(9).withMinute(0).withSecond(0));
        airDropTime = formatTimeUntil(now, nextAirDrop);

        LocalDateTime mascotT = now.withHour(15).withMinute(30).withSecond(0);
        if (now.isAfter(mascotT)) mascotT = mascotT.plusDays(1);
        mascotTime = formatTimeUntil(now, mascotT);

        LocalDateTime chestT = now.withHour(now.getHour() / 6 * 6).withMinute(0).withSecond(0);
        if (!now.isBefore(chestT)) chestT = chestT.plusHours(6);
        chestTime = formatTimeUntil(now, chestT);
    }

    private String formatTimeUntil(LocalDateTime now, LocalDateTime target) {
        long hours = ChronoUnit.HOURS.between(now, target);
        long minutes = ChronoUnit.MINUTES.between(now, target) % 60;
        long seconds = ChronoUnit.SECONDS.between(now, target) % 60;
        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0 || hours > 0) sb.append(minutes).append("m ");
        sb.append(seconds).append("s");
        return sb.toString().trim();
    }
}
