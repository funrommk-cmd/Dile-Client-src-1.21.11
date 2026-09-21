package dile.ru.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dile.ru.api.events.impl.AttackEvent;
import dile.ru.api.events.impl.BlockBreakingEvent;
import dile.ru.api.events.impl.ClickSlotEvent;
import dile.ru.api.events.impl.InteractEntityEvent;
import dile.ru.api.module.impl.combat.AuraModule;
import dile.ru.api.module.impl.combat.AutoExplosion;
import dile.ru.api.module.ModuleManager;
import dile.ru.manager.Manager;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    @Unique
    private BlockState dile$preUseItemOnState;
    @Inject(method = "handleInventoryMouseClick", at = @At("HEAD"), cancellable = true)
    private void dile$clickSlotHook(int syncId, int slotId, int button, ClickType actionType, Player player, CallbackInfo info) {
        ClickSlotEvent event = Manager.postEvent(new ClickSlotEvent(syncId, slotId, button, actionType));
        if (event.isCancelled()) {
            info.cancel();
        }
    }

    @Inject(method = "startDestroyBlock", at = @At("HEAD"))
    private void dile$startDestroyBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        Manager.postEvent(new BlockBreakingEvent(pos, direction));
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"))
    private void dile$continueDestroyBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        Manager.postEvent(new BlockBreakingEvent(pos, direction));
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void dile$onAttack(Player player, Entity target, CallbackInfo ci) {
        AttackEvent attackEvent = Manager.postEvent(new AttackEvent(target));
        InteractEntityEvent interactEvent = Manager.postEvent(new InteractEntityEvent(target));
        if (attackEvent.isCancelled() || interactEvent.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"))
    private void dile$recordUseItemOnState(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        dile$preUseItemOnState = null;
        if (player == null || player.getMainHandItem().getItem() != Items.OBSIDIAN) {
            return;
        }
        BlockPos placedPos = hitResult.getBlockPos().relative(hitResult.getDirection());
        if (player.level() != null) {
            dile$preUseItemOnState = player.level().getBlockState(placedPos);
        }
    }

    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void dile$detectObsidianPlacement(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        BlockState preState = dile$preUseItemOnState;
        dile$preUseItemOnState = null;
        if (preState == null || player == null || player.level() == null) {
            return;
        }
        if (preState.getBlock() == Blocks.OBSIDIAN) {
            return;
        }
        BlockPos placedPos = hitResult.getBlockPos().relative(hitResult.getDirection());
        if (player.level().getBlockState(placedPos).getBlock() == Blocks.OBSIDIAN) {
            AutoExplosion.handleObsidianPlaced(placedPos);
        }
    }

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void dile$cancelShieldUse(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        AuraModule aura = AuraModule.getInstance();
        if (aura != null && aura.shouldCancelInteractItem(hand)) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void dile$cancelUseItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        AuraModule aura = AuraModule.getInstance();
        ModuleManager modules = Manager.getModules();
        boolean noInteract = modules != null && modules.isNoInteractEnabled();
        if (noInteract || aura != null && aura.shouldCancelInteractBlock()) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void dile$cancelInteract(Player player, Entity target, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        AuraModule aura = AuraModule.getInstance();
        InteractEntityEvent event = Manager.postEvent(new InteractEntityEvent(target));
        ModuleManager modules = Manager.getModules();
        boolean noInteract = modules != null && modules.isNoInteractEnabled();
        if (event.isCancelled() || noInteract || aura != null && aura.shouldCancelEntityInteraction()) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
