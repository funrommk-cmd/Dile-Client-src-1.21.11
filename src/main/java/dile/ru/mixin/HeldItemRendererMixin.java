package dile.ru.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dile.ru.api.HmiRendererAccess;
import dile.ru.api.events.impl.HandAnimationEvent;
import dile.ru.api.events.impl.HandOffsetEvent;
import dile.ru.api.events.impl.HeldItemUpdateEvent;
import dile.ru.api.events.impl.ItemRendererEvent;
import dile.ru.api.module.impl.visual.HoldMyItems;
import dile.ru.manager.Manager;

@Mixin(ItemInHandRenderer.class)
public abstract class HeldItemRendererMixin {
    @Shadow
    private ItemStack mainHandItem;

    @Shadow
    private ItemStack offHandItem;

    @Inject(method = "tick", at = @At("TAIL"))
    private void dile$updateHeldItems(CallbackInfo ci) {
        HeldItemUpdateEvent event = Manager.postEvent(new HeldItemUpdateEvent(this.mainHandItem, this.offHandItem));
        if (event.getMainHand() != this.mainHandItem) {
            this.mainHandItem = event.getMainHand();
        }
        if (event.getOffHand() != this.offHandItem) {
            this.offHandItem = event.getOffHand();
        }
    }

    @WrapOperation(method = "renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/player/LocalPlayer;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"), require = 0)
    private void dile$itemRenderHook(ItemInHandRenderer instance, AbstractClientPlayer player, float tickDelta, float pitch, InteractionHand hand, float swingProgress, ItemStack item, float equipProgress, PoseStack matrices, SubmitNodeCollector nodeCollector, int light, Operation<Void> original) {
        if (HoldMyItems.getInstance() != null && HoldMyItems.getInstance().isEnabled()) {
            HoldMyItems config = HoldMyItems.getInstance();
            HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
            float sideFactor = hand == InteractionHand.MAIN_HAND ? 1.0F : -1.0F;
            if (config.swapHands.getValue()) {
                arm = arm.getOpposite();
                sideFactor *= -1.0F;
            }
            float hmiSwingProgress = hand == InteractionHand.MAIN_HAND ? swingProgress : 0.0F;
            ((HmiRendererAccess)(Object)this).hmiRenderCustomFirstPersonItem(
                player, tickDelta, pitch, hand, arm, sideFactor,
                hmiSwingProgress, item, 0.0F, matrices, nodeCollector, light
            );
            return;
        }

        ItemRendererEvent event = Manager.postEvent(new ItemRendererEvent(player, item, hand));
        player = event.getPlayer();
        hand = event.getHand();
        item = event.getStack();

        if (item.isEmpty() || (player.isUsingItem() && player.getUsedItemHand() == hand)) {
            original.call(instance, player, tickDelta, pitch, hand, swingProgress, item, equipProgress, matrices, nodeCollector, light);
            return;
        }

        matrices.pushPose();

        HandOffsetEvent offsetEvent = Manager.postEvent(new HandOffsetEvent(matrices, item, hand));
        float scale = offsetEvent.getScale();
        if (scale != 1.0F) {
            matrices.scale(scale, scale, scale);
        }

        HandAnimationEvent animEvent = Manager.postEvent(new HandAnimationEvent(matrices, hand, swingProgress, equipProgress, false));
        if (animEvent.isCancelled()) {
            HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
            ItemDisplayContext displayContext = arm == HumanoidArm.RIGHT ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
            instance.renderItem(player, item, displayContext, matrices, nodeCollector, light);
            matrices.popPose();
        } else {
            matrices.popPose();
            original.call(instance, player, tickDelta, pitch, hand, swingProgress, item, equipProgress, matrices, nodeCollector, light);
        }
    }
}
