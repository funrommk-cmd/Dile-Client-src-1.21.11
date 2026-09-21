package dile.ru.mixin.accessor;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ItemInHandRenderer.class)
public interface HeldItemRendererAccessor {
    @Invoker("renderMapHand")
    void invokeRenderMapHand(PoseStack matrices, SubmitNodeCollector nodeCollector, int light, HumanoidArm arm);

    @Invoker("renderTwoHandedMap")
    void invokeRenderTwoHandedMap(PoseStack matrices, SubmitNodeCollector nodeCollector, int light, float pitch, float equipProgress, float swingProgress);

    @Invoker("renderOneHandedMap")
    void invokeRenderOneHandedMap(PoseStack matrices, SubmitNodeCollector nodeCollector, int light, float equipProgress, HumanoidArm arm, float swingProgress, ItemStack item);

    @Invoker("applyItemArmTransform")
    void invokeApplyItemArmTransform(PoseStack matrices, HumanoidArm arm, float equipProgress);

    @Invoker("applyItemArmAttackTransform")
    void invokeApplyItemArmAttackTransform(PoseStack matrices, HumanoidArm arm, float swingProgress);

    @Invoker("renderItem")
    void invokeRenderItem(LivingEntity entity, ItemStack item, ItemDisplayContext renderMode, PoseStack matrices, SubmitNodeCollector nodeCollector, int light);
}
