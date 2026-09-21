package dile.ru.mixin.accessor;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Accessor("jumping")
    boolean dile$isJumping();

    @Accessor("noJumpDelay")
    int dile$getJumpingCooldown();

    @Accessor("noJumpDelay")
    void dile$setJumpingCooldown(int value);
}
