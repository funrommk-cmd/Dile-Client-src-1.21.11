package dile.ru.api.events.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.InteractionHand;
import dile.ru.api.events.CancellableEvent;

public final class HandAnimationEvent extends CancellableEvent {
    private final PoseStack matrices;
    private final InteractionHand hand;
    private final float swingProgress;
    private final float equipProgress;
    private final boolean equipPhase;

    public HandAnimationEvent(PoseStack matrices, InteractionHand hand, float swingProgress, float equipProgress, boolean equipPhase) {
        this.matrices = matrices;
        this.hand = hand;
        this.swingProgress = swingProgress;
        this.equipProgress = equipProgress;
        this.equipPhase = equipPhase;
    }

    public PoseStack getMatrices() {
        return matrices;
    }

    public InteractionHand getHand() {
        return hand;
    }

    public float getSwingProgress() {
        return swingProgress;
    }

    public float getEquipProgress() {
        return equipProgress;
    }

    public boolean isEquipPhase() {
        return equipPhase;
    }
}
