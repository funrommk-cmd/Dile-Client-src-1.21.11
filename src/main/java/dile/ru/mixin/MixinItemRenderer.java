package dile.ru.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dile.ru.api.module.impl.visual.ItemReplacer;

import java.util.List;

@Mixin(ItemModelResolver.class)
public abstract class MixinItemRenderer {
    @Inject(method = "updateForTopItem", at = @At("HEAD"))
    private void dile$replaceSwordModel(
            net.minecraft.client.renderer.item.ItemStackRenderState renderState,
            ItemStack stack,
            ItemDisplayContext displayContext,
            Level level,
            ItemOwner itemOwner,
            int seed,
            CallbackInfo ci) {
        ItemReplacer module = ItemReplacer.getInstance();
        if (module == null || !dile$isSword(stack)) return;

        if (!module.isEnabled()) {
            stack.remove(DataComponents.CUSTOM_MODEL_DATA);
            return;
        }

        if (module.isSelfOnly()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null || itemOwner != mc.player) return;
        }

        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(
                List.of(), List.of(), List.of(module.getSelectedWeapon()), List.of()
        ));
    }

    private static boolean dile$isSword(ItemStack stack) {
        return stack.is(Items.DIAMOND_SWORD) || stack.is(Items.NETHERITE_SWORD) ||
                stack.is(Items.IRON_SWORD) || stack.is(Items.GOLDEN_SWORD) ||
                stack.is(Items.STONE_SWORD) || stack.is(Items.WOODEN_SWORD);
    }
}
