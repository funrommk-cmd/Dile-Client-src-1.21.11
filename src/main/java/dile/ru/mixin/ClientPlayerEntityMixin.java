package dile.ru.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dile.ru.api.events.impl.CloseScreenEvent;
import dile.ru.api.events.impl.PlayerTravelEvent;
import dile.ru.api.events.impl.PushEvent;
import dile.ru.api.events.impl.UsingItemEvent;
import dile.ru.api.module.impl.combat.AuraModule;
import dile.ru.api.module.impl.combat.aura.AngleConnection;
import dile.ru.manager.Manager;
import dile.ru.utils.move.MoveUtil;

@Mixin(LocalPlayer.class)
public abstract class ClientPlayerEntityMixin {
    @Shadow
    private float yRotLast;

    @Shadow
    private float xRotLast;

    @Shadow
    @Final
    public ClientPacketListener connection;

    @Shadow
    @Final
    protected Minecraft minecraft;

    @Shadow
    public abstract boolean isUsingItem();

    @Unique
    private double dile$prevX;

    @Unique
    private double dile$prevZ;

    @Unique
    private float dile$prevBodyYaw;

    @Inject(method = "moveTowardsClosestSpace", at = @At("HEAD"), cancellable = true, require = 0)
    private void dile$pushOutOfBlocks(double x, double z, CallbackInfo ci) {
        PushEvent event = Manager.postEvent(new PushEvent(PushEvent.Type.BLOCK));
        if (event.isCancelled()) {
            ci.cancel();
        }
    }


    @Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/ClientInput;tick()V", shift = At.Shift.AFTER), require = 0)
    private void dile$onInputTick(CallbackInfo ci) {
        if (minecraft.player != null) {
            Manager.postEvent(new PlayerTravelEvent(Vec3.ZERO, false));
        }
    }

    @Redirect(method = "modifyInput", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec2;scale(F)Lnet/minecraft/world/phys/Vec2;", ordinal = 1), require = 0)
    private Vec2 dile$cancelItemSlowdown(Vec2 vec, float multiplier) {
        UsingItemEvent event = Manager.postEvent(new UsingItemEvent(UsingItemEvent.ON));
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (event.isCancelled() && isUsingItem() && !player.isPassenger()) {
            return vec.scale(1.0F);
        }
        return vec.scale(multiplier);
    }

    @Inject(method = "closeContainer", at = @At("HEAD"), cancellable = true, require = 0)
    private void dile$closeHandledScreenHook(CallbackInfo ci) {
        Screen screen = minecraft.screen;
        CloseScreenEvent event = Manager.postEvent(new CloseScreenEvent(screen));
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @ModifyExpressionValue(method = {"sendPosition", "tick"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getYRot()F"), require = 0)
    private float dile$packetYaw(float original) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        AngleConnection controller = AngleConnection.INSTANCE;
        if (!controller.shouldApplyPacketRotation()) {
            dile$syncBodyYawCache(player, original);
            return original;
        }

        float yaw = controller.getPacketYaw();
        float bodyYaw = MoveUtil.calculateBodyYaw(
                yaw,
                dile$prevBodyYaw,
                dile$prevX,
                dile$prevZ,
                player.getX(),
                player.getZ(),
                player.getAttackAnim(1.0F)
        );

        dile$prevBodyYaw = bodyYaw;
        dile$prevX = player.getX();
        dile$prevZ = player.getZ();
        player.setYBodyRot(bodyYaw);
        return yaw;
    }

    @Unique
    private void dile$syncBodyYawCache(LocalPlayer player, float yaw) {
        dile$prevBodyYaw = yaw;
        dile$prevX = player.getX();
        dile$prevZ = player.getZ();
    }

    @ModifyExpressionValue(method = {"sendPosition", "tick"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getXRot()F"), require = 0)
    private float dile$packetPitch(float original) {
        AngleConnection controller = AngleConnection.INSTANCE;
        return controller.shouldApplyPacketRotation() ? controller.getPacketPitch() : original;
    }

    @Inject(method = "sendPosition", at = @At("TAIL"))
    private void dile$ensureSilentRotationPacket(CallbackInfo ci) {
        AngleConnection controller = AngleConnection.INSTANCE;
        AuraModule aura = AuraModule.getInstance();
        boolean queuedAuraAttack = aura != null && aura.hasQueuedAttack();
        if (!controller.shouldApplyPacketRotation()) {
            if (queuedAuraAttack) {
                aura.flushQueuedAttack();
            }
            return;
        }

        LocalPlayer player = (LocalPlayer) (Object) this;
        float yaw = controller.getPacketYaw();
        float pitch = controller.getPacketPitch();
        boolean rotationChanged = Math.abs(yaw - yRotLast) > 1.0E-3F || Math.abs(pitch - xRotLast) > 1.0E-3F;
        if (rotationChanged) {
            connection.send(new ServerboundMovePlayerPacket.Rot(
                    yaw,
                    pitch,
                    player.onGround(),
                    player.horizontalCollision
            ));
            yRotLast = yaw;
            xRotLast = pitch;
        }

        if (queuedAuraAttack) {
            aura.flushQueuedAttack();
        }
    }
}
