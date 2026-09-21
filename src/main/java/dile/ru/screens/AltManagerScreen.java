package dile.ru.screens;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import dile.ru.utils.render.ui.Render2D;
import dile.ru.utils.render.ui.font.FontType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class AltManagerScreen extends Screen {
    private static final Path CONFIG_DIR = Path.of(Minecraft.getInstance().gameDirectory.getAbsolutePath(), "dile");
    private static final Path ALTS_FILE = CONFIG_DIR.resolve("alts.json");
    private static final Path ACTIVE_ALT_FILE = CONFIG_DIR.resolve("active_alt.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final float BTN_RADIUS = 6;
    private static final float BTN_HEIGHT = 26;
    private static final float BTN_GAP = 4;
    private static final float LIST_WIDTH = 240;

    private final Screen parent;
    private List<String> accounts = new ArrayList<>();
    private int selectedAccount = -1;
    private int hoveredAccount = -1;
    private boolean addingMode = false;
    private String inputBuffer = "";
    private int hoveredAction = -1;
    private int scrollOffset = 0;

    public AltManagerScreen(Screen parent) {
        super(Component.literal("Alt Manager"));
        this.parent = parent;
        loadAlts();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        Render2D.beginFrame(graphics);
        Render2D.rect(-10, -10, width + 20, height + 20, 0, 0xFF0D0D15);

        float centerX = width / 2.0f;

        Render2D.text(FontType.SEMIBOLD, "Alt Manager", centerX - Render2D.textWidth(FontType.SEMIBOLD, "Alt Manager", 10.0f) / 2.0f, 18, 10.0f, 0xFFFFFFFF);

        float listX = centerX - LIST_WIDTH / 2.0f;
        float listY = 42;
        float maxListHeight = height - 100;

        Render2D.rect(listX - 2, listY - 2, LIST_WIDTH + 4, maxListHeight + 4, BTN_RADIUS + 1, 0x22FFFFFF);

        int visibleSlots = (int) ((maxListHeight - BTN_GAP) / (BTN_HEIGHT + BTN_GAP));
        int maxScroll = Math.max(0, accounts.size() - visibleSlots);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        hoveredAccount = -1;
        for (int i = 0; i < visibleSlots && i + scrollOffset < accounts.size(); i++) {
            int accIdx = i + scrollOffset;
            float ay = listY + i * (BTN_HEIGHT + BTN_GAP);
            boolean selected = accIdx == selectedAccount;
            boolean hovered = mouseX >= listX && mouseX <= listX + LIST_WIDTH && mouseY >= ay && mouseY <= ay + BTN_HEIGHT;

            if (hovered) hoveredAccount = accIdx;

            int bg = selected ? 0xCC6C5CE7 : hovered ? 0xAA1E1E36 : 0x88141425;
            Render2D.rect(listX, ay, LIST_WIDTH, BTN_HEIGHT, BTN_RADIUS, bg);

            String name = accounts.get(accIdx);
            Render2D.text(FontType.SEMIBOLD, name, listX + 10, ay + (BTN_HEIGHT - 6) / 2.0f, 6.0f, selected ? 0xFFFFFFFF : 0xCCFFFFFF);

            float deleteX = listX + LIST_WIDTH - 22;
            boolean delHover = mouseX >= deleteX && mouseX <= deleteX + 18 && mouseY >= ay + 4 && mouseY <= ay + BTN_HEIGHT - 4;
            Render2D.rect(deleteX, ay + 4, 18, BTN_HEIGHT - 8, 4, delHover ? 0x66FF4444 : 0x33FF4444);
            Render2D.text(FontType.SEMIBOLD, "X", deleteX + 5, ay + (BTN_HEIGHT - 5) / 2.0f, 5.0f, 0xFFFF4444);
        }

        if (accounts.isEmpty()) {
            Render2D.text(FontType.REGULAR, "No accounts added", centerX - Render2D.textWidth(FontType.REGULAR, "No accounts added", 5.5f) / 2.0f, listY + 20, 5.5f, 0x66FFFFFF);
        }

        float bottomY = listY + maxListHeight + 10;

        if (addingMode) {
            Render2D.rect(listX, bottomY, LIST_WIDTH, BTN_HEIGHT, BTN_RADIUS, 0xFF1A1A2E);
            Render2D.rect(listX, bottomY, LIST_WIDTH, BTN_HEIGHT, BTN_RADIUS, 0x22FFFFFF);

            String displayText = inputBuffer.isEmpty() ? "Type nickname..." : inputBuffer + (System.currentTimeMillis() % 1000 < 500 ? "|" : "");
            Render2D.text(FontType.REGULAR, displayText, listX + 8, bottomY + (BTN_HEIGHT - 6) / 2.0f, 6.0f, inputBuffer.isEmpty() ? 0x55FFFFFF : 0xFFFFFFFF);

            float addBtnX = listX + LIST_WIDTH + 6;
            boolean addHover = mouseX >= addBtnX && mouseX <= addBtnX + 60 && mouseY >= bottomY && mouseY <= bottomY + BTN_HEIGHT;
            Render2D.rect(addBtnX, bottomY, 60, BTN_HEIGHT, BTN_RADIUS, addHover ? 0xFF6C5CE7 : 0xCC6C5CE7);
            Render2D.text(FontType.SEMIBOLD, "Add", addBtnX + 22, bottomY + (BTN_HEIGHT - 6) / 2.0f, 6.0f, 0xFFFFFFFF);

            float cancelX = addBtnX + 66;
            boolean cancelHover = mouseX >= cancelX && mouseX <= cancelX + 60 && mouseY >= bottomY && mouseY <= bottomY + BTN_HEIGHT;
            Render2D.rect(cancelX, bottomY, 60, BTN_HEIGHT, BTN_RADIUS, cancelHover ? 0x66FF4444 : 0x44FF4444);
            Render2D.text(FontType.SEMIBOLD, "Cancel", cancelX + 12, bottomY + (BTN_HEIGHT - 6) / 2.0f, 6.0f, 0xFFFF4444);
        } else {
            float addBtnX = centerX - 62;
            boolean addHover = mouseX >= addBtnX && mouseX <= addBtnX + 60 && mouseY >= bottomY && mouseY <= bottomY + BTN_HEIGHT;
            Render2D.rect(addBtnX, bottomY, 120, BTN_HEIGHT, BTN_RADIUS, addHover ? 0xFF6C5CE7 : 0xCC6C5CE7);
            Render2D.text(FontType.SEMIBOLD, "+ Add Account", addBtnX + 15, bottomY + (BTN_HEIGHT - 6) / 2.0f, 6.0f, 0xFFFFFFFF);
        }

        String currentName = getCurrentUsername();
        String statusText = "Current: " + currentName;
        Render2D.text(FontType.REGULAR, statusText, centerX - Render2D.textWidth(FontType.REGULAR, statusText, 5.0f) / 2.0f, height - 32, 5.0f, 0x88FFFFFF);

        boolean backHover = mouseX >= 4 && mouseX <= 74 && mouseY >= height - 28 && mouseY <= height - 4;
        Render2D.rect(4, height - 28, 70, 24, BTN_RADIUS, backHover ? 0xAA1E1E36 : 0x88141425);
        Render2D.text(FontType.SEMIBOLD, "Back", 22, height - 22, 6.0f, 0xFFFFFFFF);

        Render2D.flush();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (event.button() != 0) return super.mouseClicked(event, doubled);

        float centerX = width / 2.0f;
        float listX = centerX - LIST_WIDTH / 2.0f;
        float listY = 42;
        float maxListHeight = height - 100;
        int visibleSlots = (int) ((maxListHeight - BTN_GAP) / (BTN_HEIGHT + BTN_GAP));

        if (event.x() >= 4 && event.x() <= 74 && event.y() >= height - 28 && event.y() <= height - 4) {
            minecraft.setScreen(parent);
            return true;
        }

        for (int i = 0; i < visibleSlots && i + scrollOffset < accounts.size(); i++) {
            int accIdx = i + scrollOffset;
            float ay = listY + i * (BTN_HEIGHT + BTN_GAP);
            float deleteX = listX + LIST_WIDTH - 22;

            if (event.x() >= deleteX && event.x() <= deleteX + 18 && event.y() >= ay + 4 && event.y() <= ay + BTN_HEIGHT - 4) {
                accounts.remove(accIdx);
                if (selectedAccount >= accounts.size()) selectedAccount = accounts.size() - 1;
                saveAlts();
                return true;
            }

            if (event.x() >= listX && event.x() <= listX + LIST_WIDTH && event.y() >= ay && event.y() <= ay + BTN_HEIGHT) {
                selectedAccount = accIdx;
                applyAccount(accIdx);
                return true;
            }
        }

        float bottomY = listY + maxListHeight + 10;

        if (addingMode) {
            float addBtnX = listX + LIST_WIDTH + 6;
            if (event.x() >= addBtnX && event.x() <= addBtnX + 60 && event.y() >= bottomY && event.y() <= bottomY + BTN_HEIGHT) {
                if (!inputBuffer.isBlank()) {
                    accounts.add(inputBuffer.trim());
                    saveAlts();
                    inputBuffer = "";
                    addingMode = false;
                }
                return true;
            }
            float cancelX = addBtnX + 66;
            if (event.x() >= cancelX && event.x() <= cancelX + 60 && event.y() >= bottomY && event.y() <= bottomY + BTN_HEIGHT) {
                addingMode = false;
                inputBuffer = "";
                return true;
            }
        } else {
            float addBtnX = centerX - 62;
            if (event.x() >= addBtnX && event.x() <= addBtnX + 120 && event.y() >= bottomY && event.y() <= bottomY + BTN_HEIGHT) {
                addingMode = true;
                inputBuffer = "";
                return true;
            }
        }

        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollOffset -= (int) scrollY;
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) {
            if (addingMode) {
                addingMode = false;
                inputBuffer = "";
                return true;
            }
            minecraft.setScreen(parent);
            return true;
        }
        if (addingMode && event.key() == 259) {
            if (!inputBuffer.isEmpty()) {
                inputBuffer = inputBuffer.substring(0, inputBuffer.length() - 1);
            }
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
        if (addingMode && event.isAllowedChatCharacter()) {
            String s = event.codepointAsString();
            if (s != null && !s.isEmpty() && inputBuffer.length() + s.length() <= 16) {
                inputBuffer += s;
            }
            return true;
        }
        return super.charTyped(event);
    }

    private void applyAccount(int index) {
        if (index < 0 || index >= accounts.size()) return;
        String name = accounts.get(index);
        applyNick(name);
        saveActiveAlt(name);
    }

    private static void applyNick(String name) {
        Minecraft mc = Minecraft.getInstance();
        try {
            User currentUser = mc.getUser();
            if (currentUser != null) {
                ((dile.ru.UserDileAccess) currentUser).dile$setName(name);
            }
        } catch (Exception ignored) {}
    }

    private static void saveActiveAlt(String name) {
        try {
            Files.createDirectories(CONFIG_DIR);
            JsonObject obj = new JsonObject();
            obj.addProperty("nick", name);
            Files.writeString(ACTIVE_ALT_FILE, GSON.toJson(obj));
        } catch (Exception ignored) {}
    }

    public static void applySavedAlt() {
        try {
            if (Files.exists(ACTIVE_ALT_FILE)) {
                JsonObject obj = GSON.fromJson(Files.readString(ACTIVE_ALT_FILE), JsonObject.class);
                if (obj.has("nick")) {
                    String nick = obj.get("nick").getAsString();
                    if (nick != null && !nick.isBlank()) {
                        applyNick(nick);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private String getCurrentUsername() {
        Minecraft mc = Minecraft.getInstance();
        try {
            var user = mc.getUser();
            if (user != null && user.getName() != null) {
                return user.getName();
            }
        } catch (Exception ignored) {}
        return "Unknown";
    }

    private void loadAlts() {
        try {
            if (Files.exists(ALTS_FILE)) {
                JsonArray arr = GSON.fromJson(Files.readString(ALTS_FILE), JsonArray.class);
                accounts = new ArrayList<>();
                for (JsonElement el : arr) {
                    accounts.add(el.getAsString());
                }
            }
        } catch (Exception ignored) {}
        String current = getCurrentUsername();
        if (!current.equals("Unknown") && !accounts.contains(current)) {
            accounts.add(0, current);
        }
        if (!accounts.isEmpty() && selectedAccount < 0) {
            selectedAccount = 0;
        }
    }

    private void saveAlts() {
        try {
            Files.createDirectories(CONFIG_DIR);
            JsonArray arr = new JsonArray();
            for (String s : accounts) arr.add(s);
            Files.writeString(ALTS_FILE, GSON.toJson(arr));
        } catch (Exception ignored) {}
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
