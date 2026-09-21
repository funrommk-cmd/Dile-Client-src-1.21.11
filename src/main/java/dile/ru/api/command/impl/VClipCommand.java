package dile.ru.api.command.impl;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import dile.ru.IMinecraft;
import dile.ru.api.command.Command;
import dile.ru.api.command.helpers.TabCompleteHelper;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public final class VClipCommand extends Command implements IMinecraft {
    private static final double STEP = 0.25;

    public VClipCommand() {
        super("vclip", "Телепортирует вперёд на расстояние в блоках, через стены", "vc");
    }

    @Override
    public void execute(String label, String[] args) {
        if (mc.player == null || mc.level == null) {
            logDirect("Игрок недоступен.", ChatFormatting.RED);
            return;
        }

        if (args.length < 1) {
            logDirect("Использование: ." + label + " <дистанция>", ChatFormatting.RED);
            return;
        }

        double distance;
        try {
            distance = Double.parseDouble(args[0]);
        } catch (NumberFormatException e) {
            logDirect("Неверное число: " + args[0], ChatFormatting.RED);
            return;
        }

        if (distance == 0) {
            logDirect("Дистанция не может быть 0.", ChatFormatting.RED);
            return;
        }

        Vec3 pos = mc.player.position();
        float yaw = mc.player.getYRot();

        double dirX = -Math.sin(Math.toRadians(yaw));
        double dirZ = Math.cos(Math.toRadians(yaw));

        Vec3 target = findTeleportTarget(pos, dirX, dirZ, distance);

        mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
                target.x, target.y, target.z, mc.player.onGround(), mc.player.horizontalCollision
        ));
        mc.player.setPos(target.x, target.y, target.z);

        logDirect("Телепортировано на " + String.format("%.1f", distance) + " блоков вперёд.", ChatFormatting.GREEN);
    }

    private Vec3 findTeleportTarget(Vec3 start, double dirX, double dirZ, double maxDistance) {
        double traveled = 0;
        boolean insideWall = false;

        while (traveled < maxDistance) {
            double step = Math.min(STEP, maxDistance - traveled);
            double x = start.x + dirX * traveled;
            double z = start.z + dirZ * traveled;

            BlockPos blockPos = BlockPos.containing(x, start.y, z);
            BlockPos headPos = BlockPos.containing(x, start.y + 1.62, z);

            boolean feetSolid = isSolid(blockPos);
            boolean headSolid = isSolid(headPos);

            if (feetSolid || headSolid) {
                insideWall = true;
                traveled += step;
                continue;
            }

            if (insideWall) {
                return new Vec3(x, start.y, z);
            }

            traveled += step;
        }

        double finalX = start.x + dirX * maxDistance;
        double finalZ = start.z + dirZ * maxDistance;
        return new Vec3(finalX, start.y, finalZ);
    }

    private boolean isSolid(BlockPos pos) {
        BlockState state = mc.level.getBlockState(pos);
        return state.canOcclude() || state.isSolidRender();
    }

    @Override
    public Stream<String> tabComplete(String label, String[] args) {
        if (args.length == 1) {
            return new TabCompleteHelper()
                    .append("1", "2", "3", "5", "10", "50", "100")
                    .filterPrefix(args[0])
                    .stream();
        }
        return Stream.empty();
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Телепортирует игрока вперёд на указанное расстояние, через стены.",
                "Использование:",
                "> vclip <дистанция> - телепорт вперёд на N блоков, пройдя сквозь стены"
        );
    }
}
