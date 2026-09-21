package dile.ru.api.module.impl.player;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import org.lwjgl.glfw.GLFW;
import dile.ru.api.events.annotation.SubscribeEvent;
import dile.ru.api.events.impl.HandledScreenEvent;
import dile.ru.api.events.impl.KeyEvent;
import dile.ru.api.events.impl.PacketEvent;
import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;
import dile.ru.api.settings.bind.InputType;
import dile.ru.api.settings.bind.KeyBind;
import dile.ru.api.settings.impl.BindSetting;
import dile.ru.api.settings.impl.BooleanSetting;
import dile.ru.api.settings.impl.ColorSetting;
import dile.ru.api.settings.impl.ModeSetting;
import dile.ru.api.settings.impl.NumberSetting;
import dile.ru.api.settings.impl.StringSetting;
import dile.ru.screens.clickgui.ClickGui;
import dile.ru.screens.modernui.ClickGuiScreen;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AHHelper extends Module {
    private static final long PULSE_MS = 900L;
    private static final float PULSE_MIN = 0.35F;
    private static final float PULSE_MAX = 1.0F;
    private static final long RESCAN_DEBOUNCE_MS = 90L;
    private static final long RESCAN_INTERVAL_MS = 650L;
    private static final long STORAGE_CLICK_DELAY_MS = 60_100L;
    private static final int CONTAINER_Y_LIMIT = 168;

    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d{1,3}(?:[,.]\\d{3})+|\\d+[,.]\\d+|\\d+)");
    private static final Pattern SECTION_COLOR_PATTERN = Pattern.compile("\\u00A7[0-9A-FK-ORa-fk-or]");
    private static final Pattern AMP_COLOR_PATTERN = Pattern.compile("&[0-9A-FK-ORa-fk-or]");
    private static final Pattern BRACKET_CONTENT_PATTERN = Pattern.compile("\\([^)]*\\)|\\[[^\\]]*\\]|\\{[^}]*\\}");
    private static final Pattern AUCTION_QUERY_CLEANUP = Pattern.compile("[^\\p{L}\\p{N}\\s]+");

    private final BindSetting findFromHandBind = register(new BindSetting("Search Item", "Runs /ah search for the item in hand.", KeyBind.NONE));
    private final BooleanSetting enabledSetting = register(new BooleanSetting("Enabled", "Master toggle for Auto Buy.", false));
    private final BooleanSetting itemReListingSetting = register(new BooleanSetting("Storage Relist", "Quick-moves storage slot 52 once per minute.", false));
    private final ModeSetting buyMode = register(new ModeSetting("Mode", "Which lot to highlight.", "Both", "Cheapest", "Per Item", "Both"));
    private final NumberSetting maxPrice = register(new NumberSetting("Max Price", "Ignore lots above this price (0 = no limit).", 0.0, 0.0, 1_000_000_000.0, 10_000.0));
    private final StringSetting itemsFilter = register(new StringSetting("Items", "Only buy lots matching these names, comma separated (empty = any).", "", 256));
    private final StringSetting buyItemsSetting = register(new StringSetting("Buy Items", "Items selected in the item picker.", "", 512));
    private final BooleanSetting autoBuySetting = register(new BooleanSetting("Auto Buy", "Automatically open /ah, browse pages and buy selected items.", true));
    private final BooleanSetting useSearchSetting = register(new BooleanSetting("Use /ah Search", "Search each selected item via /ah search instead of browsing all auction pages.", true));
    private final NumberSetting maxPagesSetting = register(new NumberSetting("Max Pages", "Maximum auction pages to browse per item.", 6.0, 1.0, 30.0, 1.0));
    private final NumberSetting actionDelaySetting = register(new NumberSetting("Click Delay (ms)", "Delay between automated clicks.", 700.0, 100.0, 5000.0, 50.0));
    private final NumberSetting confirmSlotSetting = register(new NumberSetting("Confirm Slot", "Buy screen confirm slot (-1 = auto-detect by name).", -1.0, -1.0, 89.0, 1.0));
    private final NumberSetting nextPageSlotSetting = register(new NumberSetting("Next Page Slot", "Auction next page slot (-1 = auto-detect by name/arrow).", -1.0, -1.0, 89.0, 1.0));
    private final ColorSetting cheapestColor = register(new ColorSetting("Cheapest Color", "Highlight color for the lowest total price.", new Color(255, 184, 35, 210)));
    private final ColorSetting economicColor = register(new ColorSetting("Per Item Color", "Highlight color for the best price per item.", new Color(255, 64, 64, 210)));

    private Slot cheapestSlot;
    private Slot bestPerItemSlot;
    private int screenSyncId = -1;
    private long lastScanAt;
    private long lastStorageClick = -1L;
    private boolean dirty;
    private Field cachedLeftField;
    private Field cachedTopField;

    private enum BotState { IDLE, SEARCHING, SCANNING, CONFIRMING, AWAIT_RETURN }

    private static final long SEARCH_TIMEOUT_MS = 4000L;
    private static final int AUCTION_LOT_SLOTS = 44;
    private static final long BOT_ACTION_LEEWAY_MS = 50L;

    private BotState botState = BotState.IDLE;
    private final Deque<String> buyQueue = new ArrayDeque<>();
    private String currentTerm = "";
    private int scannedPages;
    private long lastBotActionAt;
    private long nextRoundAt;
    private boolean lastScreenWasClickGui;

    public AHHelper() {
        super("Auto Buy", "Highlights the cheapest auction lots and searches held item.", ModuleCategory.AUTO_BUY);
        buyItemsSetting.setVisible(false);
        enabledSetting.setVisible(false);
        enabledSetting.addListener((setting, oldValue, newValue) -> setEnabled(newValue));
        addStateListener(changedModule -> {
            if (enabledSetting.getValue() != changedModule.isEnabled()) {
                enabledSetting.setValue(changedModule.isEnabled());
            }
        });
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.gameMode == null) {
            resetState();
            return;
        }

        if (mc.screen instanceof ClickGuiScreen || mc.screen instanceof ClickGui) {
            if (!lastScreenWasClickGui) {
                lastScreenWasClickGui = true;
                if (isEnabled()) {
                    setEnabled(false);
                }
                return;
            }
            lastScreenWasClickGui = true;
            resetState();
            return;
        }
        lastScreenWasClickGui = false;

        if (autoBuySetting.getValue()) {
            if (client.screen instanceof ContainerScreen screen) {
                tickAutoBuy(screen);
                if (isAuctionScreen(screen)) {
                    handleHighlightScan(screen);
                }
            } else {
                long now = System.currentTimeMillis();
                if (now - lastBotActionAt < Math.round(actionDelaySetting.getValue()) - BOT_ACTION_LEEWAY_MS) {
                    return;
                }
                if (botState == BotState.SEARCHING) {
                    if (now - lastBotActionAt >= SEARCH_TIMEOUT_MS) {
                        debugMessage("Auto Buy: no auction opened for " + currentTerm + ", skipping");
                        nextRoundAt = now + 1500L;
                        botState = BotState.IDLE;
                        lastBotActionAt = now;
                    }
                } else if (botState == BotState.SCANNING
                        || botState == BotState.CONFIRMING
                        || botState == BotState.AWAIT_RETURN) {
                    botState = BotState.IDLE;
                    lastBotActionAt = now;
                } else if (botState == BotState.IDLE) {
                    startNextSearch(now);
                }
            }
            return;
        }

        if (!(client.screen instanceof ContainerScreen screen)) {
            resetState();
            return;
        }
        if (!isAuctionScreen(screen)) {
            resetState();
            return;
        }
        handleHighlightScan(screen);
    }

    private void handleHighlightScan(ContainerScreen screen) {
        if (itemReListingSetting.getValue()) {
            handleStorageRelisting(screen);
        }

        long now = System.currentTimeMillis();
        int syncId = screen.getMenu().containerId;
        if (syncId != screenSyncId) {
            screenSyncId = syncId;
            dirty = true;
            cheapestSlot = null;
            bestPerItemSlot = null;
        }

        if ((dirty && now - lastScanAt >= RESCAN_DEBOUNCE_MS) || now - lastScanAt >= RESCAN_INTERVAL_MS) {
            scanAuctionSlots(screen);
        }
    }

    private void tickAutoBuy(ContainerScreen screen) {
        long now = System.currentTimeMillis();
        if (now - lastBotActionAt < Math.round(actionDelaySetting.getValue()) - BOT_ACTION_LEEWAY_MS) {
            return;
        }

        boolean waitBuy = isWaitBuyScreen(screen);
        boolean auction = isAuctionScreen(screen);

        if (waitBuy) {
            handleConfirmScreen(screen, now);
            return;
        }
        if (auction) {
            handleAuctionScreen(screen, now);
            return;
        }

        switch (botState) {
            case CONFIRMING, AWAIT_RETURN, SCANNING -> {
                botState = BotState.IDLE;
                lastBotActionAt = now;
            }
            case SEARCHING -> {
                if (now - lastBotActionAt >= SEARCH_TIMEOUT_MS) {
                    debugMessage("Auto Buy: no auction opened for " + currentTerm + ", skipping");
                    nextRoundAt = now + 1500L;
                    botState = BotState.IDLE;
                    lastBotActionAt = now;
                }
            }
            default -> {
            }
        }
    }

    private void startNextSearch(long now) {
        if (buyQueue.isEmpty()) {
            if (now < nextRoundAt) {
                botState = BotState.IDLE;
                return;
            }
            refillQueue();
        }
        if (buyQueue.isEmpty()) {
            botState = BotState.IDLE;
            return;
        }
        if (!isFuntimeServer()) {
            botState = BotState.IDLE;
            return;
        }
        closeCurrentScreen();
        currentTerm = buyQueue.pollFirst();
        scannedPages = 0;
        sendAuctionCommand("ah search " + currentTerm);
        botState = BotState.SEARCHING;
        lastBotActionAt = now;
        debugMessage("Auto Buy: /ah search " + currentTerm);
    }

    private void closeCurrentScreen() {
        if (mc.player != null && mc.player.connection != null && mc.screen instanceof ContainerScreen screen) {
            mc.player.connection.send(new ServerboundContainerClosePacket(screen.getMenu().containerId));
        }
        mc.setScreen(null);
    }

    private boolean isFuntimeServer() {
        if (mc.getConnection() == null || mc.getConnection().getServerData() == null
                || mc.getConnection().getServerData().ip == null) {
            return false;
        }
        String ip = mc.getConnection().getServerData().ip.toLowerCase(Locale.ROOT);
        return ip.contains("funtime") || ip.contains("spooky");
    }

    private void refillQueue() {
        buyQueue.clear();
        buyQueue.addAll(selectedBuyItems());
    }

    private void handleAuctionScreen(ContainerScreen screen, long now) {
        if (botState == BotState.SEARCHING) {
            botState = BotState.SCANNING;
            lastBotActionAt = now;
            return;
        }
        if (botState == BotState.AWAIT_RETURN) {
            botState = BotState.IDLE;
            lastBotActionAt = now;
            return;
        }
        if (botState == BotState.CONFIRMING) {
            if (now - lastBotActionAt >= Math.round(actionDelaySetting.getValue())) {
                debugMessage("Auto Buy: lot did not open buy screen, continue browsing");
                botState = BotState.SCANNING;
                lastBotActionAt = now;
            }
            return;
        }
        if (botState != BotState.SCANNING) {
            if (botState == BotState.IDLE) {
                startNextSearch(now);
            }
            return;
        }

        if (tryBuyFromPage(screen, now)) {
            return;
        }

        if (scannedPages >= (int) Math.round(maxPagesSetting.getValue())) {
            debugMessage("Auto Buy: " + currentTerm + " not found in " + scannedPages + " pages");
            nextRoundAt = now + 1500L;
            botState = BotState.IDLE;
            lastBotActionAt = now;
            return;
        }

        int next = findNextPageSlot(screen);
        if (next < 0) {
            debugMessage("Auto Buy: no next page, " + currentTerm + " not found");
            nextRoundAt = now + 1500L;
            botState = BotState.IDLE;
            lastBotActionAt = now;
            return;
        }

        clickSlot(screen, next);
        scannedPages++;
        lastBotActionAt = now;
        debugMessage("Auto Buy: " + currentTerm + " page " + (scannedPages + 1));
    }

    private boolean tryBuyFromPage(ContainerScreen screen, long now) {
        List<Slot> slots = screen.getMenu().slots;
        Slot best = null;
        int bestPrice = Integer.MAX_VALUE;
        Slot unparsedFallback = null;

        for (int i = 0; i < Math.min(AUCTION_LOT_SLOTS, slots.size()); i++) {
            Slot slot = slots.get(i);
            if (!slot.hasItem()) {
                continue;
            }
            ItemStack stack = slot.getItem();
            if (slot.container == mc.player.getInventory() || slot.y >= CONTAINER_Y_LIMIT || isButtonSlot(stack)) {
                continue;
            }
            if (!matchesSelected(stack)) {
                continue;
            }
            int price = extractPrice(stack);
            int max = (int) Math.round(maxPrice.getValue());
            if (max > 0 && price >= 0 && price > max) {
                continue;
            }
            if (price >= 0) {
                if (price < bestPrice) {
                    bestPrice = price;
                    best = slot;
                }
            } else if (unparsedFallback == null) {
                unparsedFallback = slot;
            }
        }

        Slot target = best != null ? best : unparsedFallback;
        if (target == null) {
            return false;
        }

        clickSlot(screen, target.index);
        botState = BotState.CONFIRMING;
        lastBotActionAt = now;
        debugMessage("Auto Buy: buying " + currentTerm + " for $" + (bestPrice == Integer.MAX_VALUE ? "?" : bestPrice));
        return true;
    }

    private void handleConfirmScreen(ContainerScreen screen, long now) {
        if (botState != BotState.CONFIRMING && botState != BotState.SCANNING) {
            return;
        }
        int confirm = findConfirmSlot(screen);
        if (confirm >= 0) {
            clickSlot(screen, confirm);
            debugMessage("Auto Buy: confirm " + currentTerm);
        } else {
            debugMessage("Auto Buy: buying " + currentTerm + " (processing)");
        }
        botState = BotState.AWAIT_RETURN;
        lastBotActionAt = now;
    }

    private int findConfirmSlot(ContainerScreen screen) {
        int override = (int) Math.round(confirmSlotSetting.getValue());
        if (override >= 0) {
            List<Slot> slots = screen.getMenu().slots;
            if (override < slots.size() && slots.get(override).hasItem()) {
                return override;
            }
        }
        for (Slot slot : screen.getMenu().slots) {
            if (!slot.hasItem()) {
                continue;
            }
            String name = stripFormatting(slot.getItem().getHoverName().getString()).toLowerCase(Locale.ROOT);
            if (name.contains("подтверд")
                    || name.contains("купить")
                    || name.contains("приобрест")
                    || name.contains("согласен")
                    || name.contains("оплат")
                    || name.contains("уверен")
                    || name.contains("confirm")
                    || name.contains("buy")
                    || name.contains("yes")) {
                return slot.index;
            }
        }
        return -1;
    }

    private int findNextPageSlot(ContainerScreen screen) {
        int override = (int) Math.round(nextPageSlotSetting.getValue());
        if (override >= 0) {
            List<Slot> slots = screen.getMenu().slots;
            if (override < slots.size() && slots.get(override).hasItem()) {
                return override;
            }
        }
        for (Slot slot : screen.getMenu().slots) {
            if (!slot.hasItem()) {
                continue;
            }
            ItemStack stack = slot.getItem();
            String name = stripFormatting(stack.getHoverName().getString()).toLowerCase(Locale.ROOT);
            if (name.contains("предыд") || name.contains("назад") || name.contains("prev") || name.contains("previous")) {
                continue;
            }
            if (name.contains("следующ")
                    || name.contains("вперёд")
                    || name.contains("вперед")
                    || name.contains("далее")
                    || name.contains("больше")
                    || name.contains("next")) {
                return slot.index;
            }
        }
        for (Slot slot : screen.getMenu().slots) {
            if (!slot.hasItem()) {
                continue;
            }
            if (slot.getItem().getItem() == Items.ARROW) {
                return slot.index;
            }
        }
        return -1;
    }

    private boolean isButtonSlot(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        String name = stripFormatting(stack.getHoverName().getString()).toLowerCase(Locale.ROOT);
        return name.contains("страница")
                || name.contains("page")
                || name.contains("далее")
                || name.contains("назад")
                || name.contains("предыд")
                || name.contains("следующ")
                || name.contains("стрелка")
                || name.contains("arrow")
                || stack.getItem() == Items.NETHER_STAR;
    }

    private boolean isWaitBuyScreen(ContainerScreen screen) {
        List<Slot> slots = screen.getMenu().slots;
        if (slots.size() > 63) {
            return false;
        }
        String title = stripFormatting(screen.getTitle().getString()).toLowerCase(Locale.ROOT);
        if (title.contains("подтвер")
                || title.contains("покуп")
                || title.contains("confirm")
                || title.contains("buy")) {
            return true;
        }
        if (slots.size() != 63) {
            return false;
        }
        Slot first = slots.get(0);
        if (first == null || !first.hasItem()) {
            return false;
        }
        String path = BuiltInRegistries.ITEM.getKey(first.getItem().getItem()).getPath();
        return path.contains("glass_pane");
    }

    private void clickSlot(ContainerScreen screen, int slotIndex) {
        if (mc.gameMode == null || mc.player == null) {
            return;
        }
        mc.gameMode.handleInventoryMouseClick(screen.getMenu().containerId, slotIndex, 0, ClickType.PICKUP, mc.player);
    }

    private void sendAuctionCommand(String command) {
        if (mc.player == null || mc.player.connection == null) {
            return;
        }
        mc.player.connection.sendCommand(command);
    }

    private void debugMessage(String message) {
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal(message), true);
        }
    }

    @SubscribeEvent
    private void onPacket(PacketEvent event) {
        if (event.isReceive()
                && event.getPacket() instanceof ClientboundContainerSetSlotPacket
                && mc.screen instanceof ContainerScreen screen
                && isAuctionScreen(screen)) {
            dirty = true;
        }
    }

    @SubscribeEvent
    private void onKey(KeyEvent event) {
        if (event.action() != GLFW.GLFW_PRESS || !isBindTriggered(findFromHandBind.getValue(), event)) {
            return;
        }
        triggerSearchCommandFromHand();
    }

    @SubscribeEvent
    private void onHandledScreen(HandledScreenEvent event) {
        if (!(mc.screen instanceof ContainerScreen screen) || !isAuctionScreen(screen) || event.getGraphics() == null) {
            return;
        }

        int[] origin = resolveScreenOrigin(screen, event);
        int cheapest = pulsingColor(cheapestColor.getValue());
        int economic = pulsingColor(economicColor.getValue());

        if (cheapestSlot != null && cheapestSlot.hasItem()) {
            drawSlotHighlight(event.getGraphics(), origin[0], origin[1], cheapestSlot, cheapest);
        }

        if (bestPerItemSlot != null && bestPerItemSlot.hasItem()) {
            drawSlotHighlight(event.getGraphics(), origin[0], origin[1], bestPerItemSlot, economic);
        }
    }

    private void handleStorageRelisting(ContainerScreen screen) {
        if (mc.gameMode == null || mc.player == null) {
            return;
        }

        String title = screen.getTitle() == null ? "" : stripFormatting(screen.getTitle().getString()).toLowerCase(Locale.ROOT);
        if (!title.contains("хранилище") && !title.contains("storage")) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastStorageClick < STORAGE_CLICK_DELAY_MS) {
            return;
        }

        List<Slot> slots = screen.getMenu().slots;
        if (slots.size() <= 52) {
            return;
        }

        Slot storageSlot = slots.get(52);
        if (storageSlot == null || !storageSlot.hasItem()) {
            return;
        }

        mc.gameMode.handleInventoryMouseClick(screen.getMenu().containerId, 52, 0, ClickType.QUICK_MOVE, mc.player);
        lastStorageClick = now;
    }

    private void scanAuctionSlots(ContainerScreen screen) {
        if (mc.player == null) {
            resetState();
            return;
        }

        List<Slot> slots = screen.getMenu().slots;
        int[] totalPrice = new int[slots.size()];
        int[] stackCount = new int[slots.size()];

        String mode = buyMode.getValue();
        boolean wantCheapest = mode.equals("Cheapest") || mode.equals("Both");
        boolean wantPerItem = mode.equals("Per Item") || mode.equals("Both");
        int maxPriceValue = (int) Math.round(maxPrice.getValue());
        List<String> filters = parseFilters(itemsFilter.getValue());

        Slot minTotalSlot = null;
        int minTotal = Integer.MAX_VALUE;

        for (int i = 0; i < slots.size(); i++) {
            Slot slot = slots.get(i);
            ItemStack stack = slot.getItem();
            if (stack.isEmpty() || slot.container == mc.player.getInventory() || slot.y >= CONTAINER_Y_LIMIT) {
                totalPrice[i] = -1;
                stackCount[i] = 0;
                continue;
            }

            int price = extractPrice(stack);
            int count = Math.max(1, stack.getCount());
            if (maxPriceValue > 0 && price >= 0 && price > maxPriceValue) {
                price = -1;
            }
            if (price >= 0 && !matchesFilters(stack, filters)) {
                price = -1;
            }
            totalPrice[i] = price;
            stackCount[i] = count;

            if (wantCheapest && price >= 0 && price < minTotal) {
                minTotal = price;
                minTotalSlot = slot;
            }
        }

        bestPerItemSlot = null;
        double bestRatio = Double.POSITIVE_INFINITY;
        int tiePrice = Integer.MAX_VALUE;
        if (wantPerItem) {
            for (int i = 0; i < slots.size(); i++) {
                int price = totalPrice[i];
                if (price < 0 || stackCount[i] <= 0) {
                    continue;
                }
                Slot slot = slots.get(i);
                if (wantCheapest && slot == minTotalSlot) {
                    continue;
                }
                double ratio = (double) price / (double) stackCount[i];
                if (ratio < bestRatio - 1.0E-9 || (Math.abs(ratio - bestRatio) <= 1.0E-9 && price < tiePrice)) {
                    bestRatio = ratio;
                    tiePrice = price;
                    bestPerItemSlot = slot;
                }
            }
        }

        cheapestSlot = wantCheapest ? minTotalSlot : null;
        if (!wantPerItem) {
            bestPerItemSlot = null;
        }
        dirty = false;
        lastScanAt = System.currentTimeMillis();
    }

    private List<String> parseFilters(String raw) {
        List<String> filters = new ArrayList<>();
        if (raw != null) {
            for (String part : raw.split(",")) {
                String token = part.trim().toLowerCase(Locale.ROOT);
                if (!token.isEmpty()) {
                    filters.add(token);
                }
            }
        }
        return filters;
    }

    public Set<String> selectedBuyItems() {
        Set<String> selected = new LinkedHashSet<>();
        if (buyItemsSetting.getValue() != null) {
            for (String part : buyItemsSetting.getValue().split(",")) {
                String term = part.trim();
                if (!term.isEmpty()) {
                    selected.add(term);
                }
            }
        }
        return selected;
    }

    public boolean isBuyItemSelected(String term) {
        return term != null && selectedBuyItems().contains(term);
    }

    public void toggleBuyItem(String term) {
        if (term == null || term.isBlank()) {
            return;
        }
        Set<String> selected = selectedBuyItems();
        if (selected.contains(term)) {
            selected.remove(term);
        } else {
            selected.add(term);
        }
        buyItemsSetting.setValue(String.join(",", selected));
    }

    private boolean matchesSelected(ItemStack stack) {
        Set<String> selected = selectedBuyItems();
        if (selected.isEmpty()) {
            return false;
        }
        String name = stripFormatting(stack.getHoverName().getString()).toLowerCase(Locale.ROOT);
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        for (String term : selected) {
            String token = term.toLowerCase(Locale.ROOT);
            if (name.contains(token) || path.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesFilters(ItemStack stack, List<String> filters) {
        if (stack == null) {
            return false;
        }
        Set<String> selected = selectedBuyItems();
        if (!selected.isEmpty()) {
            if (matchesSelected(stack)) {
                return true;
            }
            if (filters.isEmpty()) {
                return false;
            }
            return textFilterMatch(stack, filters);
        }
        if (filters.isEmpty()) {
            return true;
        }
        return textFilterMatch(stack, filters);
    }

    private boolean textFilterMatch(ItemStack stack, List<String> filters) {
        String name = stripFormatting(stack.getHoverName().getString()).toLowerCase(Locale.ROOT);
        for (String filter : filters) {
            if (name.contains(filter)) {
                return true;
            }
        }
        return false;
    }

    private int extractPrice(ItemStack stack) {
        try {
            int count = Math.max(1, stack.getCount());
            Item.TooltipContext context = mc.level == null ? Item.TooltipContext.EMPTY : Item.TooltipContext.of(mc.level);
            List<Component> tooltip = stack.getTooltipLines(context, mc.player, TooltipFlag.NORMAL);
            StringBuilder combined = new StringBuilder();
            if (tooltip != null && !tooltip.isEmpty()) {
                for (Component line : tooltip) {
                    if (line != null) {
                        combined.append(stripFormatting(line.getString())).append(' ');
                    }
                }
            } else {
                combined.append(stripFormatting(stack.getHoverName().getString()));
            }
            return parsePriceFromText(combined.toString(), count);
        } catch (Throwable ignored) {
            return -1;
        }
    }

    private int parsePriceFromText(String text, int stackCount) {
        if (text == null || text.isEmpty()) {
            return -1;
        }

        String lower = text.toLowerCase(Locale.ROOT);
        Matcher matcher = NUMBER_PATTERN.matcher(text);
        int bestWeight = -1;
        long bestPrice = -1L;
        long fallbackPrice = -1L;

        while (matcher.find()) {
            int startIdx = matcher.start(1);
            int endIdx = matcher.end(1);
            int suffixIndex = skipSuffixSeparators(lower, endIdx);
            long multiplier = suffixMultiplier(lower, suffixIndex);
            long value = parseFlexibleNumber(matcher.group(1), multiplier);
            if (value <= 0L) {
                continue;
            }

            int contextStart = Math.max(0, startIdx - 28);
            int contextEnd = Math.min(lower.length(), Math.max(endIdx, suffixIndex + 2) + 28);
            String context = lower.substring(contextStart, contextEnd);

            boolean priceContext = looksLikePriceContext(context);
            boolean totalHint = context.contains("total") || context.contains("итог") || context.contains("всего") || context.contains("сумм");
            boolean perItem = context.contains("за шт") || context.contains("за 1") || context.contains("per item") || context.contains("each");
            int weight = priceContext ? 3 : 1;
            if (totalHint) {
                weight += 3;
            }
            if (perItem) {
                weight += 1;
            }

            long normalized = perItem ? value * Math.max(1, stackCount) : value;
            if (normalized <= 0L) {
                continue;
            }

            if (normalized > fallbackPrice) {
                fallbackPrice = normalized;
            }
            if (weight > bestWeight || (weight == bestWeight && normalized > bestPrice)) {
                bestWeight = weight;
                bestPrice = normalized;
            }
        }

        long resolved = bestPrice > 0L ? bestPrice : fallbackPrice;
        if (resolved <= 0L) {
            return -1;
        }
        return resolved > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) resolved;
    }

    private boolean looksLikePriceContext(String context) {
        return context.contains("price")
                || context.contains("total")
                || context.contains("buy")
                || context.contains("sell")
                || context.contains("coin")
                || context.contains("auction")
                || context.contains("ah")
                || context.contains("цена")
                || context.contains("стоим")
                || context.contains("куп")
                || context.contains("прод")
                || context.contains("монет")
                || context.contains("аукц")
                || context.contains("лот")
                || context.contains("руб")
                || context.contains("$")
                || context.contains("₽");
    }

    private long suffixMultiplier(String text, int indexAfterNumber) {
        if (indexAfterNumber >= text.length()) {
            return 1L;
        }
        char c0 = Character.toLowerCase(text.charAt(indexAfterNumber));
        char c1 = indexAfterNumber + 1 < text.length() ? Character.toLowerCase(text.charAt(indexAfterNumber + 1)) : 0;
        if (c0 == 'k' || c0 == 'к') {
            return (c1 == 'k' || c1 == 'к') ? 1_000_000L : 1_000L;
        }
        if (c0 == 'm' || c0 == 'м') {
            return 1_000_000L;
        }
        return 1L;
    }

    private int skipSuffixSeparators(String text, int index) {
        int i = index;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c) || c == ':' || c == '=' || c == '-') {
                i++;
                continue;
            }
            break;
        }
        return i;
    }

    private long parseFlexibleNumber(String source, long multiplier) {
        if (source == null || source.isBlank()) {
            return -1L;
        }

        String compact = source.trim().replace(" ", "").replace("_", "");
        if (compact.matches("\\d{1,3}([,.]\\d{3})+")) {
            long base = parseNumber(compact.replace(",", "").replace(".", ""));
            return multiplySafe(base, multiplier);
        }

        if (compact.indexOf('.') >= 0 || compact.indexOf(',') >= 0) {
            try {
                double parsed = Double.parseDouble(compact.replace(',', '.'));
                if (!(parsed > 0.0D)) {
                    return -1L;
                }
                double scaled = parsed * (double) multiplier;
                return scaled >= (double) Long.MAX_VALUE ? Long.MAX_VALUE : Math.round(scaled);
            } catch (NumberFormatException ignored) {
                return -1L;
            }
        }

        return multiplySafe(parseNumber(compact), multiplier);
    }

    private long parseNumber(String source) {
        long value = 0L;
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c >= '0' && c <= '9') {
                value = value * 10L + (c - '0');
            }
        }
        return value;
    }

    private long multiplySafe(long base, long multiplier) {
        if (base <= 0L) {
            return -1L;
        }
        if (multiplier > 1L && base >= Long.MAX_VALUE / multiplier) {
            return Long.MAX_VALUE;
        }
        return base * multiplier;
    }

    private int pulsingColor(Color base) {
        long now = System.currentTimeMillis();
        float phase = (float) (now % PULSE_MS) / (float) PULSE_MS;
        float wave = 0.5F - 0.5F * Mth.cos(phase * (float) (Math.PI * 2.0D));
        float alphaMul = Mth.clamp(PULSE_MIN + (PULSE_MAX - PULSE_MIN) * wave, 0.0F, 1.0F);
        int alpha = Mth.clamp((int) (Math.max(120, base.getAlpha()) * alphaMul), 40, 220);
        return alpha << 24 | base.getRed() << 16 | base.getGreen() << 8 | base.getBlue();
    }

    private void drawSlotHighlight(GuiGraphics graphics, int originX, int originY, Slot slot, int color) {
        int x = originX + slot.x;
        int y = originY + slot.y;
        graphics.fill(x, y, x + 16, y + 16, color);

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int borderColor = (230 << 24) | (r << 16) | (g << 8) | b;
        graphics.fill(x, y, x + 16, y + 1, borderColor);
        graphics.fill(x, y + 15, x + 16, y + 16, borderColor);
        graphics.fill(x, y, x + 1, y + 16, borderColor);
        graphics.fill(x + 15, y, x + 16, y + 16, borderColor);
    }

    private boolean isAuctionScreen(ContainerScreen screen) {
        if (mc.player == null || screen == null) {
            return false;
        }

        String title = stripFormatting(screen.getTitle().getString()).toLowerCase(Locale.ROOT);
        if (title.contains("auction") || title.contains("аукц") || title.contains("ah")) {
            return true;
        }

        int checked = 0;
        for (Slot slot : screen.getMenu().slots) {
            if (slot.container == mc.player.getInventory() || !slot.hasItem()) {
                continue;
            }
            if (checked++ >= 8) {
                break;
            }
            try {
                Item.TooltipContext context = mc.level == null ? Item.TooltipContext.EMPTY : Item.TooltipContext.of(mc.level);
                List<Component> tooltip = slot.getItem().getTooltipLines(context, mc.player, TooltipFlag.NORMAL);
                boolean hasPrice = false;
                boolean hasSeller = false;
                for (Component line : tooltip) {
                    String text = stripFormatting(line.getString()).toLowerCase(Locale.ROOT);
                    if (looksLikePriceContext(text)) {
                        hasPrice = true;
                    }
                    if (text.contains("seller") || text.contains("продавец")) {
                        hasSeller = true;
                    }
                }
                if (hasPrice && hasSeller) {
                    return true;
                }
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    private int[] resolveScreenOrigin(AbstractContainerScreen<?> screen, HandledScreenEvent event) {
        int fallbackX = (screen.width - event.getImageWidth()) / 2;
        int fallbackY = (screen.height - event.getImageHeight()) / 2;
        try {
            if (cachedLeftField == null || cachedTopField == null) {
                cachedLeftField = AbstractContainerScreen.class.getDeclaredField("leftPos");
                cachedTopField = AbstractContainerScreen.class.getDeclaredField("topPos");
                cachedLeftField.setAccessible(true);
                cachedTopField.setAccessible(true);
            }
            return new int[]{cachedLeftField.getInt(screen), cachedTopField.getInt(screen)};
        } catch (Throwable ignored) {
            return new int[]{fallbackX, fallbackY};
        }
    }

    private String stripFormatting(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String stripped = ChatFormatting.stripFormatting(text);
        return stripped == null ? text : stripped;
    }

    private void triggerSearchCommandFromHand() {
        if (mc.player == null || mc.player.connection == null) {
            return;
        }

        ItemStack hand = mc.player.getMainHandItem();
        if (hand.isEmpty()) {
            mc.player.displayClientMessage(Component.literal("Auto Buy: no item in hand"), true);
            return;
        }

        String query = sanitizeAuctionQuery(hand.getHoverName().getString());
        if (query.isBlank()) {
            query = stripFormatting(hand.getHoverName().getString());
        }
        if (query == null || query.isBlank()) {
            mc.player.displayClientMessage(Component.literal("Auto Buy: empty item name"), true);
            return;
        }

        mc.player.connection.sendCommand("ah search " + query);
        mc.player.displayClientMessage(Component.literal("Auto Buy: /ah search " + query), true);
        dirty = true;
    }

    private String sanitizeAuctionQuery(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return "";
        }
        String normalized = stripFormatting(rawName);
        normalized = SECTION_COLOR_PATTERN.matcher(normalized).replaceAll(" ");
        normalized = AMP_COLOR_PATTERN.matcher(normalized).replaceAll(" ");
        normalized = BRACKET_CONTENT_PATTERN.matcher(normalized).replaceAll(" ");
        normalized = AUCTION_QUERY_CLEANUP.matcher(normalized).replaceAll(" ");
        normalized = normalized.replace('\u00A0', ' ');
        return normalized.trim().replaceAll("\\s{2,}", " ");
    }

    private boolean isBindTriggered(KeyBind bind, KeyEvent event) {
        if (bind == null || !bind.isBound()) {
            return false;
        }
        if (bind.getType() == InputType.KEYBOARD) {
            return event.type() == InputConstants.Type.KEYSYM && event.key() == bind.getCode();
        }
        if (bind.getType() == InputType.MOUSE) {
            return event.type() == InputConstants.Type.MOUSE && event.key() == bind.getCode();
        }
        return false;
    }

    private void resetState() {
        cheapestSlot = null;
        bestPerItemSlot = null;
        screenSyncId = -1;
        lastScanAt = 0L;
        dirty = false;
        cachedLeftField = null;
        cachedTopField = null;
        botState = BotState.IDLE;
        buyQueue.clear();
        currentTerm = "";
        scannedPages = 0;
        lastBotActionAt = 0L;
    }
}
