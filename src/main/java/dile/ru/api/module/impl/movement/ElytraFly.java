package dile.ru.api.module.impl.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.ClickType;
import dile.ru.api.events.annotation.SubscribeEvent;
import dile.ru.api.events.impl.TickEvent;
import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;
import dile.ru.api.settings.impl.NumberSetting;
import dile.ru.utils.inventory.lookup.InventoryUtils;

public final class ElytraFly extends Module {
    private final NumberSetting swapDelay = register(new NumberSetting("Swap Delay", "Ticks between equip cycles.", 2.0, 0.0, 10.0, 1.0));

    private int tickCounter;

    public ElytraFly() {
        super("Elytra Fly", "Fast elytra flight via rapid equip/firework cycles.", ModuleCategory.MOVEMENT);
    }

    @Override
    protected void onEnable() {
        tickCounter = 0;
    }

    @Override
    protected void onDisable() {
        tickCounter = 0;
    }

    @SubscribeEvent
    private void onTick(TickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (mc.screen != null) return;

        tickCounter++;
        if (tickCounter < swapDelay.getValue().intValue()) return;
        tickCounter = 0;

        if (!isElytraEquipped()) {
            equipElytra(mc);
            return;
        }

        if (!mc.player.isFallFlying()) {
            mc.options.keyJump.setDown(true);
            return;
        }

        useFirework(mc);
        unequipElytra(mc);
    }

    private void equipElytra(Minecraft mc) {
        int elytraSlot = findElytraSlot(mc);
        if (elytraSlot == -1) return;

        swapToChestSlot(mc, elytraSlot);
    }

    private void unequipElytra(Minecraft mc) {
        int chestSlot = findChestplateSlot(mc);
        if (chestSlot == -1) return;

        swapToChestSlot(mc, chestSlot);
    }

    private void swapToChestSlot(Minecraft mc, int inventorySlot) {
        int wrappedSlot = InventoryUtils.wrapSlot(inventorySlot);
        mc.gameMode.handleInventoryMouseClick(
                mc.player.containerMenu.containerId,
                wrappedSlot, 6, ClickType.SWAP, mc.player
        );
    }

    private void useFirework(Minecraft mc) {
        if (mc.player.getCooldowns().isOnCooldown(Items.FIREWORK_ROCKET.getDefaultInstance())) return;

        int fireworkSlot = findFireworkSlot(mc);
        if (fireworkSlot == -1) return;

        int prevSlot = mc.player.getInventory().getSelectedSlot();
        int hotbarSlot = fireworkSlot < 9 ? fireworkSlot : -1;

        if (hotbarSlot == -1) {
            mc.gameMode.handleInventoryMouseClick(
                    mc.player.containerMenu.containerId,
                    InventoryUtils.wrapSlot(fireworkSlot),
                    prevSlot,
                    ClickType.SWAP, mc.player
            );
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            mc.player.swing(InteractionHand.MAIN_HAND);
            mc.gameMode.handleInventoryMouseClick(
                    mc.player.containerMenu.containerId,
                    InventoryUtils.wrapSlot(fireworkSlot),
                    prevSlot,
                    ClickType.SWAP, mc.player
            );
        } else {
            if (hotbarSlot != prevSlot) {
                mc.getConnection().send(new ServerboundSetCarriedItemPacket(hotbarSlot));
            }
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            mc.player.swing(InteractionHand.MAIN_HAND);
            if (hotbarSlot != prevSlot) {
                mc.getConnection().send(new ServerboundSetCarriedItemPacket(prevSlot));
            }
        }
    }

    private boolean isElytraEquipped() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        return mc.player.getItemBySlot(EquipmentSlot.CHEST).getItem() == Items.ELYTRA;
    }

    private int findElytraSlot(Minecraft mc) {
        for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
            if (mc.player.getInventory().getItem(i).getItem() == Items.ELYTRA) {
                return i;
            }
        }
        return -1;
    }

    private int findChestplateSlot(Minecraft mc) {
        for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.is(ItemTags.CHEST_ARMOR) && stack.getItem() != Items.ELYTRA) {
                return i;
            }
        }
        return -1;
    }

    private int findFireworkSlot(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).getItem() == Items.FIREWORK_ROCKET) {
                return i;
            }
        }
        for (int i = 9; i < mc.player.getInventory().getContainerSize(); i++) {
            if (mc.player.getInventory().getItem(i).getItem() == Items.FIREWORK_ROCKET) {
                return i;
            }
        }
        return -1;
    }
}
