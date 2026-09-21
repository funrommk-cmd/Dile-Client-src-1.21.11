package dile.ru.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dile.ru.api.module.impl.visual.ArmorDurability;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EquipmentLayerRenderer.class)
public abstract class EquipmentLayerRendererMixin {

    private static final String RENDER_LAYERS = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V";

    private static ItemStack dile$currentStack;

    @Shadow
    private static int getColorForLayer(EquipmentClientInfo.Layer layer, int color) {
        return 0;
    }

    @Inject(method = RENDER_LAYERS, at = @At("HEAD"))
    private static void dile$captureStack(
            EquipmentClientInfo.LayerType layerType,
            ResourceKey<?> assetKey,
            Model<?> model,
            Object state,
            ItemStack stack,
            PoseStack matrices,
            SubmitNodeCollector queue,
            int light,
            Identifier textureId,
            int outlineColor,
            int initialOrder,
            CallbackInfo ci
    ) {
        dile$currentStack = stack;
    }

    @Redirect(
            method = RENDER_LAYERS,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/layers/EquipmentLayerRenderer;getColorForLayer(Lnet/minecraft/client/resources/model/EquipmentClientInfo$Layer;I)I")
    )
    private static int dile$durabilityTint(EquipmentClientInfo.Layer layer, int color) {
        int original = getColorForLayer(layer, color);
        ArmorDurability module = ArmorDurability.getInstance();
        if (module == null || !module.isEnabled()) {
            return original;
        }
        ItemStack stack = dile$currentStack;
        if (stack == null || stack.isEmpty() || !stack.isDamageableItem()) {
            return original;
        }
        int maxDamage = stack.getMaxDamage();
        if (maxDamage <= 0) {
            return original;
        }
        float ratio = Math.min(1.0f, (float) stack.getDamageValue() / (float) maxDamage) * module.getGreenStrength();
        if (ratio <= 0.0f) {
            return original;
        }
        int base = original == -1 ? 0xFFFFFFFF : original;
        int red = (int) lerp((base >> 16) & 0xFF, 0.0f, ratio);
        int green = (int) lerp((base >> 8) & 0xFF, 255.0f, ratio);
        int blue = (int) lerp(base & 0xFF, 0.0f, ratio);
        return (0xFF << 24) | (red << 16) | (green << 8) | blue;
    }

    private static float lerp(float start, float end, float delta) {
        return start + (end - start) * delta;
    }
}
