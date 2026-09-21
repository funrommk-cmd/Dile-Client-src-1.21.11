package dile.ru.api.events.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import dile.ru.api.events.CancellableEvent;

public final class FirstPersonArmRenderEvent extends CancellableEvent {
    private final AbstractClientPlayer player;
    private final InteractionHand hand;
    private final float swingProgress;
    private final float equipProgress;
    private final ItemStack stack;
    private final PoseStack matrices;
    private final SubmitNodeCollector queue;
    private final int light;

    public FirstPersonArmRenderEvent(AbstractClientPlayer player, InteractionHand hand, float swingProgress, float equipProgress, ItemStack stack, PoseStack matrices, SubmitNodeCollector queue, int light) {
        this.player = player;
        this.hand = hand;
        this.swingProgress = swingProgress;
        this.equipProgress = equipProgress;
        this.stack = stack;
        this.matrices = matrices;
        this.queue = queue;
        this.light = light;
    }

    public AbstractClientPlayer getPlayer() { return player; }
    public InteractionHand getHand() { return hand; }
    public float getSwingProgress() { return swingProgress; }
    public float getEquipProgress() { return equipProgress; }
    public ItemStack getStack() { return stack; }
    public PoseStack getMatrices() { return matrices; }
    public SubmitNodeCollector getQueue() { return queue; }
    public int getLight() { return light; }
}
