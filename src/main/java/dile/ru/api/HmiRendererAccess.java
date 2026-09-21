package dile.ru.api;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

public interface HmiRendererAccess {
    void hmiRenderCustomFirstPersonItem(
            AbstractClientPlayer player, float tickDelta, float pitch,
            InteractionHand hand, HumanoidArm arm, float sideFactor,
            float swingProgress, ItemStack item, float equipProgress,
            PoseStack matrices, SubmitNodeCollector nodeCollector, int light
    );
}
