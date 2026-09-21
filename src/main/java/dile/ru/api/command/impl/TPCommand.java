package dile.ru.api.command.impl;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import dile.ru.IMinecraft;
import dile.ru.api.command.Command;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public final class TPCommand extends Command implements IMinecraft {
    public TPCommand() {
        super("tp", "Телепортирует к указанному игроку");
    }

    @Override
    public void execute(String label, String[] args) {
        if (mc.player == null || mc.level == null) {
            logDirect("Игрок недоступен.", ChatFormatting.RED);
            return;
        }

        if (args.length < 1) {
            logDirect("Использование: ." + label + " <ник>", ChatFormatting.RED);
            return;
        }

        String targetName = args[0];
        Player target = findPlayer(targetName);

        if (target == null) {
            logDirect("Игрок " + ChatFormatting.GOLD + targetName + ChatFormatting.RED + " не найден.", ChatFormatting.RED);
            return;
        }

        double distance = mc.player.distanceTo(target);
        double targetX = target.getX();
        double targetZ = target.getZ();
        double targetY = findSolidBlockY(target);

        if (targetY == Double.MIN_VALUE) {
            logDirect("Не удалось найти твёрдый блок рядом с игроком "
                    + ChatFormatting.GOLD + targetName + ChatFormatting.RED + ".", ChatFormatting.RED);
            return;
        }

        int packetsCount = Math.max((int) (distance / 1000), 3);

        for (int i = 0; i < packetsCount; i++) {
            mc.player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(mc.player.onGround(), mc.player.horizontalCollision));
        }

        mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(targetX, targetY, targetZ, false, mc.player.horizontalCollision));
        mc.player.setPos(targetX, targetY, targetZ);

        logDirect("Телепортация к " + ChatFormatting.GOLD + targetName + ChatFormatting.GRAY + " выполнена.", ChatFormatting.GREEN);
        logDirect(String.format("Координаты: %.1f %.1f %.1f", targetX, targetY, targetZ), ChatFormatting.GRAY);
    }

    private double findSolidBlockY(Player target) {
        BlockPos targetPos = target.blockPosition();

        for (int y = targetPos.getY() - 1; y >= 0; y--) {
            BlockPos pos = new BlockPos(targetPos.getX(), y, targetPos.getZ());
            if (isSolid(pos)) {
                return y + 0.25;
            }
        }

        for (int y = targetPos.getY() + 1; y < mc.level.getHeight(); y++) {
            BlockPos pos = new BlockPos(targetPos.getX(), y, targetPos.getZ());
            if (isSolid(pos)) {
                return y + 0.25;
            }
        }

        return Double.MIN_VALUE;
    }

    private boolean isSolid(BlockPos pos) {
        if (mc.level == null) {
            return false;
        }
        BlockState state = mc.level.getBlockState(pos);
        return state != null && state.isSolid();
    }

    private Player findPlayer(String name) {
        if (mc.level == null) {
            return null;
        }

        for (Player player : mc.level.players()) {
            if (player != null && player.getGameProfile() != null
                    && player.getGameProfile().name().equalsIgnoreCase(name)) {
                return player;
            }
        }
        return null;
    }

    @Override
    public Stream<String> tabComplete(String label, String[] args) {
        if (args.length == 1 && mc.getConnection() != null) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return mc.getConnection().getOnlinePlayers().stream()
                    .map(info -> info.getProfile().name())
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .sorted();
        }
        return Stream.empty();
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Телепортирует к указанному игроку.",
                "Использование:",
                "> tp <ник> - телепортация к игроку"
        );
    }
}
