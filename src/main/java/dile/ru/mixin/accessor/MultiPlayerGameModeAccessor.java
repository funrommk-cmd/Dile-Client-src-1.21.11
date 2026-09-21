package dile.ru.mixin.accessor;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MultiPlayerGameMode.class)
public interface MultiPlayerGameModeAccessor {
    @Accessor("isDestroying")
    void dile$setDestroying(boolean isDestroying);

    @Accessor("isDestroying")
    boolean dile$isDestroying();

    @Accessor("destroyBlockPos")
    BlockPos dile$getDestroyBlockPos();

    @Invoker("ensureHasSentCarriedItem")
    void dile$ensureHasSentCarriedItem();

    @Accessor("destroyDelay")
    void dile$setDestroyDelay(int destroyDelay);

    @Accessor("destroyProgress")
    float dile$getDestroyProgress();

    @Accessor("destroyProgress")
    void dile$setDestroyProgress(float destroyProgress);
}
