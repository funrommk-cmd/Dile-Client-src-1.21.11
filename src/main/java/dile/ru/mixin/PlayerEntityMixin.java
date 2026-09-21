package dile.ru.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dile.ru.api.events.impl.PlayerTravelEvent;
import dile.ru.api.events.impl.SwimmingEvent;
import dile.ru.api.module.impl.combat.aura.Angle;
import dile.ru.api.module.impl.combat.aura.AngleConnection;
import dile.ru.manager.Manager;

@Mixin(Player.class)
public abstract class PlayerEntityMixin {
    @Unique
    private boolean dile$restoreAttackYaw;

    @Unique
    private float dile$attackYawBefore;

    @Inject(method = "attack", at = @At("HEAD"), require = 0)
    private void dile$attackYawHead(Entity target, CallbackInfo ci) {
        dile$restoreAttackYaw = false;
        if ((Object) this != Minecraft.getInstance().player) {
            return;
        }

        Angle angle = AngleConnection.INSTANCE.getMoveRotation();
        if (angle != null) {
            Player player = (Player) (Object) this;
            dile$attackYawBefore = player.getYRot();
            dile$restoreAttackYaw = true;
            player.setYRot(angle.getYaw());
        }
    }

    @Inject(method = "attack", at = @At("RETURN"), require = 0)
    private void dile$attackYawReturn(Entity target, CallbackInfo ci) {
        if (dile$restoreAttackYaw) {
            ((Player) (Object) this).setYRot(dile$attackYawBefore);
            dile$restoreAttackYaw = false;
        }
    }

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true, require = 0)
    private void dile$onTravelPre(Vec3 movementInput, CallbackInfo ci) {
        if ((Object) this != Minecraft.getInstance().player) {
            return;
        }
        PlayerTravelEvent event = Manager.postEvent(new PlayerTravelEvent(movementInput, true));
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @ModifyExpressionValue(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getLookAngle()Lnet/minecraft/world/phys/Vec3;"), require = 0)
    private Vec3 dile$travelLookAngle(Vec3 original) {
        if ((Object) this != Minecraft.getInstance().player) {
            return original;
        }
        SwimmingEvent event = Manager.postEvent(new SwimmingEvent(original));
        return event.getVector();
    }

    @Inject(method = "travel", at = @At("RETURN"), require = 0)
    private void dile$onTravelPost(Vec3 movementInput, CallbackInfo ci) {
        if ((Object) this == Minecraft.getInstance().player) {
            Manager.postEvent(new PlayerTravelEvent(movementInput, false));
        }
    }
}
