package dile.ru.utils.render.ui.menubackground;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import org.joml.Matrix3x2f;

public final class MenuBackgroundRenderState implements GuiElementRenderState {
    private final Matrix3x2f pose;
    private final float x;
    private final float y;
    private final float width;
    private final float height;
    private final ScreenRectangle scissorArea;

    public MenuBackgroundRenderState(Matrix3x2f pose, float x, float y, float width, float height, ScreenRectangle scissorArea) {
        this.pose = pose;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.scissorArea = scissorArea;
    }

    @Override
    public void buildVertices(VertexConsumer consumer) {
        consumer.addVertexWith2DPose(pose, x, y).setUv(0.0f, 0.0f);
        consumer.addVertexWith2DPose(pose, x, y + height).setUv(0.0f, 1.0f);
        consumer.addVertexWith2DPose(pose, x + width, y + height).setUv(1.0f, 1.0f);
        consumer.addVertexWith2DPose(pose, x + width, y).setUv(1.0f, 0.0f);
    }

    @Override
    public RenderPipeline pipeline() {
        return MenuBackgroundRenderer.MENU_BACKGROUND_PIPELINE;
    }

    @Override
    public TextureSetup textureSetup() {
        return TextureSetup.noTexture();
    }

    @Override
    public ScreenRectangle scissorArea() {
        return scissorArea;
    }

    @Override
    public ScreenRectangle bounds() {
        return new ScreenRectangle(
                Math.round(x),
                Math.round(y),
                Math.round(width),
                Math.round(height)
        ).transformMaxBounds(pose);
    }
}
