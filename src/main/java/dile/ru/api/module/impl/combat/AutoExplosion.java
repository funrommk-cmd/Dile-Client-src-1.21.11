package dile.ru.api.module.impl.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;
import dile.ru.api.settings.impl.BooleanSetting;
import dile.ru.api.settings.impl.NumberSetting;
import dile.ru.utils.inventory.lookup.InventoryUtils;

public class AutoExplosion extends Module {
    private static AutoExplosion instance;

    private final NumberSetting placeDelay = register(new NumberSetting("Place Delay", "Delay before placing the crystal.", 0.0, 0.0, 5.0, 1.0));
    private final NumberSetting attackDelay = register(new NumberSetting("Attack Delay", "Delay before exploding the crystal.", 0.0, 0.0, 5.0, 1.0));
    private final NumberSetting attackRange = register(new NumberSetting("Range", "Max distance to the placed obsidian.", 6.0, 1.0, 8.0, 0.5));
    private final BooleanSetting keepItem = register(new BooleanSetting("Keep Item", "Keep the held item (obsidian) in hand after the explosion.", true));

    private BlockPos targetPos;
    private boolean needPlace;
    private int placeCooldown;
    private int attackCooldown;
    private int attackTimeout;
    private int previousCrystalSlot = -1;
    private int restoreSlot = -1;
    private int restoreDelay;
    private int pendingInventorySlot = -1;
    private int pendingHotbarSlot = -1;
    private int restoreInventorySlot = -1;
    private int restoreHotbarSlot = -1;
    private int restoreInventoryDelay;

    public AutoExplosion() {
        super("Auto Explosion", "When you place obsidian, automatically places a crystal on it and explodes it.", ModuleCategory.COMBAT);
        instance = this;
    }

    public static AutoExplosion getInstance() {
        return instance;
    }

    public static void handleObsidianPlaced(BlockPos pos) {
        AutoExplosion module = instance;
        if (module != null && module.isEnabled() && pos != null) {
            module.onObsidianPlaced(pos);
        }
    }

    private void onObsidianPlaced(BlockPos pos) {
        targetPos = pos.immutable();
        needPlace = true;
        placeCooldown = 0;
        attackCooldown = 0;
        attackTimeout = 0;
    }

    @Override
    protected void onEnable() {
        resetState();
    }

    @Override
    protected void onDisable() {
        resetState();
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null) {
            resetState();
            return;
        }

        boolean restoring = tickRestoreInventory() | tickRestoreSlot(client);
        if (targetPos == null) {
            return;
        }

        if (!isTargetValid(client)) {
            resetTarget();
            return;
        }

        if (needPlace) {
            if (restoring) {
                return;
            }
            if (placeCooldown < placeDelay.getValue()) {
                placeCooldown++;
                return;
            }
            if (placeCrystal(client)) {
                needPlace = false;
                attackCooldown = 0;
                attackTimeout = 0;
            }
            return;
        }

        if (attackCooldown < attackDelay.getValue()) {
            attackCooldown++;
            return;
        }
        if (attackCrystal(client)) {
            resetTarget();
        } else if (++attackTimeout > 40) {
            resetTarget();
        }
    }

    private boolean isTargetValid(Minecraft client) {
        BlockState state = client.level.getBlockState(targetPos);
        if (state.getBlock() != Blocks.OBSIDIAN) {
            return false;
        }
        double range = attackRange.getValue();
        return client.player.distanceToSqr(Vec3.atCenterOf(targetPos)) <= range * range;
    }

    private boolean placeCrystal(Minecraft client) {
        if (hasCrystalOn(client, targetPos)) {
            needPlace = false;
            return true;
        }

        int crystalSlot = InventoryUtils.findHotbarItem(Items.END_CRYSTAL);
        if (crystalSlot == -1) {
            int inventorySlot = InventoryUtils.findItemInInventory(Items.END_CRYSTAL);
            if (inventorySlot == -1) {
                resetTarget();
                return false;
            }
            swapCrystalFromInventory(client, inventorySlot);
            return false;
        }

        int selectedSlot = client.player.getInventory().getSelectedSlot();
        if (selectedSlot != crystalSlot) {
            previousCrystalSlot = selectedSlot;
            InventoryUtils.syncSelectedHotbarSlot(crystalSlot);
        }

        Vec3 hitVec = Vec3.atCenterOf(targetPos);
        BlockHitResult crystalTarget = new BlockHitResult(hitVec, Direction.UP, targetPos, false);
        InteractionResult result = client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, crystalTarget);
        if (result.consumesAction()) {
            client.player.swing(InteractionHand.MAIN_HAND);
            scheduleRestoreSlot();
            scheduleRestoreInventory();
            return true;
        }
        return false;
    }

    private boolean attackCrystal(Minecraft client) {
        EndCrystal crystal = findCrystalOn(client, targetPos);
        if (crystal == null) {
            return false;
        }

        client.gameMode.attack(client.player, crystal);
        client.player.swing(InteractionHand.MAIN_HAND);
        return true;
    }

    private EndCrystal findCrystalOn(Minecraft client, BlockPos basePos) {
        BlockPos crystalPos = basePos.above();
        AABB searchBox = new AABB(
                crystalPos.getX(),
                crystalPos.getY(),
                crystalPos.getZ(),
                crystalPos.getX() + 1.0D,
                crystalPos.getY() + 2.0D,
                crystalPos.getZ() + 1.0D
        ).inflate(0.1D);

        for (Entity entity : client.level.getEntities(client.player, searchBox, entity -> entity instanceof EndCrystal)) {
            if (entity.isAlive()) {
                return (EndCrystal) entity;
            }
        }
        return null;
    }

    private boolean hasCrystalOn(Minecraft client, BlockPos basePos) {
        return findCrystalOn(client, basePos) != null;
    }

    private void resetTarget() {
        cancelPendingInventorySwap();
        targetPos = null;
        needPlace = false;
        placeCooldown = 0;
        attackCooldown = 0;
        attackTimeout = 0;
    }

    private void resetState() {
        resetTarget();
        previousCrystalSlot = -1;
        restoreSlot = -1;
        restoreDelay = 0;
        pendingInventorySlot = -1;
        pendingHotbarSlot = -1;
        restoreInventorySlot = -1;
        restoreHotbarSlot = -1;
        restoreInventoryDelay = 0;
    }

    private void scheduleRestoreSlot() {
        if (!keepItem.getValue() || previousCrystalSlot == -1) {
            previousCrystalSlot = -1;
            return;
        }

        restoreSlot = previousCrystalSlot;
        restoreDelay = 2;
        previousCrystalSlot = -1;
    }

    private boolean tickRestoreSlot(Minecraft client) {
        if (restoreSlot == -1) {
            return false;
        }
        if (restoreDelay > 0) {
            restoreDelay--;
            return true;
        }

        int slot = restoreSlot;
        restoreSlot = -1;
        if (client.player.getInventory().getSelectedSlot() != slot) {
            InventoryUtils.syncSelectedHotbarSlot(slot);
        }
        return true;
    }

    private void swapCrystalFromInventory(Minecraft client, int inventorySlot) {
        if (pendingInventorySlot != -1 || restoreInventorySlot != -1) {
            return;
        }

        int selectedSlot = client.player.getInventory().getSelectedSlot();
        int wrappedSlot = InventoryUtils.wrapSlot(inventorySlot);
        pendingInventorySlot = wrappedSlot;
        pendingHotbarSlot = selectedSlot;
        InventoryUtils.click(wrappedSlot, selectedSlot, ClickType.SWAP);
    }

    private void scheduleRestoreInventory() {
        if (pendingInventorySlot == -1 || pendingHotbarSlot == -1) {
            return;
        }

        restoreInventorySlot = pendingInventorySlot;
        restoreHotbarSlot = pendingHotbarSlot;
        restoreInventoryDelay = 2;
        pendingInventorySlot = -1;
        pendingHotbarSlot = -1;
    }

    private void cancelPendingInventorySwap() {
        if (pendingInventorySlot == -1 || pendingHotbarSlot == -1 || restoreInventorySlot != -1) {
            return;
        }

        restoreInventorySlot = pendingInventorySlot;
        restoreHotbarSlot = pendingHotbarSlot;
        restoreInventoryDelay = 1;
        pendingInventorySlot = -1;
        pendingHotbarSlot = -1;
    }

    private boolean tickRestoreInventory() {
        if (restoreInventorySlot == -1 || restoreHotbarSlot == -1) {
            return false;
        }
        if (restoreInventoryDelay > 0) {
            restoreInventoryDelay--;
            return true;
        }

        int inventorySlot = restoreInventorySlot;
        int hotbarSlot = restoreHotbarSlot;
        restoreInventorySlot = -1;
        restoreHotbarSlot = -1;
        InventoryUtils.click(inventorySlot, hotbarSlot, ClickType.SWAP);
        return true;
    }
}
