package dile.ru.api.module.impl.player;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import dile.ru.api.events.annotation.SubscribeEvent;
import dile.ru.api.events.impl.TickEvent;
import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;
import dile.ru.api.module.impl.combat.aura.util.StopWatch;
import dile.ru.api.settings.impl.MultiModeSetting;
import dile.ru.api.settings.impl.NumberSetting;
import dile.ru.utils.chat.ChatMessage;
import dile.ru.utils.inventory.InventoryTask;
import dile.ru.utils.inventory.interaction.PlayerInteractionHelper;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class KitFarmer extends Module {
    private static final String[] KITS = {
            "Tiger", "Rabbit", "Dragon", "Winner", "Delta",
            "Sponsor", "Universal", "Cobra", "Slime", "SnowMan", "Fame"
    };

    private static final String[] WHAT_TO_TAKE = {
            "Шары", "Зелья", "Броня незер",
            "Стрелы", "Дерево", "Обсидиан",
            "Кристаллы", "Фейерверки",
            "Элитры", "Тотемы зачарованные",
            "Тотемы обычные",
            "Эндер-Жемчуг",
            "Золотые яблока",
            "Зачарованные золотые яблока"
    };

    private enum State {
        IDLE,
        REQUESTING_KIT,
        WAITING_FOR_ITEMS,
        OPENING_CHEST,
        SORTING_ITEMS,
        CLOSING_CHEST,
        COOLDOWN
    }

    private final MultiModeSetting selectedKits = register(new MultiModeSetting("Kits", "Kits to farm.", KITS));
    private final MultiModeSetting whatToTake = register(new MultiModeSetting("What To Take", "Items to keep in chest.", WHAT_TO_TAKE));
    private final NumberSetting kitDelay = register(new NumberSetting("Kit Delay", "Seconds between kits.", 10.0, 1.0, 60.0, 1.0));
    private final NumberSetting hourDelay = register(new NumberSetting("Hour Delay", "Minutes between full cycles.", 60.0, 1.0, 120.0, 1.0));

    private final StopWatch stateWatch = new StopWatch();
    private final StopWatch hourWatch = new StopWatch();

    private State currentState = State.IDLE;
    private int kitIndex = 0;
    private List<String> activeKits;

    public KitFarmer() {
        super("Kit Farmer", "Automatically farms kits and sorts items into chests.", ModuleCategory.PLAYER);
    }

    @Override
    protected void onEnable() {
        currentState = State.IDLE;
        kitIndex = 0;
        hourWatch.reset();
        ChatMessage.brandmessage("Kit Farmer enabled.");
    }

    @Override
    protected void onDisable() {
        currentState = State.IDLE;
        ChatMessage.brandmessage("Kit Farmer disabled.");
    }

    @SubscribeEvent
    private void onTick(TickEvent.Post event) {
        Minecraft client = event.getClient();
        if (client.player == null || client.level == null || client.gameMode == null) {
            return;
        }

        Set<String> selected = selectedKits.getValue();
        if (selected == null || selected.isEmpty()) {
            return;
        }

        switch (currentState) {
            case IDLE -> handleIdle(client);
            case REQUESTING_KIT -> handleRequestingKit(client);
            case WAITING_FOR_ITEMS -> handleWaitingForItems(client);
            case OPENING_CHEST -> handleOpeningChest(client);
            case SORTING_ITEMS -> handleSortingItems(client);
            case CLOSING_CHEST -> handleClosingChest(client);
            case COOLDOWN -> handleCooldown(client);
        }
    }

    private void handleIdle(Minecraft client) {
        if (hourWatch.elapsedTime() < hourDelay.getValue() * 60_000) {
            return;
        }

        activeKits = selectedKits.getValue().stream().toList();
        if (activeKits.isEmpty()) {
            return;
        }

        kitIndex = 0;
        sendKitCommand(client, activeKits.get(kitIndex));
        currentState = State.REQUESTING_KIT;
        stateWatch.reset();
    }

    private void handleRequestingKit(Minecraft client) {
        if (stateWatch.elapsedTime() >= 3000) {
            currentState = State.WAITING_FOR_ITEMS;
            stateWatch.reset();
        }
    }

    private void handleWaitingForItems(Minecraft client) {
        if (stateWatch.elapsedTime() >= 2000) {
            currentState = State.OPENING_CHEST;
            stateWatch.reset();
        }
    }

    private void handleOpeningChest(Minecraft client) {
        if (client.player.containerMenu instanceof ChestMenu) {
            currentState = State.SORTING_ITEMS;
            stateWatch.reset();
            return;
        }

        BlockPos chestPos = findNearestChest(client);
        if (chestPos == null) {
            ChatMessage.brandmessage("No chest found nearby.");
            skipToNextKit();
            return;
        }

        openChest(client, chestPos);
    }

    private void handleSortingItems(Minecraft client) {
        if (!(client.player.containerMenu instanceof ChestMenu menu)) {
            currentState = State.CLOSING_CHEST;
            stateWatch.reset();
            return;
        }

        Set<String> takeItems = whatToTake.getValue();
        int chestSlots = menu.getRowCount() * 9;

        for (int i = 9; i < 45; i++) {
            if (i >= client.player.containerMenu.slots.size()) {
                break;
            }

            Slot invSlot = client.player.containerMenu.getSlot(i);
            if (!invSlot.hasItem()) {
                continue;
            }

            ItemStack stack = invSlot.getItem();
            String name = getCleanName(stack);

            if (takeItems != null && !takeItems.isEmpty() && shouldTake(name, takeItems)) {
                continue;
            }

            for (int chestSlot = 0; chestSlot < chestSlots; chestSlot++) {
                Slot targetSlot = menu.slots.get(chestSlot);
                if (!targetSlot.hasItem() || targetSlot.getItem().getItem() == stack.getItem()) {
                    InventoryTask.clickSlot(i, chestSlot, ClickType.SWAP, false);
                    break;
                }
            }
        }

        for (int i = 9; i < 45; i++) {
            if (i >= client.player.containerMenu.slots.size()) {
                break;
            }

            Slot invSlot = client.player.containerMenu.getSlot(i);
            if (!invSlot.hasItem()) {
                continue;
            }

            ItemStack stack = invSlot.getItem();
            String name = getCleanName(stack);

            if (takeItems != null && !takeItems.isEmpty() && shouldTake(name, takeItems)) {
                continue;
            }

            InventoryTask.clickSlot(i, 1, ClickType.THROW, false);
        }

        currentState = State.CLOSING_CHEST;
        stateWatch.reset();
    }

    private void handleClosingChest(Minecraft client) {
        if (stateWatch.elapsedTime() >= 500) {
            InventoryTask.closeScreen(false);
            currentState = State.COOLDOWN;
            stateWatch.reset();
        }
    }

    private void handleCooldown(Minecraft client) {
        if (stateWatch.elapsedTime() >= kitDelay.getValue() * 1000) {
            skipToNextKit();
        }
    }

    private void skipToNextKit() {
        kitIndex++;
        if (activeKits != null && kitIndex < activeKits.size()) {
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                sendKitCommand(client, activeKits.get(kitIndex));
            }
            currentState = State.REQUESTING_KIT;
            stateWatch.reset();
        } else {
            currentState = State.IDLE;
            hourWatch.reset();
            ChatMessage.brandmessage("All kits done. Waiting for next cycle.");
        }
    }

    private void sendKitCommand(Minecraft client, String kitName) {
        String cmd = "kit " + kitName.toLowerCase();
        client.player.connection.send(new ServerboundChatCommandPacket(cmd));
        ChatMessage.brandmessage("Requesting kit: " + kitName);
    }

    private BlockPos findNearestChest(Minecraft client) {
        BlockPos playerPos = client.player.blockPosition();
        List<BlockPos> nearby = PlayerInteractionHelper.getCube(playerPos, 5.0f, 3.0f);

        return nearby.stream()
                .filter(pos -> {
                    BlockState state = client.level.getBlockState(pos);
                    return state.getBlock() instanceof ChestBlock;
                })
                .min(Comparator.comparingDouble(pos -> client.player.distanceToSqr(
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)))
                .orElse(null);
    }

    private void openChest(Minecraft client, BlockPos pos) {
        BlockState state = client.level.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock)) {
            return;
        }

        client.gameMode.useItemOn(
                client.player,
                net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(
                        new net.minecraft.world.phys.Vec3(
                                pos.getX() + 0.5,
                                pos.getY() + 0.5,
                                pos.getZ() + 0.5
                        ),
                        net.minecraft.core.Direction.UP,
                        pos,
                        false
                )
        );
    }

    private boolean shouldTake(String itemName, Set<String> takeItems) {
        for (String take : takeItems) {
            if (itemName.contains(take.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private String getCleanName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }

        Component name = stack.getHoverName();
        if (name == null) {
            return "";
        }

        return name.getString().toLowerCase()
                .replaceAll("(?i)§[0-9A-FK-OR]", "")
                .trim();
    }
}
