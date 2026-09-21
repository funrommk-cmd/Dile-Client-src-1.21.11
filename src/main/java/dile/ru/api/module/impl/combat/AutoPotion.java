package dile.ru.api.module.impl.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import dile.ru.api.events.annotation.SubscribeEvent;
import dile.ru.api.events.impl.TickEvent;
import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;
import dile.ru.api.settings.impl.MultiModeSetting;
import dile.ru.api.settings.impl.NumberSetting;
import dile.ru.utils.inventory.lookup.InventoryUtils;

public final class AutoPotion extends Module {
    private static final Minecraft mc = Minecraft.getInstance();

    private static final String FIRE_RESISTANCE = "Огнестойкость";
    private static final String STRENGTH = "Сила";
    private static final String SPEED = "Скорость";

    private final MultiModeSetting potionEffects = register(new MultiModeSetting("Effects", "Potion effects to auto-throw.",
            new String[]{FIRE_RESISTANCE, STRENGTH, SPEED},
            FIRE_RESISTANCE, STRENGTH, SPEED));
    private final NumberSetting throwDelay = register(new NumberSetting("Delay", "Delay between throws in ticks.", 20.0, 5.0, 60.0, 1.0));

    private int throwTimer;
    private Holder<MobEffect> pendingEffect;

    public AutoPotion() {
        super("Auto Potion", "Automatically throws splash potions with selected effects.", ModuleCategory.COMBAT);
    }

    @SubscribeEvent
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.level == null || mc.screen != null) {
            return;
        }

        if (throwTimer > 0) {
            throwTimer--;
            if (throwTimer == 0 && pendingEffect != null) {
                if (!mc.player.hasEffect(pendingEffect)) {
                    tryThrow(pendingEffect);
                }
                pendingEffect = null;
            }
            return;
        }

        if (mc.player.getCooldowns().isOnCooldown(Items.SPLASH_POTION.getDefaultInstance())) {
            return;
        }

        if (potionEffects.isSelected(FIRE_RESISTANCE) && !mc.player.hasEffect(MobEffects.FIRE_RESISTANCE)) {
            tryThrow(MobEffects.FIRE_RESISTANCE);
        } else if (potionEffects.isSelected(STRENGTH) && !mc.player.hasEffect(MobEffects.STRENGTH)) {
            tryThrow(MobEffects.STRENGTH);
        } else if (potionEffects.isSelected(SPEED) && !mc.player.hasEffect(MobEffects.SPEED)) {
            tryThrow(MobEffects.SPEED);
        }
    }

    private void tryThrow(Holder<MobEffect> effect) {
        int slot = findPotion(effect);
        if (slot == -1) {
            return;
        }

        pendingEffect = effect;
        throwPotion(slot);
        throwTimer = (int) throwDelay.getFloat();
    }

    private int findPotion(Holder<MobEffect> effect) {
        for (int i = 0; i < 9; i++) {
            if (matchesEffect(mc.player.getInventory().getItem(i), effect)) {
                return i;
            }
        }
        for (int i = 9; i < 36; i++) {
            if (matchesEffect(mc.player.getInventory().getItem(i), effect)) {
                return i;
            }
        }
        return -1;
    }

    private boolean matchesEffect(ItemStack stack, Holder<MobEffect> effect) {
        if (stack.getItem() != Items.SPLASH_POTION) {
            return false;
        }
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) {
            return false;
        }
        for (MobEffectInstance instance : contents.getAllEffects()) {
            if (instance.getEffect().equals(effect)) {
                return true;
            }
        }
        return false;
    }

    private void throwPotion(int slot) {
        if (mc.player == null || mc.getConnection() == null) return;

        float savedYaw = mc.player.getYRot();
        float savedPitch = mc.player.getXRot();
        int savedSlot = mc.player.getInventory().getSelectedSlot();

        mc.player.setXRot(90.0f);
        mc.getConnection().send(new ServerboundMovePlayerPacket.Rot(
                savedYaw, 90.0f, mc.player.onGround(), mc.player.horizontalCollision));

        if (slot < 9) {
            if (slot != savedSlot) {
                InventoryUtils.syncSelectedHotbarSlot(slot);
            }
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            mc.player.swing(InteractionHand.MAIN_HAND);
            if (slot != savedSlot) {
                InventoryUtils.syncSelectedHotbarSlot(savedSlot);
            }
        } else {
            int wrappedSlot = InventoryUtils.wrapSlot(slot);
            InventoryUtils.click(wrappedSlot, savedSlot, ClickType.SWAP);
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            mc.player.swing(InteractionHand.MAIN_HAND);
            InventoryUtils.click(wrappedSlot, savedSlot, ClickType.SWAP);
        }

        mc.player.connection.send(new ServerboundMovePlayerPacket.Rot(
                savedYaw, savedPitch, mc.player.onGround(), mc.player.horizontalCollision));
        mc.player.setYRot(savedYaw);
        mc.player.setXRot(savedPitch);
    }
}
