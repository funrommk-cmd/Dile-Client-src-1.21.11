package dile.ru.api.module.impl.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import org.joml.Quaternionf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import dile.ru.api.events.annotation.SubscribeEvent;
import dile.ru.api.events.impl.WorldRenderEvent;
import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;
import dile.ru.api.settings.impl.BooleanSetting;
import dile.ru.api.settings.impl.NumberSetting;
import dile.ru.utils.render.Render3D;
import dile.ru.utils.render.pipeline.ClientPipelines;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class Breasts extends Module {
    private final BooleanSetting renderSelf = register(new BooleanSetting("Self", "Render on yourself.", false));
    private final BooleanSetting renderOthers = register(new BooleanSetting("Others", "Render on other players.", true));
    private final NumberSetting size = register(new NumberSetting("Size", "Breast size.", 0.6, 0, 0.8, 0.01));
    private final NumberSetting cleavage = register(new NumberSetting("Cleavage", "How much the breasts point outward.", 0.0, 0, 0.1, 0.01));
    private final BooleanSetting uniboob = register(new BooleanSetting("Uniboob", "Merge the breasts into one.", true));
    private final NumberSetting physics = register(new NumberSetting("Physics", "Breast bounce intensity.", 0.333, 0, 0.5, 0.001));
    private final NumberSetting floppiness = register(new NumberSetting("Floppiness", "How much the breasts flop around.", 0.75, 0.25, 1, 0.01));
    private final BooleanSetting showInArmor = register(new BooleanSetting("Show In Armor", "Render breasts while wearing a chestplate.", true));
    private final NumberSetting xOffset = register(new NumberSetting("Offset X", "Horizontal breast offset.", 0, -1, 1, 0.01));
    private final NumberSetting yOffset = register(new NumberSetting("Offset Y", "Vertical breast offset.", 0, -1, 1, 0.01));
    private final NumberSetting zOffset = register(new NumberSetting("Offset Z", "Depth breast offset.", 0, -1, 1, 0.01));

    private final Map<UUID, BreastPhysics> leftPhysics = new HashMap<>();
    private final Map<UUID, BreastPhysics> rightPhysics = new HashMap<>();

    private static final float DEG_TO_RAD = (float) Math.PI / 180;

    private static final float[][] LEFT_UVS = {
            {24f, 21f, 27f, 26f},
            {16f, 21f, 20f, 26f},
            {20f, 17f, 24f, 21f},
            {20f, 25f, 24f, 27f},
            {20f, 21f, 24f, 26f},
    };

    private static final float[][] RIGHT_UVS = {
            {28f, 21f, 32f, 26f},
            {21f, 21f, 24f, 26f},
            {24f, 17f, 28f, 21f},
            {24f, 25f, 28f, 27f},
            {24f, 21f, 28f, 26f},
    };

    private static final float[][] LEFT_OVERLAY_UVS = {
            {0f, 0f, 0f, 0f},
            {17f, 37f, 20f, 42f},
            {20f, 34f, 24f, 37f},
            {20f, 42f, 24f, 45f},
            {20f, 37f, 24f, 42f},
    };

    private static final float[][] RIGHT_OVERLAY_UVS = {
            {28f, 37f, 31f, 42f},
            {0f, 0f, 0f, 0f},
            {24f, 34f, 28f, 37f},
            {24f, 42f, 28f, 45f},
            {24f, 37f, 28f, 42f},
    };

    public Breasts() {
        super("Breasts", "Renders breasts on the chest using the player's skin texture, replicating the Wildfire Female Gender Mod.", ModuleCategory.VISUAL);
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.level == null || client.player == null) {
            return;
        }
        Set<UUID> present = new HashSet<>();
        for (Player player : client.level.players()) {
            if (!(player instanceof AbstractClientPlayer)) {
                continue;
            }
            present.add(player.getUUID());
            if (!shouldProcess(player, client)) {
                continue;
            }
            boolean chestplate = hasChestplateArmor(player);
            float resistance = chestplate ? 0.5f : 0f;
            leftPhysics.computeIfAbsent(player.getUUID(), ignored -> new BreastPhysics()).update(player, resistance);
            rightPhysics.computeIfAbsent(player.getUUID(), ignored -> new BreastPhysics()).update(player, resistance);
        }
        leftPhysics.keySet().retainAll(present);
        rightPhysics.keySet().retainAll(present);
    }

    @SubscribeEvent
    private void onWorldRender(WorldRenderEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.gameRenderer == null) {
            return;
        }
        float tickDelta = event.getTickDelta();
        Vec3 camera = Render3D.lastCameraPos;
        if (camera == null) {
            return;
        }
        PoseStack stack = event.getStack();
        boolean rendered = false;
        for (Player player : mc.level.players()) {
            if (!shouldProcess(player, mc)) {
                continue;
            }
            try {
                rendered |= renderPlayer(stack, player, tickDelta, camera, mc);
            } catch (Exception ignored) {
            }
        }
        if (rendered) {
            mc.renderBuffers().bufferSource().endBatch();
        }
    }

    private boolean shouldProcess(Player player, Minecraft mc) {
        if (!(player instanceof AbstractClientPlayer)) {
            return false;
        }
        if (!player.isAlive()) {
            return false;
        }
        if (player.isInvisible() && !player.hasGlowingTag()) {
            return false;
        }
        if (player == mc.player) {
            if (!renderSelf.getValue()) {
                return false;
            }
            if (mc.options.getCameraType().isFirstPerson()) {
                return false;
            }
        } else if (!renderOthers.getValue()) {
            return false;
        }
        return true;
    }

    private boolean renderPlayer(PoseStack stack, Player player, float tickDelta, Vec3 camera, Minecraft mc) {
        AbstractClientPlayer clientPlayer = (AbstractClientPlayer) player;
        boolean chestplate = hasChestplateArmor(player);
        if (!showInArmor.getValue() && chestplate) {
            return false;
        }

        BreastPhysics lp = leftPhysics.computeIfAbsent(player.getUUID(), ignored -> new BreastPhysics());
        BreastPhysics rp = rightPhysics.computeIfAbsent(player.getUUID(), ignored -> new BreastPhysics());

        float lPosX = Mth.lerp(tickDelta, lp.getPrePositionX(), lp.getPositionX());
        float lPosY = Mth.lerp(tickDelta, lp.getPrePositionY(), lp.getPositionY());
        float lRot = Mth.lerp(tickDelta, lp.getPreBounceRotation(), lp.getBounceRotation());
        float physSize = Mth.lerp(tickDelta, lp.getPreBreastSize(), lp.getBreastSize());

        float bSize = physSize;
        float breastSize = Math.min(bSize * 1.5f, 0.7f);
        if (bSize > 0.7f) {
            breastSize = bSize;
        }
        if (breastSize < 0.02f) {
            return false;
        }
        float zOff = 0.0625f - bSize * 0.0625f;
        breastSize += 0.5f * Math.abs(bSize - 0.7f) * 2f;

        float breastOffsetX = round(xOffset.getFloat(), 1);
        float breastOffsetY = -round(yOffset.getFloat(), 1);
        float breastOffsetZ = -round(zOffset.getFloat(), 1);
        float outwardAngle = Math.min(Math.round(cleavage.getFloat() * 100f), 10f);
        boolean isUniboob = uniboob.getValue();

        float rPosX;
        float rPosY;
        float rRot;
        if (isUniboob) {
            rPosX = lPosX;
            rPosY = lPosY;
            rRot = lRot;
        } else {
            rPosX = Mth.lerp(tickDelta, rp.getPrePositionX(), rp.getPositionX());
            rPosY = Mth.lerp(tickDelta, rp.getPrePositionY(), rp.getPositionY());
            rRot = Mth.lerp(tickDelta, rp.getPreBounceRotation(), rp.getBounceRotation());
        }

        float resistance = chestplate ? 0.5f : 0f;
        boolean breathingAnimation = resistance <= 0.5f && isBreathing(player);
        boolean bounceEnabled = physics.getFloat() > 0 && (!chestplate || resistance < 1f);
        float breathing = -Mth.cos(player.tickCount * 0.09f) * 0.45f + 0.45f;

        Identifier skin = clientPlayer.getSkin().body().texturePath();
        MultiBufferSource.BufferSource provider = mc.renderBuffers().bufferSource();

        double x = Mth.lerp(tickDelta, player.xo, player.getX()) - camera.x;
        double y = Mth.lerp(tickDelta, player.yo, player.getY()) - camera.y;
        double z = Mth.lerp(tickDelta, player.zo, player.getZ()) - camera.z;

        stack.pushPose();
        stack.translate(x, y, z);

        float yaw = Mth.rotLerp(tickDelta, player.yBodyRotO, player.yBodyRot);
        stack.mulPose(Axis.YP.rotationDegrees(180f - yaw));

        stack.scale(-1f, -1f, 1f);
        stack.translate(0f, -1.501f, 0f);

        ModelPart body = getBodyPart(mc, player);
        float bodyX = 0f;
        float bodyY = 0f;
        float bodyZ = 0f;
        float bodyXRot = 0f;
        float bodyYRot = 0f;
        float bodyZRot = 0f;
        if (body != null) {
            bodyX = body.x;
            bodyY = body.y;
            bodyZ = body.z;
            bodyXRot = body.xRot;
            bodyYRot = body.yRot;
            bodyZRot = body.zRot;
        }

        boolean hasJacket = player.isModelPartShown(PlayerModelPart.JACKET);
        VertexConsumer consumer = provider.getBuffer(ClientPipelines.SKIN_BOX.apply(skin));

        renderSide(consumer, stack, true, bodyX, bodyY, bodyZ, bodyXRot, bodyYRot, bodyZRot,
                chestplate, lPosX, lPosY, lRot, breastSize, zOff,
                breastOffsetX, breastOffsetY, breastOffsetZ, outwardAngle, isUniboob,
                breathingAnimation, bounceEnabled, breathing, LEFT_UVS, hasJacket ? LEFT_OVERLAY_UVS : null);
        renderSide(consumer, stack, false, bodyX, bodyY, bodyZ, bodyXRot, bodyYRot, bodyZRot,
                chestplate, rPosX, rPosY, rRot, breastSize, zOff,
                breastOffsetX, breastOffsetY, breastOffsetZ, outwardAngle, isUniboob,
                breathingAnimation, bounceEnabled, breathing, RIGHT_UVS, hasJacket ? RIGHT_OVERLAY_UVS : null);

        stack.popPose();
        return true;
    }

    private void renderSide(VertexConsumer consumer, PoseStack stack, boolean left,
                            float bodyX, float bodyY, float bodyZ,
                            float bodyXRot, float bodyYRot, float bodyZRot,
                            boolean chestplate, float posX, float posY, float bounceRot,
                            float breastSize, float zOff,
                            float offsetX, float offsetY, float offsetZ,
                            float outwardAngle, boolean uniboob,
                            boolean breathingAnimation, boolean bounceEnabled,
                            float breathing, float[][] uvs, float[][] overlayUvs) {
        stack.pushPose();
        stack.translate(bodyX * 0.0625f, bodyY * 0.0625f, bodyZ * 0.0625f);
        if (bodyXRot != 0f || bodyYRot != 0f || bodyZRot != 0f) {
            stack.mulPose(new Quaternionf().rotationZYX(bodyZRot, bodyYRot, bodyXRot));
        }
        if (bounceEnabled) {
            stack.translate(posX / 32f, 0f, 0f);
            stack.translate(0f, posY / 32f, 0f);
        }
        stack.translate((left ? offsetX : -offsetX) * 0.0625f, 0.05625f + offsetY * 0.0625f, zOff - 0.125f + offsetZ * 0.0425f);
        if (!uniboob) {
            stack.translate(-0.125f * (left ? 1 : -1), 0f, 0f);
        }
        if (bounceEnabled) {
            stack.mulPose(new Quaternionf().rotationXYZ(0f, bounceRot * DEG_TO_RAD, 0f));
        }
        if (!uniboob) {
            stack.translate(0.125f * (left ? 1 : -1), 0f, 0f);
        }
        float rotation = breastSize;
        if (bounceEnabled) {
            stack.translate(0f, -0.035f * breastSize, 0f);
            rotation -= posY / 12f;
        }
        rotation = Math.min(rotation, breastSize + 0.2f);
        rotation = Math.min(rotation, 1f);
        if (chestplate) {
            stack.translate(0f, 0f, 0.01f);
        }
        Quaternionf quaternion = new Quaternionf().rotationY((left ? outwardAngle : -outwardAngle) * DEG_TO_RAD)
                .rotateX(-35f * rotation * DEG_TO_RAD);
        if (breathingAnimation) {
            quaternion.rotateX(breathing * DEG_TO_RAD);
        }
        stack.mulPose(quaternion);
        stack.scale(0.9995f, 1f, 1f);

        PoseStack.Pose pose = stack.last();
        renderSideQuads(consumer, pose, uvs, left);
        if (overlayUvs != null) {
            stack.pushPose();
            stack.translate(0f, 0f, -0.015f);
            stack.scale(1.05f, 1.05f, 1.05f);
            renderSideQuads(consumer, stack.last(), overlayUvs, left);
            stack.popPose();
        }
        stack.popPose();
    }

    private static void renderSideQuads(VertexConsumer consumer, PoseStack.Pose pose, float[][] uvs, boolean left) {
        float x0 = left ? -0.25f : 0f;
        float x1 = left ? 0f : 0.25f;
        float y0 = 0f, y1 = 0.3125f, z0 = 0f, z1 = 0.1875f;
        int color = 0xFFFFFFFF;
        quad(consumer, pose, uvs[0], x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1f, 0f, 0f, color);
        quad(consumer, pose, uvs[1], x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1f, 0f, 0f, color);
        quad(consumer, pose, uvs[2], x1, y0, z1, x0, y0, z1, x0, y0, z0, x1, y0, z0, 0f, -1f, 0f, color);
        quad(consumer, pose, uvs[3], x1, y1, z0, x0, y1, z0, x0, y1, z1, x1, y1, z1, 0f, 1f, 0f, color);
        quad(consumer, pose, uvs[4], x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0f, 0f, -1f, color);
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, float[] uv,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float nx, float ny, float nz, int color) {
        if (uv[0] == 0f && uv[1] == 0f && uv[2] == 0f && uv[3] == 0f) {
            return;
        }
        float u1 = uv[0] / 64f, v1 = uv[1] / 64f, u2 = uv[2] / 64f, v2 = uv[3] / 64f;
        consumer.addVertex(pose, ax, ay, az).setUv(u2, v1).setColor(color).setNormal(pose, nx, ny, nz);
        consumer.addVertex(pose, bx, by, bz).setUv(u1, v1).setColor(color).setNormal(pose, nx, ny, nz);
        consumer.addVertex(pose, cx, cy, cz).setUv(u1, v2).setColor(color).setNormal(pose, nx, ny, nz);
        consumer.addVertex(pose, dx, dy, dz).setUv(u2, v2).setColor(color).setNormal(pose, nx, ny, nz);
    }

    private static ModelPart getBodyPart(Minecraft mc, Player player) {
        try {
            EntityRenderer<? super Player, ?> renderer = mc.getEntityRenderDispatcher().getRenderer(player);
            if (renderer instanceof LivingEntityRenderer<?, ?, ?> livingRenderer) {
                EntityModel<?> model = livingRenderer.getModel();
                if (model instanceof HumanoidModel<?> humanoid) {
                    return humanoid.body;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static boolean hasChestplateArmor(Player player) {
        ItemStack stack = player.getItemBySlot(EquipmentSlot.CHEST);
        return !stack.isEmpty() && stack.is(ItemTags.CHEST_ARMOR);
    }

    private static boolean isBreathing(LivingEntity entity) {
        if (!entity.isUnderWater()) {
            return true;
        }
        if (MobEffectUtil.hasWaterBreathing(entity)) {
            return true;
        }
        return entity.level().getBlockState(entity.getBlockPosBelowThatAffectsMyMovement()).is(Blocks.WATER);
    }

    private static float round(float num, float decimalPlaces) {
        float factor = (float) Math.pow(10.0, decimalPlaces);
        return (float) Math.round(num * factor) / factor;
    }

    private static int swingDuration(LivingEntity entity) {
        int i = entity.getItemInHand(InteractionHand.MAIN_HAND).getSwingAnimation().duration();
        if (MobEffectUtil.hasDigSpeed(entity)) {
            return i - (1 + MobEffectUtil.getDigSpeedAmplification(entity));
        }
        return entity.hasEffect(MobEffects.MINING_FATIGUE) ? i + (1 + entity.getEffect(MobEffects.MINING_FATIGUE).getAmplifier()) * 2 : i;
    }

    private final class BreastPhysics {
        private float bounceVelX;
        private float targetBounceX;
        private float velocityX;
        private float positionX;
        private float prePositionX;
        private float bounceVel;
        private float targetBounceY;
        private float velocity;
        private float positionY;
        private float prePositionY;
        private float bounceRotVel;
        private float targetRotVel;
        private float rotVelocity;
        private float bounceRotation;
        private float preBounceRotation;
        private float breastSize;
        private float preBreastSize;
        private Pose lastPose;
        private int lastSwingDuration = 6;
        private int lastSwingTick;
        private Vec3 prePos;
        private int randomB = 1;
        private double lastVerticalMoveVelocity;

        private void update(LivingEntity entity, float resistance) {
            this.prePositionY = this.positionY;
            this.prePositionX = this.positionX;
            this.preBounceRotation = this.bounceRotation;
            this.preBreastSize = this.breastSize;
            if (this.prePos == null) {
                this.prePos = entity.position();
                return;
            }
            float bustSize = size.getFloat();
            float breastWeight = bustSize * 1.25f;
            float targetBreastSize = bustSize;
            this.breastSize += this.breastSize < targetBreastSize ? Math.abs(this.breastSize - targetBreastSize) / 2.0f : -Math.abs(this.breastSize - targetBreastSize) / 2.0f;
            Vec3 motion = entity.position().subtract(this.prePos);
            this.prePos = entity.position();
            float bounceIntensity = targetBreastSize * 3.0f * (float) Math.round(physics.getFloat() * 3.0f * 100.0f) / 100.0f;
            bounceIntensity *= 1.0f - resistance;
            if (!uniboob.getValue()) {
                bounceIntensity *= (float) ThreadLocalRandom.current().nextDouble(0.5, 2.5);
            }
            this.tickMovement(entity, motion, bounceIntensity, breastWeight);
            this.tickPose(entity, bounceIntensity);
            this.tickVehicle(entity, bounceIntensity, breastWeight);
            this.tickArmSwing(entity, bounceIntensity);
            this.finishTick();
        }

        private void tickMovement(LivingEntity entity, Vec3 motion, float bounceIntensity, float breastWeight) {
            double vertVelocity = entity.getDeltaMovement().y;
            if (this.lastVerticalMoveVelocity <= 0.0 && vertVelocity > 0.0 || this.lastVerticalMoveVelocity < 0.0 && vertVelocity == 0.0) {
                this.randomB = entity.level().random.nextBoolean() ? -1 : 1;
            }
            this.lastVerticalMoveVelocity = vertVelocity;
            this.targetBounceY = (float) motion.y * bounceIntensity;
            this.targetBounceY += breastWeight;
            this.targetRotVel = this.calcRotation(entity, bounceIntensity);
            this.targetRotVel += (float) motion.y * bounceIntensity * (float) this.randomB;
            this.targetBounceX = -this.calcRotation(entity, bounceIntensity) / 10.0f;
            float f2 = (float) entity.getDeltaMovement().lengthSqr() / 0.2f;
            if ((f2 = f2 * f2 * f2) < 1.0f) {
                f2 = 1.0f;
            }
            this.targetBounceY += Mth.cos(entity.walkAnimation.position() * 0.6662f + Mth.PI) * 0.5f * entity.walkAnimation.speed() * 0.5f / f2;
        }

        private void tickPose(LivingEntity entity, float bounceIntensity) {
            Pose pose = entity.getPose();
            if (pose != this.lastPose) {
                if (pose == Pose.CROUCHING || this.lastPose == Pose.CROUCHING) {
                    this.targetBounceY += bounceIntensity;
                } else if (pose == Pose.SLEEPING || this.lastPose == Pose.SLEEPING) {
                    this.targetBounceY = bounceIntensity;
                }
                this.lastPose = pose;
            }
        }

        private void tickVehicle(LivingEntity entity, float bounceIntensity, float breastWeight) {
            Entity vehicle = entity.getVehicle();
            if (vehicle instanceof Boat boat) {
                int rowTime0 = (int) boat.getRowingTime(0, entity.walkAnimation.position());
                int rowTime1 = (int) boat.getRowingTime(1, entity.walkAnimation.position());
                float rotationL = (float) Mth.clampedLerp(-1.0471975803375244, -0.2617993950843811, (double) ((Mth.sin(-rowTime1) + 1.0f) / 2.0f));
                float rotationR = (float) Mth.clampedLerp(-0.7853981852531433, 0.7853981852531433, (double) ((Mth.sin((float) (-rowTime0) + 1.0f) + 1.0f) / 2.0f));
                if (rotationL < -1.0f || rotationR < -0.6f) {
                    this.targetBounceY = bounceIntensity / 3.25f;
                }
            } else if (vehicle instanceof AbstractMinecart cart) {
                float speed = (float) cart.getDeltaMovement().lengthSqr();
                if (Math.random() * (double) speed < 0.5 && speed > 0.2f) {
                    this.targetBounceY = (Math.random() > 0.5 ? -bounceIntensity : bounceIntensity) / 6.0f;
                    this.targetBounceY += breastWeight;
                }
            } else if (vehicle instanceof AbstractHorse horse) {
                float movement = (float) horse.getDeltaMovement().lengthSqr();
                if (horse.getAge() % this.clampMovement(movement) != 5 && movement > 0.05f) {
                    this.targetBounceY = bounceIntensity / 4.0f;
                    this.targetBounceY += breastWeight;
                }
            } else if (vehicle instanceof Pig pig) {
                float movement = (float) pig.getDeltaMovement().lengthSqr();
                if (pig.getAge() % this.clampMovement(movement) != 5 && movement > 0.002f) {
                    this.targetBounceY = bounceIntensity * Mth.clamp(movement * 75.0f, 0.1f, 1.0f) / 4.0f;
                    this.targetBounceY += breastWeight;
                }
            } else if (vehicle instanceof Strider strider) {
                double heightOffset = (double) strider.getBbHeight() - 0.19 + (double) (0.12f * Mth.cos(strider.walkAnimation.position() * 1.5f) * 2.0f * Math.min(0.25f, strider.walkAnimation.speed()));
                this.targetBounceY += ((float) (heightOffset * 3.0) - 4.5f) * bounceIntensity;
            }
        }

        private void tickArmSwing(LivingEntity entity, float bounceIntensity) {
            int swingDuration = swingDuration(entity);
            if ((swingDuration > 1 || this.lastSwingDuration > 1) && entity.getPose() != Pose.SLEEPING) {
                float rawAmplifier = 0.0f;
                if (swingDuration < 6) {
                    rawAmplifier = 0.15f * (float) (6 - swingDuration);
                } else if (swingDuration > 6) {
                    rawAmplifier = -0.055f * (float) (swingDuration - 6);
                }
                float amplifier = Mth.clamp(1.0f + rawAmplifier, 0.6f, 1.3f);
                HumanoidArm swingingArm = entity.swingingArm == InteractionHand.MAIN_HAND ? entity.getMainArm() : entity.getMainArm().getOpposite();
                int swingTickDelta = entity.swingTime - this.lastSwingTick;
                float swingProgress = distanceFromMedian(0, this.lastSwingDuration, Mth.clamp(this.lastSwingTick, 0, this.lastSwingDuration));
                HumanoidArm swingingToward = swingProgress > -0.2f ? swingingArm.getOpposite() : swingingArm;
                int everyNthTick = Mth.clamp(swingDuration - 1, 1, 5);
                if (entity.swinging && entity.tickCount % everyNthTick == 0) {
                    this.targetBounceY += (Math.random() > 0.5 ? -0.25f : 0.25f) * amplifier * bounceIntensity;
                    float xAmp = Mth.clamp(1.0f + rawAmplifier * (rawAmplifier < 0.0f ? 1.625f : 0.8f), 0.25f, 1.225f);
                    this.targetBounceX = 0.325f * xAmp * bounceIntensity * (swingingArm == HumanoidArm.LEFT ? -1.0f : 1.0f);
                }
                if (swingTickDelta < 0 && this.lastSwingTick != this.lastSwingDuration - 1) {
                    this.targetRotVel += (swingingArm == HumanoidArm.LEFT ? -4.0f : 4.0f) * Math.abs(swingProgress) * bounceIntensity;
                } else if (entity.swinging && swingDuration > 1) {
                    this.targetRotVel += (swingingToward == HumanoidArm.LEFT ? -0.2f : 0.2f) * amplifier * bounceIntensity;
                }
                this.lastSwingTick = entity.swingTime;
            }
            if (!entity.swinging) {
                this.lastSwingTick = 0;
            }
            this.lastSwingDuration = Math.max(swingDuration, 1);
        }

        private void finishTick() {
            float percent = floppiness.getFloat();
            float bounceAmount = 0.45f * (1.0f - percent) + 0.15f;
            bounceAmount = Mth.clamp(bounceAmount, 0.15f, 0.6f);
            float delta = 2.25f - bounceAmount;
            float distanceFromMin = Math.abs(this.bounceVel + 1.5f) * 0.5f;
            float distanceFromMax = Math.abs(this.bounceVel - 2.65f) * 0.5f;
            if (this.bounceVel < -0.5f) {
                this.targetBounceY += distanceFromMin;
            }
            if (this.bounceVel > 2.5f) {
                this.targetBounceY -= distanceFromMax;
            }
            this.targetBounceY = Mth.clamp(this.targetBounceY, -1.5f, 2.5f);
            this.targetRotVel = Mth.clamp(this.targetRotVel, -25.0f, 25.0f);
            this.velocity = Mth.lerp(bounceAmount, this.velocity, (this.targetBounceY - this.bounceVel) * delta);
            this.bounceVel += this.velocity * percent * 1.1625f;
            this.velocityX = Mth.lerp(bounceAmount, this.velocityX, (this.targetBounceX - this.bounceVelX) * delta);
            this.bounceVelX += this.velocityX * percent;
            this.rotVelocity = Mth.lerp(bounceAmount, this.rotVelocity, (this.targetRotVel - this.bounceRotVel) * delta);
            this.bounceRotVel += this.rotVelocity * percent;
            this.bounceRotation = this.bounceRotVel;
            this.positionX = this.bounceVelX;
            this.positionY = this.bounceVel;
            if (this.positionY < -0.5f) {
                this.positionY = -0.5f;
            }
            if (this.positionY > 1.5f) {
                this.positionY = 1.5f;
                this.velocity = 0.0f;
            }
        }

        private float calcRotation(LivingEntity entity, float bounceIntensity) {
            Entity vehicle = entity.getVehicle();
            if (vehicle != null) {
                if (vehicleSuppressesRotation(vehicle)) {
                    return 0.0f;
                }
                if (shouldUseVehicleYaw(entity, vehicle)) {
                    float f;
                    if (vehicle instanceof LivingEntity living) {
                        f = living.yBodyRot;
                    } else {
                        f = vehicle.yRotO;
                    }
                    float previous = f;
                    return -((vehicle.getVisualRotationYInDegrees() - previous) / 15.0f) * bounceIntensity;
                }
            }
            return -((entity.yBodyRot - entity.yBodyRotO) / 15.0f) * bounceIntensity;
        }

        private int clampMovement(float movement) {
            return Math.max((int) (10.0f - movement * 2.0f), 1);
        }

        float getPrePositionY() {
            return this.prePositionY;
        }

        float getPositionY() {
            return this.positionY;
        }

        float getPrePositionX() {
            return this.prePositionX;
        }

        float getPositionX() {
            return this.positionX;
        }

        float getBounceRotation() {
            return this.bounceRotation;
        }

        float getPreBounceRotation() {
            return this.preBounceRotation;
        }

        float getBreastSize() {
            return this.breastSize;
        }

        float getPreBreastSize() {
            return this.preBreastSize;
        }
    }

    private static boolean vehicleSuppressesRotation(Entity vehicle) {
        return vehicle instanceof Chicken
                || vehicle instanceof AbstractHorse horseLike && !horseLike.isSaddled()
                || vehicle instanceof Camel camel && camel.refuseToMove();
    }

    private static boolean shouldUseVehicleYaw(LivingEntity rider, Entity vehicle) {
        return vehicle.hasControllingPassenger()
                || vehicle instanceof Boat
                || vehicle.getVisualRotationYInDegrees() == rider.getVisualRotationYInDegrees();
    }

    private static float distanceFromMedian(int p1, int p2, float point) {
        if (p1 >= p2) {
            throw new IllegalArgumentException("p2 must be greater than p1");
        }
        if (point < (float) p1 || point > (float) p2) {
            throw new IllegalArgumentException(point + " is not within bounds of (" + p1 + ", " + p2 + ")");
        }
        if (point == (float) p1 || point == (float) p2) {
            return 0.0f;
        }
        float median = (float) (p2 - p1) / 2.0f;
        if ((point -= (float) p1) > median) {
            point = -(median - (point - median));
        }
        return point / median;
    }
}
