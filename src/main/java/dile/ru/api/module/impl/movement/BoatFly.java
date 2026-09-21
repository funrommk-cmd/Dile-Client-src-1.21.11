package dile.ru.api.module.impl.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;
import dile.ru.api.settings.impl.BooleanSetting;
import dile.ru.api.settings.impl.ModeSetting;
import dile.ru.api.settings.impl.NumberSetting;
import java.util.List;

public final class BoatFly extends Module {
    private final ModeSetting mode = register(new ModeSetting("Mode", "Режим работы.", "Clip", "Clip", "Fly"));
    private final NumberSetting speed = register(new NumberSetting("Speed", "Скорость движения.", 1.5, 0.1, 5.0, 0.1));
    private final NumberSetting boost = register(new NumberSetting("Boost", "Вертикальный подъём.", 0.6, 0.1, 2.0, 0.1));
    private final BooleanSetting autoBreak = register(new BooleanSetting("Auto Break", "Автоматически ломать лодку при выключении.", true));

    private static final List<net.minecraft.world.item.Item> BOAT_ITEMS = List.of(
            Items.OAK_BOAT, Items.SPRUCE_BOAT, Items.BIRCH_BOAT, Items.JUNGLE_BOAT,
            Items.ACACIA_BOAT, Items.DARK_OAK_BOAT, Items.MANGROVE_BOAT, Items.CHERRY_BOAT,
            Items.BAMBOO_RAFT
    );

    private AbstractBoat currentBoat;
    private boolean placed;

    public BoatFly() {
        super("BoatFly", "Лодка для обхода античита Grim. Клип через стены или полёт.", ModuleCategory.MOVEMENT);
    }

    @Override
    protected void onEnable() {
        currentBoat = null;
        placed = false;
    }

    @Override
    protected void onDisable() {
        if (autoBreak.getValue()) {
            breakBoat();
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && currentBoat != null && client.player.getVehicle() == currentBoat) {
            client.player.stopRiding();
        }
        currentBoat = null;
        placed = false;
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null) return;

        if (!placed) {
            tryPlaceBoat(client);
            return;
        }

        if (currentBoat == null || !currentBoat.isAlive()) {
            currentBoat = findBoat(client);
            if (currentBoat == null) return;
        }

        if (!client.player.isPassenger() || client.player.getVehicle() != currentBoat) {
            client.player.startRiding(currentBoat);
            if (!client.player.isPassenger()) return;
        }

        if (mode.is("Fly")) {
            flyBoat(client);
        } else {
            clipBoat(client);
        }
    }

    private void tryPlaceBoat(Minecraft client) {
        int boatSlot = findBoatSlot(client);
        if (boatSlot == -1) return;

        int prev = client.player.getInventory().getSelectedSlot();
        client.player.getInventory().setSelectedSlot(boatSlot);
        client.player.connection.send(new ServerboundSetCarriedItemPacket(boatSlot));

        BlockPos placePos = client.player.blockPosition();
        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(placePos), Direction.UP, placePos, false
        );
        client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hit);
        client.player.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));

        client.player.getInventory().setSelectedSlot(prev);
        client.player.connection.send(new ServerboundSetCarriedItemPacket(prev));

        placed = true;
    }

    private AbstractBoat findBoat(Minecraft client) {
        for (Entity entity : client.level.getEntities(client.player, client.player.getBoundingBox().inflate(5.0))) {
            if (entity instanceof AbstractBoat boat && boat.isAlive()) return boat;
        }
        if (client.player.getVehicle() instanceof AbstractBoat boat && boat.isAlive()) return boat;
        return null;
    }

    private void flyBoat(Minecraft client) {
        Vec3 velocity = client.player.getDeltaMovement();
        double y = velocity.y;

        if (client.options.keyJump.isDown()) y = boost.getValue();
        else if (client.options.keyShift.isDown()) y = -boost.getValue();
        else if (mode.is("Fly")) y = 0.0;

        double forward = client.player.zza;
        double strafe = client.player.xxa;
        float yaw = client.player.getYRot();

        double sin = Math.sin(Math.toRadians(yaw));
        double cos = Math.cos(Math.toRadians(yaw));

        double mx = strafe * cos - forward * sin;
        double mz = forward * cos + strafe * sin;

        double len = Math.sqrt(mx * mx + mz * mz);
        if (len > 0.0) {
            mx /= len;
            mz /= len;
        }

        double spd = speed.getValue();
        mx *= spd;
        mz *= spd;

        currentBoat.setDeltaMovement(mx, y, mz);
        client.player.setDeltaMovement(mx, y, mz);
    }

    private void clipBoat(Minecraft client) {
        Vec3 velocity = client.player.getDeltaMovement();
        double y = velocity.y;

        if (client.options.keyJump.isDown()) y = boost.getValue();
        else if (client.options.keyShift.isDown()) y = -boost.getValue();

        double forward = client.player.zza;
        double strafe = client.player.xxa;
        float yaw = client.player.getYRot();

        double sin = Math.sin(Math.toRadians(yaw));
        double cos = Math.cos(Math.toRadians(yaw));

        double mx = strafe * cos - forward * sin;
        double mz = forward * cos + strafe * sin;

        double len = Math.sqrt(mx * mx + mz * mz);
        if (len > 0.0) {
            mx /= len;
            mz /= len;
        }

        double spd = speed.getValue();
        mx *= spd;
        mz *= spd;

        currentBoat.setDeltaMovement(mx, y, mz);
        client.player.setDeltaMovement(mx, y, mz);
    }

    private void breakBoat() {
        if (currentBoat == null || !currentBoat.isAlive()) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        if (client.player.getVehicle() == currentBoat) {
            client.player.stopRiding();
        }

        currentBoat.discard();
        currentBoat = null;
        placed = false;
    }

    private int findBoatSlot(Minecraft client) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getItem(i);
            if (BOAT_ITEMS.contains(stack.getItem())) return i;
        }
        return -1;
    }
}
