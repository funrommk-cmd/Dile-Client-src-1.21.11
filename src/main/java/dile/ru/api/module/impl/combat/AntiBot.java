package dile.ru.api.module.impl.combat;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.phys.Vec3;
import dile.ru.api.events.annotation.SubscribeEvent;
import dile.ru.api.events.impl.PacketEvent;
import dile.ru.api.events.impl.TickEvent;
import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AntiBot extends Module {
    private static final Minecraft MC = Minecraft.getInstance();
    private static final Set<UUID> BOT_SET = new HashSet<>();
    private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static AntiBot instance;

    private final Set<UUID> suspectSet = new HashSet<>();
    private final Map<UUID, Vec3> lastPositions = new HashMap<>();
    private final Map<UUID, Long> lastMoveTimes = new HashMap<>();

    public AntiBot() {
        super("Anti Bot", "Filters fake server-side entities.", ModuleCategory.COMBAT);
        instance = this;
    }

    public static AntiBot getInstance() {
        return instance;
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null) {
            reset();
            return;
        }
        tickSuspects(client);
        tickSpeedCheck(client);
        cleanup(client);
    }

    @SubscribeEvent
    private void onPacket(PacketEvent event) {
        if (!event.isReceive() || MC.level == null || MC.player == null) {
            return;
        }
        switch (event.getPacket()) {
            case ClientboundPlayerInfoUpdatePacket packet -> {
                for (ClientboundPlayerInfoUpdatePacket.Entry entry : packet.newEntries()) {
                    checkPlayerAfterSpawn(entry);
                }
            }
            case ClientboundPlayerInfoRemovePacket packet -> packet.profileIds().forEach(uuid -> {
                suspectSet.remove(uuid);
                BOT_SET.remove(uuid);
                lastPositions.remove(uuid);
                lastMoveTimes.remove(uuid);
            });
            default -> {
            }
        }
    }

    private void checkPlayerAfterSpawn(ClientboundPlayerInfoUpdatePacket.Entry entry) {
        GameProfile profile = entry.profile();
        if (profile == null) {
            return;
        }
        UUID uuid = profile.id();
        if (BOT_SET.contains(uuid) || suspectSet.contains(uuid)) {
            return;
        }
        int latency = entry.latency();
        boolean hasProperties = profile.properties() != null && !profile.properties().isEmpty();
        boolean isInvalid = !hasProperties || latency > 1000 || latency < 0;
        if (isInvalid) {
            suspectSet.add(uuid);
        }
    }

    private void tickSuspects(Minecraft client) {
        for (UUID uuid : Set.copyOf(suspectSet)) {
            Player player = client.level.getPlayerByUUID(uuid);
            if (player != null) {
                boolean isBot = checkBot(player);
                if (isBot) {
                    BOT_SET.add(uuid);
                }
            }
            suspectSet.remove(uuid);
        }
    }

    private void tickSpeedCheck(Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }
        for (Player player : client.level.players()) {
            if (player == client.player) {
                continue;
            }
            UUID uuid = player.getUUID();
            Vec3 currentPos = new Vec3(player.getX(), player.getY(), player.getZ());
            Long lastTime = lastMoveTimes.get(uuid);
            Vec3 lastPos = lastPositions.get(uuid);
            if (lastTime != null && lastPos != null) {
                long timeDiff = System.currentTimeMillis() - lastTime;
                if (timeDiff > 0) {
                    double distance = currentPos.distanceTo(lastPos);
                    double speed = distance / (timeDiff / 1000.0);
                    if (speed > 20.0) {
                        BOT_SET.add(uuid);
                    }
                }
            }
            lastPositions.put(uuid, currentPos);
            lastMoveTimes.put(uuid, System.currentTimeMillis());
        }
    }

    private void cleanup(Minecraft client) {
        if (client.level == null) {
            return;
        }
        if (client.player != null && client.player.tickCount % 100 == 0) {
            BOT_SET.removeIf(uuid -> client.level.getPlayerByUUID(uuid) == null);
            lastPositions.keySet().removeIf(uuid -> client.level.getPlayerByUUID(uuid) == null);
            lastMoveTimes.keySet().removeIf(uuid -> client.level.getPlayerByUUID(uuid) == null);
        }
    }

    private boolean checkBot(Player entity) {
        if (entity.getHealth() <= 0) {
            return true;
        }

        int armorCount = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                armorCount++;
            }
        }
        boolean isFullArmor = armorCount == 4;

        boolean invalidUUID = !entity.getUUID().equals(
                UUID.nameUUIDFromBytes(("OfflinePlayer:" + entity.getName().getString()).getBytes()));
        boolean shortExistence = entity.tickCount < 20;
        boolean suspiciousPing = false;
            if (MC.getConnection() != null) {
                for (PlayerInfo info : MC.getConnection().getOnlinePlayers()) {
                    if (info.getProfile().id().equals(entity.getUUID()) && info.getLatency() > 1000) {
                        suspiciousPing = true;
                    }
                }
        }

        return isFullArmor || invalidUUID || shortExistence || suspiciousPing;
    }

    public boolean isBot(Player player) {
        return player != null && (BOT_SET.contains(player.getUUID()) || checkBot(player) || isBotU(player) || hasForeignUuid(player));
    }

    public static boolean shouldIgnore(Player player) {
        return instance != null && instance.isEnabled() && instance.isBot(player);
    }

    public static boolean isBot(Entity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        return BOT_SET.contains(player.getUUID());
    }

    public static boolean isKnownBot(UUID uuid) {
        return BOT_SET.contains(uuid);
    }

    public static boolean hasForeignUuid(Player player) {
        if (player == null) {
            return false;
        }
        String name = player.getName().getString();
        if (name.contains("NPC") || name.startsWith("[ZNPC]")) {
            return false;
        }
        return !player.getUUID().equals(
                UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes()));
    }

    private boolean isBotU(Entity entity) {
        String name = entity.getName().getString();
        return entity.isInvisible()
                && !name.contains("NPC")
                && !name.startsWith("[ZNPC]")
                && !entity.getUUID().equals(
                UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes()));
    }

    private void reset() {
        suspectSet.clear();
        BOT_SET.clear();
        lastPositions.clear();
        lastMoveTimes.clear();
    }

    @Override
    protected void onDisable() {
        reset();
    }
}
