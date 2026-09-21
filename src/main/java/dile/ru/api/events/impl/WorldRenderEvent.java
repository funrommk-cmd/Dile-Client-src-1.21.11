package dile.ru.api.events.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import dile.ru.api.events.Event;

public record WorldRenderEvent(PoseStack poseStack, float tickDelta) implements Event {
    public PoseStack getStack() {
        return poseStack;
    }

    public float getTickDelta() {
        return tickDelta;
    }

    public float getPartialTicks() {
        return tickDelta;
    }
}
