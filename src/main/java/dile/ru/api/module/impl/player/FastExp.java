package dile.ru.api.module.impl.player;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;
import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;
import dile.ru.mixin.accessor.MinecraftAccessor;

public final class FastExp extends Module {
    public FastExp() {
        super("Fast Exp", "Кидает бутылочки опыта без задержки.", ModuleCategory.PLAYER);
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null) {
            return;
        }
        if (client.player.getMainHandItem().getItem() == Items.EXPERIENCE_BOTTLE
                || client.player.getOffhandItem().getItem() == Items.EXPERIENCE_BOTTLE) {
            ((MinecraftAccessor) client).dile$setRightClickDelay(0);
        }
    }
}
