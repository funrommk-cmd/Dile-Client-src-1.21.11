package dile.ru.mixin.accessor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {
    @Invoker("startAttack")
    boolean dile$startAttack();

    @Invoker("startUseItem")
    void dile$startUseItem();

    @Invoker("updateLevelInEngines")
    void dile$updateLevelInEngines(ClientLevel level);

    @Accessor("rightClickDelay")
    void dile$setRightClickDelay(int delay);
}
