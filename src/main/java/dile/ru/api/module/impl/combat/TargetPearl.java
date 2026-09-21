package dile.ru.api.module.impl.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import dile.ru.api.events.annotation.SubscribeEvent;
import dile.ru.api.events.impl.TickEvent;
import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;
import dile.ru.api.settings.impl.ModeSetting;
import dile.ru.utils.inventory.lookup.InventoryUtils;

import java.util.HashMap;
import java.util.Map;

public final class TargetPearl extends Module {
    private static final Minecraft mc = Minecraft.getInstance();

    private static final double PEARL_DRAG = 0.99;
    private static final double PEARL_GRAVITY = 0.03;
    private static final int MAX_SIMULATION_TICKS = 100;

    private final ModeSetting priority = register(new ModeSetting("Priority", "Throw towards the Aura target.",
            "\u0417\u0430 \u0442\u0430\u0440\u0433\u0435\u0442\u043e\u043c", "\u0417\u0430 \u0442\u0430\u0440\u0433\u0435\u0442\u043e\u043c"));

    private final Map<Integer, Boolean> trackedPearls = new HashMap<>();

    public TargetPearl() {
        super("Target Pearl", "Counter-throws a pearl when the Aura target throws theirs.", ModuleCategory.COMBAT);
    }

    @Override
    protected void onDisable() {
        trackedPearls.clear();
    }

    @SubscribeEvent
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.level == null || mc.options.hideGui) {
            return;
        }

        LivingEntity target = getAuraTarget();
        if (target == null) {
            cleanTrackedPearls();
            return;
        }

        if (mc.player.getCooldowns().isOnCooldown(Items.ENDER_PEARL.getDefaultInstance())) {
            cleanTrackedPearls();
            return;
        }

        int pearlSlot = findPearl();
        if (pearlSlot == -1) {
            cleanTrackedPearls();
            return;
        }

        ThrownEnderpearl targetPearl = findNewTargetPearl(target);
        if (targetPearl == null) {
            cleanTrackedPearls();
            return;
        }

        Vec3 landingPos = predictLanding(targetPearl);
        Vec3 eyePos = mc.player.getEyePosition();
        float yaw = calculateYaw(eyePos, landingPos);
        float pitch = calculatePitch(eyePos, landingPos);

        throwPearl(pearlSlot, yaw, pitch);
        cleanTrackedPearls();
    }

    private LivingEntity getAuraTarget() {
        if (!priority.is("\u0417\u0430 \u0442\u0430\u0440\u0433\u0435\u0442\u043e\u043c")) {
            return null;
        }
        return AuraModule.target;
    }

    private ThrownEnderpearl findNewTargetPearl(LivingEntity target) {
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof ThrownEnderpearl pearl)) {
                continue;
            }
            int id = pearl.getId();
            if (trackedPearls.containsKey(id)) {
                continue;
            }

            Entity owner = pearl.getOwner();
            if (owner == null) {
                continue;
            }

            trackedPearls.put(id, Boolean.TRUE);

            if (owner.getId() == target.getId()) {
                return pearl;
            }
        }
        return null;
    }

    private void cleanTrackedPearls() {
        trackedPearls.keySet().removeIf(id -> {
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (entity.getId() == id) {
                    return false;
                }
            }
            return true;
        });
    }

    private Vec3 predictLanding(ThrownEnderpearl pearl) {
        Vec3 pos = pearl.position();
        Vec3 motion = pearl.getDeltaMovement();

        for (int i = 0; i < MAX_SIMULATION_TICKS; i++) {
            Vec3 next = pos.add(motion);
            BlockHitResult blockHit = mc.level.clip(new ClipContext(
                    pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, pearl));
            if (blockHit != null && blockHit.getType() != HitResult.Type.MISS) {
                return blockHit.getLocation();
            }
            pos = next;
            motion = motion.scale(PEARL_DRAG).add(0.0, -PEARL_GRAVITY, 0.0);
            if (pos.y < -128.0) {
                break;
            }
        }
        return pos;
    }

    private int findPearl() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).getItem() == Items.ENDER_PEARL) {
                return i;
            }
        }
        for (int i = 9; i < 36; i++) {
            if (mc.player.getInventory().getItem(i).getItem() == Items.ENDER_PEARL) {
                return i;
            }
        }
        return -1;
    }

    private void throwPearl(int slot, float yaw, float pitch) {
        float savedYaw = mc.player.getYRot();
        float savedPitch = mc.player.getXRot();

        mc.player.setYRot(yaw);
        mc.player.setXRot(pitch);
        mc.getConnection().send(new ServerboundMovePlayerPacket.Rot(
                yaw, pitch, mc.player.onGround(), mc.player.horizontalCollision));

        if (slot >= 0 && slot < 9) {
            int current = mc.player.getInventory().getSelectedSlot();
            if (slot != current) {
                InventoryUtils.sendHeldItemChange(slot);
            }
            InventoryUtils.sendUsePacket(InteractionHand.MAIN_HAND, yaw, pitch);
            if (slot != current) {
                InventoryUtils.sendHeldItemChange(current);
            }
        } else if (slot >= 9) {
            int current = mc.player.getInventory().getSelectedSlot();
            InventoryUtils.click(slot, current, ClickType.SWAP);
            InventoryUtils.sendUsePacket(InteractionHand.MAIN_HAND, yaw, pitch);
            InventoryUtils.click(slot, current, ClickType.SWAP);
        }

        mc.player.connection.send(new ServerboundMovePlayerPacket.Rot(
                savedYaw, savedPitch, mc.player.onGround(), mc.player.horizontalCollision));
        mc.player.setYRot(savedYaw);
        mc.player.setXRot(savedPitch);
    }

    private static float calculateYaw(Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        return (float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(-dx, dz)));
    }

    private static float calculatePitch(Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        double dy = to.y - from.y;
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        float pitch = (float) Mth.wrapDegrees(Math.toDegrees(-Math.atan2(dy, horizontalDist)));
        return Mth.clamp(pitch, -90.0f, 90.0f);
    }
}
