package dile.ru.mixin;

import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.world.level.block.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.ItemInHandRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import dile.ru.api.HmiRendererAccess;
import dile.ru.api.module.impl.visual.HoldMyItems;

import java.util.Random;

@Mixin({ItemInHandRenderer.class})
public abstract class HeldItemRendererHmiMixin implements HmiRendererAccess {
    @Shadow protected abstract void renderMapHand(PoseStack matrices, SubmitNodeCollector nodeCollector, int light, HumanoidArm arm);
    @Shadow protected abstract void renderPlayerArm(PoseStack matrices, SubmitNodeCollector nodeCollector, int light, float equipProgress, float swingProgress, HumanoidArm arm);
    @Shadow protected abstract void renderOneHandedMap(PoseStack matrices, SubmitNodeCollector nodeCollector, int light, float equipProgress, HumanoidArm arm, float swingProgress, ItemStack item);
    @Shadow protected abstract void renderTwoHandedMap(PoseStack matrices, SubmitNodeCollector nodeCollector, int light, float pitch, float equipProgress, float swingProgress);
    @Shadow protected abstract void applyItemArmTransform(PoseStack matrices, HumanoidArm arm, float equipProgress);
    @Shadow protected abstract void applyItemArmAttackTransform(PoseStack matrices, HumanoidArm arm, float swingProgress);
    @Shadow public abstract void renderItem(net.minecraft.world.entity.LivingEntity entity, ItemStack item, ItemDisplayContext renderMode, PoseStack matrices, SubmitNodeCollector nodeCollector, int light);

    @Unique
    private boolean hmi$repPower = false;
    @Unique
    private float hmi$prevAge = 0.0F;
    @Unique
    private double hmi$previousRotation = 0.0;
    @Unique
    private float hmi$swingAngleY = 0.0F;
    @Unique
    private float hmi$swingAngleX = 0.0F;
    @Unique
    private float hmi$swingVelocityY = 0.0F;
    @Unique
    private float hmi$swingVelocityX = 0.0F;
    @Unique
    private float hmi$swingVelocityZ = 0.0F;
    @Unique
    private float hmi$vertAngleY = 0.0F;
    @Unique
    private float hmi$vertVelocityY = 0.0F;
    @Unique
    private float hmi$vertVelocityYSlime = 0.0F;
    @Unique
    private float hmi$vertAngleYSlime = 0.0F;
    @Unique
    private float hmi$riptideCounter = 0.0F;
    @Unique
    private float hmi$netherCounter = 0.0F;
    @Unique
    private float hmi$fallCounter = 0.0F;
    @Unique
    private float hmi$inWaterCounter = 0.0F;
    @Unique
    private float hmi$inspect = 0.0F;
    @Unique
    private float hmi$tilt = 0.0F;
    @Unique
    private float hmi$freezeCounter = 0.0F;
    @Unique
    private float hmi$clCount = 0.0F;
    @Unique
    private float hmi$crawlCount = 0.0F;
    @Unique
    private float hmi$directionalCrawlCount = 0.0F;
    @Unique
    private float hmi$climbCount = 0.0F;
    @Unique
    private float hmi$mouseHolding = 1.0F;
    @Unique
    private boolean hmi$isSwinging = false;
    @Unique
    private float hmi$swingProgress = 0.0F;
    @Unique
    private boolean hmi$isForward = false;
    @Unique
    private boolean hmi$isAttacking = false;
    @Unique
    private boolean hmi$left = false;

    @Shadow
    private ItemStack mainHandItem;
    @Shadow
    @Final
    private Minecraft minecraft;

    @Unique
    private float hmiEaseInOutBack(float x) {
        float c1 = 1.70158F;
        float c2 = c1 * 1.525F;
        return (float) ((double) x < 0.5
                ? Math.pow(2.0 * x, 2.0) * ((c2 + 1.0F) * 2.0F * x - c2) / 2.0
                : (Math.pow(2.0 * x - 2.0, 2.0) * ((c2 + 1.0F) * (x * 2.0F - 2.0F) + c2) + 2.0) / 2.0);
    }

    @Unique
    private float hmiGetAttackDamage(ItemStack stack) {
        ItemAttributeModifiers modifiers = stack.getComponents().get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (modifiers == null) {
            return 0.0F;
        }
        float totalDamage = 0.0F;
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (entry.attribute().value() == Attributes.ATTACK_DAMAGE.value()) {
                totalDamage += (float) entry.modifier().amount();
            }
        }
        return totalDamage;
    }

    @Unique
    private boolean hmi$isCrawling(AbstractClientPlayer player) {
        return player.getPose() == Pose.SWIMMING && !player.isSwimming();
    }

    @Unique
    private boolean hmiIsSharpAnimation(HoldMyItems config) {
        return config != null && config.animationType.is("\u0428\u0430\u0440\u043f");
    }

    @Unique
    private void hmiAltSwing(PoseStack matrices, HumanoidArm arm, float swingProgress, ItemStack item) {
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        float f = Mth.sin(swingProgress * 3.14F);
        matrices.mulPose(Axis.YP.rotationDegrees((float) i * (45.0F + f * 0.0F)));
        matrices.mulPose(Axis.YP.rotationDegrees((float) i * -45.0F));
    }

    @Override
    public void hmiRenderCustomFirstPersonItem(
            AbstractClientPlayer player, float tickDelta, float pitch, InteractionHand hand, HumanoidArm arm, float sideFactor,
            float swingProgress, ItemStack item, float equipProgress, PoseStack matrices,
            SubmitNodeCollector nodeCollector, int light
    ) {
        HoldMyItems hmi = HoldMyItems.getInstance();
        if (hmi != null && hmi.isEnabled()) {
            boolean isUsingSpyglass = player.isUsingItem() && player.getUseItem().is(Items.SPYGLASS);
            if (!isUsingSpyglass) {
                HoldMyItems config = HoldMyItems.getInstance();
                float yaw = player.getYRot();
                double radians = Math.toRadians((double) yaw);
                double forwardX = -Math.sin(radians);
                double forwardZ = Math.cos(radians);
                net.minecraft.world.phys.Vec3 horizontalVelocity = player.getDeltaMovement();
                double dotProduct = horizontalVelocity.x * forwardX + horizontalVelocity.z * forwardZ;
                double crossProduct = horizontalVelocity.x * forwardZ - horizontalVelocity.z * forwardX;
                float al;
                if (player.getXRot() != 0.0F) {
                    al = 90.0F / player.getXRot() / 10.0F;
                } else {
                    al = 1.0F;
                }

                if (al > 1.0F) {
                    al = 1.0F;
                }

                if (al < 0.0F) {
                    al = 1.0F;
                }

                boolean bl = hand == InteractionHand.MAIN_HAND;
                matrices.pushPose();
                matrices.pushPose();

                if (bl) {
                    matrices.translate(config.mainHandX.getValue(), config.mainHandY.getValue(), config.mainHandZ.getValue());
                } else {
                    matrices.translate(config.offHandX.getValue(), config.offHandY.getValue(), config.offHandZ.getValue());
                }

                double tt = (1.0 / Math.max(Minecraft.getInstance().getFps(), 1)) * 30.0;
                float smoothness = Mth.clamp(config.smoothness.getFloat(), 0.35F, 2.5F);
                float hmiProgress = (float) Math.pow(Mth.clamp(swingProgress, 0.0F, 1.0F), smoothness);
                float swing_rot = (double) hmiProgress < 0.6 ? Mth.sin(Mth.clamp(hmiProgress, 0.0F, 0.12506F) * 12.56F) : Mth.sin(Mth.clamp(hmiProgress, 0.62532F, 0.75038F) * 12.56F);
                float swing = Mth.sin(hmiProgress * 3.14F);
                swing = hmiEaseInOutBack(swing);
                boolean sharpSword = item.is(ItemTags.SWORDS) && hmiIsSharpAnimation(config);
                if ((item.is(Items.EXPERIENCE_BOTTLE) || item.is(Items.WIND_CHARGE) || item.is(Items.EGG) || item.is(Items.ENDER_EYE) || item.is(Items.SNOWBALL) || item.getItem() instanceof SplashPotionItem || item.getItem() instanceof LingeringPotionItem) && player.getOffhandItem().isEmpty() && item.getUseAnimation() != ItemUseAnimation.TRIDENT && !item.is(Items.FIRE_CHARGE) && !player.isSwimming() && !hmi$isCrawling(player) && !player.onClimbable()) {
                    if (player.getMainArm() == HumanoidArm.LEFT) {
                        bl = !bl;
                    }

                    matrices.pushPose();
                    matrices.mulPose(Axis.YP.rotationDegrees(-25.0F * sideFactor));
                    matrices.mulPose(Axis.XP.rotationDegrees(-10.0F));
                    matrices.mulPose(Axis.YP.rotationDegrees(25.0F * sideFactor * swing));
                    matrices.mulPose(Axis.XP.rotationDegrees(30.0F * swing));
                    matrices.translate(-0.15 * (double) sideFactor, 0.1, 0.1);
                    matrices.translate((double) 0.0F, -0.55 * (double) swing, 0.4 * (double) swing * (double) 3.14F);
                    renderPlayerArm(matrices, nodeCollector, light, equipProgress, 0.0F, arm.getOpposite());
                    matrices.popPose();
                }

                if (this.minecraft.options.keyAttack.isDown() && !this.hmi$isAttacking && (double) swingProgress == (double) 0.0F) {
                    this.hmi$left = !this.hmi$left;
                }

                if (!item.isEmpty()) {
                    if (player.getMainArm() == HumanoidArm.LEFT) {
                        bl = !bl;
                    }


                    if ((this.hmi$left || item.is(ItemTags.AXES) || item.getUseAnimation() == ItemUseAnimation.TRIDENT || item.getUseAnimation() == ItemUseAnimation.BLOCK) && !item.is(ItemTags.SHOVELS)) {
                        if (sharpSword) {
                            matrices.translate(0.1 * (double) sideFactor * (double) swing_rot, 0.1 * (double) swing_rot, (double) -0.5F * (double) swing);
                            matrices.mulPose(Axis.XN.rotationDegrees(-30.0F * swing_rot));
                            matrices.mulPose(Axis.ZP.rotationDegrees(-20.0F * swing_rot * sideFactor));
                            matrices.mulPose(Axis.XN.rotationDegrees(40.0F * swing));
                        } else if (!item.is(ItemTags.SWORDS) && !item.is(ItemTags.AXES)) {
                            if (item.getUseAnimation() == ItemUseAnimation.TRIDENT) {
                                matrices.translate((double) 0.0F, (double) 0.0F, 0.45 * (double) swing_rot);
                                matrices.translate((double) -0.25F * (double) sideFactor * (double) swing, -0.35 * (double) swing_rot, -0.6 * (double) swing);
                                matrices.translate((double) 0.0F, 0.1 * (double) swing, (double) 0.0F);
                                matrices.mulPose(Axis.YP.rotationDegrees(15.0F * swing_rot * sideFactor));
                                matrices.mulPose(Axis.ZP.rotationDegrees(30.0F * swing_rot * sideFactor));
                            } else if (item.is(ConventionalItemTags.TOOLS) && item.getUseAnimation() != ItemUseAnimation.BLOCK && !item.is(ItemTags.SHOVELS)) {
                                matrices.translate(0.1 * (double) sideFactor * (double) swing_rot, 0.1 * (double) swing_rot, (double) -0.5F * (double) swing);
                                matrices.mulPose(Axis.XN.rotationDegrees(-30.0F * swing_rot));
                                matrices.mulPose(Axis.ZP.rotationDegrees(-20.0F * swing_rot * sideFactor));
                                matrices.mulPose(Axis.XN.rotationDegrees(40.0F * swing));
                            } else if (item.getUseAnimation() != ItemUseAnimation.BLOCK) {
                                matrices.translate(0.1 * (double) sideFactor * (double) swing_rot, 0.1 * (double) swing_rot, -0.1 * (double) swing);
                                matrices.mulPose(Axis.XN.rotationDegrees(-30.0F * swing_rot));
                                matrices.mulPose(Axis.ZP.rotationDegrees(-10.0F * swing_rot * sideFactor));
                                matrices.mulPose(Axis.XN.rotationDegrees(40.0F * swing));
                                matrices.mulPose(Axis.YP.rotationDegrees(10.0F * swing * sideFactor));
                            } else {
                                matrices.translate(0.1 * (double) sideFactor * (double) swing_rot, 0.1 * (double) swing_rot, -0.2 * (double) swing);
                                matrices.mulPose(Axis.XN.rotationDegrees(-10.0F * swing_rot));
                                matrices.mulPose(Axis.ZP.rotationDegrees(-10.0F * swing_rot * sideFactor));
                                matrices.mulPose(Axis.XN.rotationDegrees(20.0F * swing));
                            }
                        } else {
                            matrices.translate(0.8 * (double) sideFactor * (double) swing_rot, 0.3 * (double) swing_rot, (double) -0.5F * (double) swing);
                            matrices.mulPose(Axis.YP.rotationDegrees(15.0F * swing_rot * sideFactor));
                            matrices.mulPose(Axis.XN.rotationDegrees(-20.0F * swing_rot));
                            matrices.mulPose(Axis.ZP.rotationDegrees(-70.0F * swing_rot * sideFactor));
                            if (item.is(ItemTags.SWORDS)) {
                                matrices.mulPose(Axis.XN.rotationDegrees(40.0F * swing));
                            } else {
                                matrices.mulPose(Axis.XN.rotationDegrees(30.0F * swing));
                            }
                        }
                    } else if (!item.is(ItemTags.SHOVELS)) {
                        if (sharpSword) {
                            matrices.translate(0.1 * (double) sideFactor * (double) swing_rot, 0.1 * (double) swing_rot, (double) -0.5F * (double) swing);
                            matrices.mulPose(Axis.XN.rotationDegrees(-30.0F * swing_rot));
                            matrices.mulPose(Axis.ZP.rotationDegrees(-20.0F * swing_rot * sideFactor));
                            matrices.mulPose(Axis.XN.rotationDegrees(40.0F * swing));
                        } else if (item.is(ItemTags.SWORDS)) {
                            matrices.translate(0.1 * (double) sideFactor * (double) swing_rot, 0.1 * (double) swing_rot, (double) -0.5F * (double) swing);
                            matrices.mulPose(Axis.XN.rotationDegrees(-30.0F * swing_rot));
                            matrices.mulPose(Axis.ZP.rotationDegrees(-20.0F * swing_rot * sideFactor));
                            matrices.mulPose(Axis.XN.rotationDegrees(40.0F * swing));
                        } else if (item.is(ConventionalItemTags.TOOLS) && !item.is(ItemTags.SHOVELS)) {
                            matrices.translate(0.1 * (double) sideFactor * (double) swing_rot, 0.1 * (double) swing_rot, (double) -0.5F * (double) swing);
                            matrices.mulPose(Axis.XN.rotationDegrees(-30.0F * swing_rot));
                            matrices.mulPose(Axis.ZP.rotationDegrees(-20.0F * swing_rot * sideFactor));
                            matrices.mulPose(Axis.XN.rotationDegrees(40.0F * swing));
                        } else {
                            matrices.translate(0.1 * (double) sideFactor * (double) swing_rot, 0.1 * (double) swing_rot, -0.1 * (double) swing);
                            matrices.mulPose(Axis.XN.rotationDegrees(-30.0F * swing_rot));
                            matrices.mulPose(Axis.ZP.rotationDegrees(-10.0F * swing_rot * sideFactor));
                            matrices.mulPose(Axis.XN.rotationDegrees(40.0F * swing));
                            matrices.mulPose(Axis.YP.rotationDegrees(10.0F * swing * sideFactor));
                        }
                    } else if (item.is(ItemTags.SHOVELS)) {
                        matrices.translate((double) 0.0F, 0.15 * (double) swing_rot, (double) -0.25F * (double) swing_rot);
                        matrices.translate((double) 0.0F, (double) 0.0F, -0.2 * (double) swing);
                        matrices.mulPose(Axis.YP.rotationDegrees(15.0F * swing_rot));
                        matrices.mulPose(Axis.XP.rotationDegrees(-35.0F * swing_rot));
                        matrices.mulPose(Axis.XP.rotationDegrees(30.0F * swing));
                    }
                } else if (Block.byItem(item.getItem()) != Blocks.AIR && (!item.is(ConventionalItemTags.TOOLS) || item.is(ItemTags.TRIMMABLE_ARMOR) || item.is(ItemTags.BOOKSHELF_BOOKS) || item.getUseAnimation() == ItemUseAnimation.EAT || !item.isEnchantable()) && item.getUseAnimation() != ItemUseAnimation.BOW && item.getUseAnimation() != ItemUseAnimation.SPYGLASS && this.hmiGetAttackDamage(item) == 0.0F && item.getUseAnimation() != ItemUseAnimation.BLOCK && !item.is(Items.WARPED_FUNGUS_ON_A_STICK) && !item.is(Items.CARROT_ON_A_STICK) && !item.is(Items.FISHING_ROD) && !item.is(Items.SHEARS)) {
                    swingProgress = (float) ((double) swingProgress * 1.2);
                    if (swingProgress > 1.0F) {
                        swingProgress = 0.0F;
                    }
                } else if (!item.is(ItemTags.SHOVELS)) {
                    swingProgress = (float) ((double) swingProgress * (double) 1.5F);
                    if (swingProgress > 1.0F) {
                        swingProgress = 0.0F;
                    }
                }

                if (player.getDeltaMovement().length() >= 0.08) {
                    this.hmi$crawlCount = (float) ((double) this.hmi$crawlCount + 0.1 * player.getDeltaMovement().length() * (double) 2.0F * tt);
                    this.hmi$directionalCrawlCount = (float) ((double) this.hmi$directionalCrawlCount + 0.1 * dotProduct * (double) 4.0F * tt);
                    this.hmi$directionalCrawlCount = (float) ((double) this.hmi$directionalCrawlCount + (dotProduct > (double) 0.0F ? 0.1 * Math.abs(crossProduct) * (double) 4.0F * tt : 0.1 * Math.abs(crossProduct) * (double) -1.0F * (double) 4.0F * tt));
                }

                if (player.getDeltaMovement().y > (double) 0.0F) {
                    this.hmi$climbCount = (float) ((double) this.hmi$climbCount + 0.1 * tt);
                }

                if (player.getDeltaMovement().y < (double) 0.0F) {
                    this.hmi$climbCount = (float) ((double) this.hmi$climbCount - 0.1 * tt);
                }

                if ((hmi$isCrawling(player) && config.climbAndCrawl.getValue() || player.onClimbable() && !player.onGround() && Math.abs(player.getDeltaMovement().y) > (double) 0.0F && config.climbAndCrawl.getValue()) && !player.isUsingItem() && swingProgress == 0.0F) {
                    this.hmi$clCount = (float) ((double) this.hmi$clCount + 0.1 * tt);
                    if (this.hmi$clCount > 1.0F) {
                        this.hmi$clCount = 1.0F;
                    }

                    if (!item.is(Items.LANTERN) && !item.is(Items.SOUL_LANTERN)) {
                        matrices.mulPose(Axis.XP.rotationDegrees(-20.0F * this.hmi$clCount));
                    }
                } else {
                    this.hmi$clCount = (float) ((double) this.hmi$clCount * Math.pow((double) 0.88F, tt));
                }

                if (swingProgress == 0.0F) {
                    matrices.translate(bl ? player.getXRot() / 650.0F * this.hmi$clCount * -1.0F : player.getXRot() / 650.0F * this.hmi$clCount, 0.0F, 0.0F);
                    matrices.mulPose(Axis.XP.rotationDegrees(player.getXRot() * this.hmi$clCount));
                }

                if (!item.is(Items.LANTERN) && !item.is(Items.SOUL_LANTERN)) {
                    matrices.translate(0.0F, 0.0F, player.getXRot() / 120.0F * this.hmi$clCount);
                } else if (swingProgress == 0.0F) {
                    matrices.translate(0.0F, 0.0F, player.getXRot() / 80.0F * this.hmi$clCount);
                }

                if (player.onClimbable() && config.climbAndCrawl.getValue() && !player.onGround() && !item.is(Items.LANTERN) && !item.is(Items.SOUL_LANTERN) && !player.isUsingItem()) {
                    matrices.translate((double) 0.0F, 0.1, -0.2);
                }

                if ((player.isInWater()) && !player.isSwimming() && !player.isEyeInFluid(FluidTags.WATER)) {
                    this.hmi$inWaterCounter = (float) ((double) this.hmi$inWaterCounter + 0.1 * tt);
                    if (this.hmi$inWaterCounter >= 1.0F) {
                        this.hmi$inWaterCounter = 1.0F;
                    }
                } else {
                    this.hmi$inWaterCounter = (float) ((double) this.hmi$inWaterCounter * Math.pow((double) 0.88F, tt));
                }

                if (false && (double) player.getPercentFrozen() > 0.1) {
                    this.hmi$freezeCounter = (float) ((double) this.hmi$freezeCounter + 0.1 * tt);
                } else {
                    this.hmi$freezeCounter = (float) ((double) this.hmi$freezeCounter * Math.pow((double) 0.88F, tt));
                }

                matrices.translate((double) 0.0F, 0.02 * (double) this.hmi$inWaterCounter, (double) 0.0F);
                matrices.mulPose(Axis.ZP.rotationDegrees(8.0F * sideFactor * this.hmi$inWaterCounter));
                matrices.mulPose(Axis.XP.rotationDegrees(0.3F * Mth.sin(this.hmi$freezeCounter * 5.0F)));
                if (player.getDeltaMovement().y < -0.85 && item.is(Items.MACE) && player.getMainHandItem() == item) {
                    this.hmi$fallCounter = (float) ((double) this.hmi$fallCounter + 0.1 * tt);
                    if (this.hmi$fallCounter >= 1.0F) {
                        this.hmi$fallCounter = 1.0F;
                    }
                } else {
                    this.hmi$fallCounter = (float) ((double) this.hmi$fallCounter * Math.pow((double) 0.88F, tt));
                }

                if (bl) {
                    matrices.mulPose(Axis.XP.rotationDegrees(45.0F * this.hmi$fallCounter));
                    matrices.translate((double) 0.0F, -0.2 * (double) this.hmi$fallCounter, (double) 0.0F);
                }

                this.hmi$vertAngleY = (float) ((double) this.hmi$vertAngleY + player.getDeltaMovement().y * (double) 0.015F * tt);
                this.hmi$vertAngleY = (float) ((double) this.hmi$vertAngleY - (double) (0.1F * this.hmi$vertAngleY) * tt);
                this.hmi$vertAngleY = (float) ((double) this.hmi$vertAngleY * Math.pow((double) 0.88F, tt));
                this.hmi$vertVelocityYSlime = (float) ((double) this.hmi$vertVelocityYSlime + player.getDeltaMovement().y * (double) 0.015F * tt);
                this.hmi$vertVelocityYSlime = (float) ((double) this.hmi$vertVelocityYSlime - (double) (0.1F * this.hmi$vertAngleYSlime) * tt);
                this.hmi$vertVelocityYSlime = (float) ((double) this.hmi$vertVelocityYSlime * Math.pow((double) 0.88F, tt));
                this.hmi$vertAngleYSlime = (float) ((double) this.hmi$vertAngleYSlime + (double) this.hmi$vertVelocityYSlime * tt);
                matrices.translate(0.0F, this.hmi$vertAngleY * -1.0F, 0.0F);
                matrices.translate((double) 0.0F, Math.sin((double) player.tickCount * 0.1) * 0.007 * (double) sideFactor, (double) 0.0F);
                matrices.mulPose(Axis.YP.rotationDegrees(0.15F * Mth.sin((float) player.tickCount * 0.15F) * sideFactor));
                if (!item.isEmpty() || hmi$isCrawling(player) || player.onClimbable() && !player.onGround() || player.isSwimming()) {
                    if (player.getMainArm() == HumanoidArm.LEFT) {
                        bl = !bl;
                    }

                    if (item.getUseAnimation() == ItemUseAnimation.BLOCK) {
                        matrices.translate(0.0F, 0.0F, 0.0F);
                    } else {
                        matrices.translate((double) 0.0F, -0.1, 0.1);
                    }
                }

                if (item.is(Items.LANTERN) || item.is(Items.SOUL_LANTERN) || item.is(ItemTags.HANGING_SIGNS)) {
                    matrices.translate((double) 0.0F, 0.1, (double) 0.0F);
                    if (player.isSwimming()) {
                        matrices.translate((double) 0.0F, -0.1, 0.1);
                    }
                }

                if (player.isSwimming() && swingProgress == 0.0F && config.swimmingAnimation.getValue()) {
                    double distance = (double) this.hmi$crawlCount;
                    double swingAmplitude = (double) 1.5F;
                    double frequency = (double) 2.0F;
                    double s = distance * frequency;
                    double handRotation = Math.sin(s) * swingAmplitude;
                    double smoothRotation = handRotation * 0.8 + this.hmi$previousRotation * 0.2;
                    matrices.mulPose(Axis.YP.rotationDegrees((float) (bl ? smoothRotation : -smoothRotation)));
                    matrices.translate((double) 0.0F, (double) 0.0F, smoothRotation * (double) 0.2F);
                    double k = (double) (this.hmi$crawlCount * 2.0F);
                    double a = Math.cos(k);
                    double b = a;
                    if (a <= (double) 0.0F) {
                        b = a * (double) 0.5F;
                    }

                    matrices.mulPose(Axis.YN.rotationDegrees((float) (bl ? b * (double) 30.0F : b * (double) 30.0F * (double) -1.0F)));
                    matrices.translate((double) 0.0F, (double) 0.0F, a * (double) 0.2F);
                    if (item.isEmpty() && !bl && !player.isInvisible()) {
                        matrices.translate((double) (1.0F * sideFactor), (double) 0.0F - (double) equipProgress * 0.3, 0.3);
                        matrices.mulPose(Axis.YP.rotationDegrees(45.0F * sideFactor));
                        matrices.mulPose(Axis.ZP.rotationDegrees(-40.0F * sideFactor));
                        matrices.mulPose(Axis.XP.rotationDegrees(30.0F));
                        this.hmiAltSwing(matrices, arm, swingProgress, item);
                        float c = Mth.sin(equipProgress * 3.14F);
                        matrices.scale(0.9F, 0.9F, 0.9F);
                        renderPlayerArm(matrices, nodeCollector, light, 0.0F, 0.0F, arm);
                    }

                    this.hmi$previousRotation = smoothRotation;
                }

                if ((player.onClimbable() && !player.onGround() || hmi$isCrawling(player) && swingProgress == 0.0F) && !player.isUsingItem()) {
                    double s = (double) this.hmi$climbCount;
                    float v = (float) player.getDeltaMovement().y;
                    float a = Mth.cos((float) s * 2.0F);
                    if (player.onClimbable()) {
                        if (!item.is(Items.LANTERN) && !item.is(Items.SOUL_LANTERN)) {
                            matrices.mulPose(Axis.XP.rotationDegrees(20.0F * a * sideFactor));
                        } else {
                            matrices.mulPose(Axis.XP.rotationDegrees(1.0F * a * sideFactor));
                        }
                    }

                    if (hmi$isCrawling(player) && !player.isUsingItem() && swingProgress == 0.0F) {
                        float crawlProgress = Mth.sin(this.hmi$directionalCrawlCount * 4.0F * this.hmi$mouseHolding);
                        float upAndDown = Mth.cos(this.hmi$directionalCrawlCount * 4.0F * this.hmi$mouseHolding);
                        if (item.is(Items.LANTERN) || item.is(Items.SOUL_LANTERN)) {
                            crawlProgress *= 0.14F;
                            upAndDown *= 0.14F;
                        }

                        matrices.translate(0.2 * (double) crawlProgress, 0.3 * (double) crawlProgress * (double) sideFactor, -0.2 * (double) crawlProgress * (double) sideFactor * (double) al);
                        matrices.mulPose(Axis.YP.rotationDegrees(25.0F * crawlProgress));
                        matrices.mulPose(Axis.XP.rotationDegrees(Mth.clamp(20.0F * upAndDown * sideFactor, 0.0F, 20.0F)));
                    }

                    if (item.isEmpty() && !bl && !player.isInvisible() && (!player.onGround() && player.onClimbable() || hmi$isCrawling(player))) {
                        matrices.translate((double) (1.0F * sideFactor), (double) 0.0F - (double) equipProgress * 0.3, 0.3);
                        matrices.mulPose(Axis.YP.rotationDegrees(45.0F * sideFactor));
                        matrices.mulPose(Axis.ZP.rotationDegrees(-40.0F * sideFactor));
                        matrices.mulPose(Axis.XP.rotationDegrees(30.0F));
                        this.hmiAltSwing(matrices, arm, swingProgress, item);
                        matrices.scale(0.9F, 0.9F, 0.9F);
                        renderPlayerArm(matrices, nodeCollector, light, 0.0F, 0.0F, arm);
                    }
                }

                if (item.isEmpty()) {
                    if (bl && !player.isInvisible()) {
                        if ((player.onGround() || !player.onClimbable()) && !player.isSwimming() && !hmi$isCrawling(player)) {
                            if (player.getMainArm() == HumanoidArm.LEFT) {
                                bl = !bl;
                            }


                            matrices.translate((double) 0.0F, 0.2 * (double) swing_rot, 0.15 * (double) swing_rot);
                            matrices.translate(0.1 * (double) sideFactor * (double) swing, 0.15 * (double) swing, -0.45 * (double) swing);
                            matrices.mulPose(Axis.YP.rotationDegrees(35.0F * swing * sideFactor));
                            matrices.mulPose(Axis.XP.rotationDegrees(-30.0F * swing));
                            matrices.mulPose(Axis.YP.rotationDegrees(-10.0F * swing_rot * sideFactor));
                            matrices.mulPose(Axis.XP.rotationDegrees(10.0F * swing_rot));
                            renderPlayerArm(matrices, nodeCollector, light, 0.0F, 0.0F, arm);
                        } else {
                            matrices.translate((double) (1.0F * sideFactor), (double) 0.0F - (double) equipProgress * 0.3, 0.3);
                            matrices.mulPose(Axis.YP.rotationDegrees(45.0F * sideFactor));
                            matrices.mulPose(Axis.ZP.rotationDegrees(-40.0F * sideFactor));
                            matrices.mulPose(Axis.XP.rotationDegrees(30.0F));
                            this.hmiAltSwing(matrices, arm, swingProgress, item);
                            float c = Mth.sin(equipProgress * 3.14F);
                            matrices.scale(0.9F, 0.9F, 0.9F);
                            renderPlayerArm(matrices, nodeCollector, light, 0.0F, 0.0F, arm);
                        }
                    } else if (!bl && !player.isInvisible()) {
                        matrices.translate((double) (1.0F * sideFactor), (double) 0.0F - (double) equipProgress * 0.3, 0.3);
                        matrices.mulPose(Axis.YP.rotationDegrees(45.0F * sideFactor));
                        matrices.mulPose(Axis.ZP.rotationDegrees(-40.0F * sideFactor));
                        matrices.mulPose(Axis.XP.rotationDegrees(30.0F));
                        this.hmiAltSwing(matrices, arm, swingProgress, item);
                        float c = Mth.sin(equipProgress * 3.14F);
                        matrices.scale(0.9F, 0.9F, 0.9F);
                        renderPlayerArm(matrices, nodeCollector, light, 0.0F, 0.0F, arm);
                    }
                } else if (item.has(DataComponents.MAP_ID)) {
                    if (bl && this.mainHandItem.isEmpty()) {
                        matrices.translate((double) 0.0F, 0.1, (double) 0.0F);
                        renderTwoHandedMap(matrices, nodeCollector, light, pitch, equipProgress, swingProgress);
                    } else {
                        matrices.translate(bl ? -0.1 : 0.1, 0.1, (double) 0.0F);
                        renderOneHandedMap(matrices, nodeCollector, light, equipProgress, arm, swingProgress, item);
                    }
                } else if (item.getUseAnimation() == ItemUseAnimation.CROSSBOW) {
                    matrices.pushPose();
                    boolean bl2 = CrossbowItem.isCharged(item);
                    boolean bl3 = arm == HumanoidArm.RIGHT;
                    int i = bl3 ? 1 : -1;
                    if (player.isUsingItem() && player.getUseItemRemainingTicks() > 0 && player.getUsedItemHand() == hand) {
                        applyItemArmTransform(matrices, arm, equipProgress);
                        matrices.translate((float) i * -0.4785682F, -0.24387F, 0.05731531F);
                        matrices.mulPose(Axis.XP.rotationDegrees(-11.935F));
                        matrices.mulPose(Axis.YP.rotationDegrees((float) i * 65.3F));
                        matrices.mulPose(Axis.ZP.rotationDegrees((float) i * 9.785F));
                        float f = (float) item.getUseDuration(player) - ((float) player.getUseItemRemainingTicks() - tickDelta + 1.0F);
                        float g = f / (float) CrossbowItem.getChargeDuration(item, player);
                        if (g > 1.0F) {
                            g = 1.0F;
                        }

                        if (g > 0.1F) {
                            float h = Mth.sin((f - 0.1F) * 1.3F);
                            float j = g - 0.1F;
                            float k = h * j;
                            matrices.translate(k * 0.0F, k * 0.004F, k * 0.0F);
                        }

                        matrices.translate(g * 0.0F, g * 0.0F, g * 0.04F);
                        matrices.scale(1.0F, 1.0F, 1.0F);
                        matrices.mulPose(Axis.YN.rotationDegrees((float) i * 45.0F));
                    } else {
                        applyItemArmAttackTransform(matrices, arm, swingProgress);
                        if (bl2 && swingProgress < 0.001F && bl) {
                            matrices.translate((float) i * -0.341864F, 0.0F, 0.0F);
                            matrices.mulPose(Axis.YP.rotationDegrees((float) i * 10.0F));
                        }
                    }

                    matrices.translate(0.0F, 0.0F, -1.0F);
                    matrices.translate(-0.45 * (double) i, 0.45, 1.7);
                    matrices.translate((double) (1.0F * sideFactor), (double) 0.0F - (double) equipProgress * 0.3, 0.3);
                    matrices.mulPose(Axis.YP.rotationDegrees(45.0F * sideFactor));
                    matrices.mulPose(Axis.ZP.rotationDegrees(-40.0F * sideFactor));
                    matrices.mulPose(Axis.XP.rotationDegrees(30.0F));
                    this.hmiAltSwing(matrices, arm, swingProgress, item);
                    float c = Mth.sin(equipProgress * 3.14F);
                    matrices.scale(0.9F, 0.9F, 0.9F);
                    renderPlayerArm(matrices, nodeCollector, light, 0.0F, 0.0F, arm);
                    matrices.translate((double) -0.25F * (double) i, (double) 1.25F, 0.05);
                    matrices.mulPose(Axis.YP.rotationDegrees((float) (-90 * i)));
                    matrices.mulPose(Axis.XP.rotationDegrees(77.0F));
                    matrices.mulPose(Axis.ZP.rotationDegrees((float) (85 * i)));
                    matrices.scale(1.2F, 1.2F, 1.2F);
                    matrices.mulPose(Axis.XP.rotationDegrees(-10.0F));
                    matrices.translate((double) 0.0F, -0.15, 0.15);
                    renderItem(player, item, bl3 ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, matrices, nodeCollector, light);
                    matrices.popPose();
                    if (player.isUsingItem() && player.getUseItemRemainingTicks() > 0 && player.getUsedItemHand() == hand) {
                        float f = (float) item.getUseDuration(player) - ((float) player.getUseItemRemainingTicks() - tickDelta + 1.0F);
                        float g = f / (float) CrossbowItem.getChargeDuration(item, player);
                        if (g > 1.0F) {
                            g = 1.0F;
                        }

                        if (g > 0.1F) {
                            float h = Mth.sin((f - 0.1F) * 1.3F);
                            float j = g - 0.1F;
                            float k = h * j;
                            matrices.translate(k * 0.0F, k * 0.004F, k * 0.0F);
                        }

                        matrices.mulPose(Axis.YN.rotationDegrees((double) g <= 0.2 ? 75.0F * g * 5.0F * (float) i : (float) (75 * i)));
                        matrices.mulPose(Axis.XN.rotationDegrees(10.0F * g * 1.5F));
                        matrices.translate(-0.37 * (double) i, (double) 0.0F, 0.6);
                        matrices.translate(0.15 * (double) g * (double) i, (double) 0.0F, (double) 0.0F);
                        renderPlayerArm(matrices, nodeCollector, light, equipProgress, swingProgress, arm.getOpposite());
                    }
                } else {
                    boolean bl2 = arm == HumanoidArm.RIGHT;
                    int l = bl2 ? 1 : -1;
                    if (player.isUsingItem() && player.getUseItemRemainingTicks() > 0 && player.getUsedItemHand() == hand) {
                        switch (item.getUseAnimation()) {
                            case NONE -> {
                                applyItemArmTransform(matrices, arm, equipProgress);
                            }
                            case EAT, DRINK -> {
                                float u = (float) item.getUseDuration(player) - ((float) player.getUseItemRemainingTicks() - tickDelta + 1.0F);
                                float y = u / 5.0F;
                                if (y > 1.0F) {
                                    y = 1.0F;
                                }

                                float q = Mth.sin(u / 2.0F * 3.14F);
                                q /= 10.0F;
                                matrices.translate((double) (1 * l), 0.1, 0.3);
                                matrices.translate(0.2 * (double) l * (double) y, -0.7 * (double) y, -0.2 * (double) y);
                                matrices.translate((double) 0.0F, -0.2 * (double) q, -0.2 * (double) q);
                                matrices.translate((double) 0.0F, 0.1 * (double) hmiEaseInOutBack(Mth.sin(y * 3.14F)), (double) 0.0F);
                                matrices.mulPose(Axis.YP.rotationDegrees((float) (45 * l)));
                                matrices.mulPose(Axis.ZP.rotationDegrees((float) (-40 * l)));
                                matrices.mulPose(Axis.XP.rotationDegrees(30.0F));
                                this.hmiAltSwing(matrices, arm, swingProgress, item);
                                float c = Mth.sin(equipProgress * 3.14F);
                                matrices.scale(0.9F, 0.9F, 0.9F);
                                matrices.mulPose(Axis.YP.rotationDegrees(45.0F * y * (float) l));
                                renderPlayerArm(matrices, nodeCollector, light, 0.0F, swingProgress, arm);
                            }
                            case BLOCK -> {
                                float k = (float) item.getUseDuration(player) - ((float) player.getUseItemRemainingTicks() - tickDelta + 1.0F);
                                float s = k / 4.0F;
                                float s2 = k / 6.0F;
                                if (s > 1.0F) {
                                    s = 1.0F;
                                }

                                if (s2 > 1.0F) {
                                    s2 = 1.0F;
                                }

                                matrices.translate((double) 0.0F, -0.2, (double) 0.0F);
                                matrices.translate((double) (1 * l), (double) 0.0F, 0.3);
                                matrices.translate(0.7 * (double) s * (double) l, (double) 0.0F, -1.3 * (double) s);
                                matrices.translate(-0.2 * (double) l * (double) s2, (double) 0.0F, (double) 0.0F);
                                matrices.mulPose(Axis.XP.rotationDegrees((float) ((double) 10.0F * Math.sin((double) s2 * 3.14))));
                                matrices.mulPose(Axis.YP.rotationDegrees(70.0F * s * (float) l));
                                matrices.mulPose(Axis.YP.rotationDegrees((float) (45 * l)));
                                matrices.mulPose(Axis.ZP.rotationDegrees((float) (-40 * l)));
                                matrices.mulPose(Axis.XP.rotationDegrees(30.0F));
                                matrices.mulPose(Axis.YP.rotationDegrees((float) (5 * l) * s));
                                matrices.mulPose(Axis.XP.rotationDegrees(-10.0F * s));
                                matrices.translate((double) 0.0F, (double) 0.0F, -0.2 * (double) s);
                                this.hmiAltSwing(matrices, arm, swingProgress, item);
                                matrices.scale(0.9F, 0.9F, 0.9F);
                                renderPlayerArm(matrices, nodeCollector, light, 0.0F, swingProgress, arm);
                                matrices.translate(0.35 * (double) l, -0.13, -0.12);
                                matrices.mulPose(Axis.ZP.rotationDegrees(10.0F * (float) l));
                                matrices.mulPose(Axis.YP.rotationDegrees(10.0F * (float) l));
                                matrices.mulPose(Axis.XP.rotationDegrees(0.0F));
                                matrices.translate(-0.2 * (double) l, -0.04, 0.15);
                                matrices.scale(1.0F, 1.0F, 1.0F);
                            }
                            case BOW -> {
                                matrices.pushPose();
                                if (player.getMainArm() == HumanoidArm.LEFT) {
                                    bl = !bl;
                                }

                                float m1 = (float) item.getUseDuration(player) - ((float) player.getUseItemRemainingTicks() - tickDelta + 1.0F);
                                float f1 = m1 / 20.0F;
                                float f = (f1 * f1 + f1 * 2.0F) / 3.0F;
                                if (f1 > 1.0F) {
                                    f1 = 1.0F;
                                }

                                if (f1 > 0.1F) {
                                    float g1 = Mth.sin((m1 - 0.1F) * 1.3F);
                                    float j1 = g1 * f1;
                                    matrices.translate(j1 * 0.0F, j1 * 0.004F, j1 * 0.0F);
                                }

                                matrices.translate(bl ? -0.1 : 0.1, (double) 0.0F, (double) f1 * 0.15);
                                renderPlayerArm(matrices, nodeCollector, light, equipProgress, swingProgress, arm);
                                matrices.popPose();
                                matrices.translate(bl ? (double) -0.5F : (double) 0.5F, -0.45, 0.1);
                                matrices.mulPose(Axis.XP.rotation(0.3F));
                                if (bl) {
                                    matrices.mulPose(Axis.ZN.rotation(-0.3F));
                                    matrices.mulPose(Axis.YN.rotation(1.0F));
                                } else {
                                    matrices.mulPose(Axis.ZP.rotation(-0.3F));
                                    matrices.mulPose(Axis.YP.rotation(1.0F));
                                }

                                renderPlayerArm(matrices, nodeCollector, light, equipProgress, swingProgress, arm.getOpposite());
                                if (bl) {
                                    matrices.mulPose(Axis.YN.rotation(2.5F));
                                } else {
                                    matrices.mulPose(Axis.YP.rotation(2.5F));
                                }

                                matrices.translate(bl ? -0.65 : 0.65, -0.35, 0.27);
                                if (f1 > 1.0F) {
                                    f1 = 1.0F;
                                }

                                matrices.popPose();
                                if (config.mb3DCompat.getValue()) {
                                    matrices.mulPose(Axis.YP.rotationDegrees((float) (10 * l)));
                                }

                                matrices.mulPose(Axis.XN.rotationDegrees(75.0F));
                                matrices.mulPose(Axis.ZN.rotationDegrees((float) (-15 * l)));
                                matrices.translate(0.8 * (double) l, (double) (0.0F - equipProgress * 0.3F), -0.1);
                                if (f > 0.1F) {
                                    float g1 = Mth.sin((m1 - 0.1F) * 1.3F);
                                    float h1 = f1 - 0.1F;
                                    float j1 = g1 * h1;
                                    matrices.translate(j1 * 0.0F, j1 * 0.004F, j1 * 0.0F);
                                }

                                matrices.pushPose();
                            }
                            case TRIDENT -> {
                                if (player.getOffhandItem().isEmpty() && !hmi$isCrawling(player) && !player.isSwimming() && !player.onClimbable()) {
                                    matrices.pushPose();
                                    matrices.mulPose(Axis.YP.rotationDegrees((float) (-25 * l)));
                                    matrices.translate(-0.15 * (double) l, 0.1, 0.1);
                                    renderPlayerArm(matrices, nodeCollector, light, equipProgress, swingProgress, arm.getOpposite());
                                    matrices.popPose();
                                }

                                float m = (float) item.getUseDuration(player) - ((float) player.getUseItemRemainingTicks() - tickDelta + 1.0F);
                                float f = m / 10.0F;
                                if (f > 1.0F) {
                                    f = 1.0F;
                                }

                                if (f > 0.1F) {
                                    float g = Mth.sin((m - 0.1F) * 1.3F);
                                    float h = f - 0.1F;
                                    float j = g * h;
                                    matrices.translate(j * 0.0F, j * 0.004F, j * 0.0F);
                                }

                                matrices.mulPose(Axis.XP.rotationDegrees(45.0F));
                                matrices.mulPose(Axis.YP.rotationDegrees((float) (25 * l)));
                                matrices.translate(0.2 * (double) l, (double) 0.0F, 0.8);
                                renderPlayerArm(matrices, nodeCollector, light, equipProgress, swingProgress, arm);
                                matrices.mulPose(Axis.XP.rotationDegrees(135.0F));
                                matrices.mulPose(Axis.ZP.rotationDegrees((float) (-65 * l)));
                                matrices.translate((double) (0.65F * (float) l), (double) -1.0F, -0.6);
                            }
                            case BRUSH -> {
                                float f5 = (float) (player.getUseItemRemainingTicks() % 10);
                                float g5 = f5 - tickDelta + 1.0F;
                                float h5 = 1.0F - g5 / 10.0F;
                                float n = -15.0F + 75.0F * Mth.cos(h5 * 2.0F * (float) Math.PI);
                                float z = (float) item.getUseDuration(player) - ((float) player.getUseItemRemainingTicks() - tickDelta + 1.0F);
                                float x = z / 4.0F;
                                if (x > 1.0F) {
                                    x = 1.0F;
                                }

                                matrices.mulPose(Axis.YP.rotationDegrees((float) (25 * l) * x));
                                matrices.translate((double) (0.3F * (float) l * x), 0.3 * (double) x, 0.1 * (double) x);
                                if (x == 1.0F) {
                                    matrices.mulPose(Axis.YP.rotationDegrees(n / 20.0F));
                                }

                                renderPlayerArm(matrices, nodeCollector, light, equipProgress, swingProgress, arm);
                            }
                            case BUNDLE -> {
                                matrices.translate((double) (1 * l), (double) 0.0F - (double) equipProgress * 0.3, 0.3);
                                matrices.mulPose(Axis.YP.rotationDegrees((float) (45 * l)));
                                matrices.mulPose(Axis.ZP.rotationDegrees((float) (-40 * l)));
                                matrices.mulPose(Axis.XP.rotationDegrees(30.0F));
                                this.hmiAltSwing(matrices, arm, swingProgress, item);
                                matrices.scale(0.9F, 0.9F, 0.9F);
                                renderPlayerArm(matrices, nodeCollector, light, 0.0F, 0.0F, arm);
                            }
                            default -> {
                            }
                        }
                    } else if (player.isAutoSpinAttack() && item.getUseAnimation() == ItemUseAnimation.TRIDENT) {
                        this.hmi$riptideCounter = (float) ((double) this.hmi$riptideCounter + 0.15 * tt);
                        float m = (float) item.getUseDuration(player) - ((float) player.getUseItemRemainingTicks() - tickDelta + 1.0F);
                        float f = m / 10.0F;
                        if (f > 1.0F) {
                            f = 1.0F;
                        }

                        if (f > 0.1F) {
                            float g = Mth.sin((m - 0.1F) * 1.3F);
                            float h = f - 0.1F;
                            float j = g * h;
                            matrices.translate(j * 0.0F, j * 0.004F, j * 0.0F);
                        }

                        matrices.mulPose(Axis.XP.rotationDegrees(45.0F - this.hmi$riptideCounter * 2.0F));
                        matrices.mulPose(Axis.YP.rotationDegrees((float) (25 * l)));
                        matrices.translate(0.2 * (double) l, (double) 0.0F, (double) 0.75F);
                        matrices.translate((double) 0.0F, (double) 0.0F, 0.01 * (double) Mth.sin(this.hmi$riptideCounter * 6.28F));
                        renderPlayerArm(matrices, nodeCollector, light, equipProgress, swingProgress, arm);
                        matrices.mulPose(Axis.XP.rotationDegrees(135.0F));
                        matrices.mulPose(Axis.ZP.rotationDegrees((float) (-65 * l)));
                        matrices.translate((double) (0.65F * (float) l), (double) -1.0F, -0.6);
                    } else {
                        this.hmi$riptideCounter = 0.0F;
                        if (!item.is(Items.LANTERN) && !item.is(Items.SOUL_LANTERN) && !item.is(ItemTags.HANGING_SIGNS)) {
                            if (item.getUseAnimation() == ItemUseAnimation.BLOCK) {
                                matrices.translate((double) 0.0F, -0.2, (double) 0.0F);
                            }
                        } else {
                            matrices.translate(0.1 * (double) l, (double) 0.0F, -0.1);
                            matrices.mulPose(Axis.XP.rotationDegrees(10.0F));
                        }

                        matrices.translate((double) (1 * l), (double) 0.0F - (double) equipProgress * 0.3, 0.3);
                        matrices.mulPose(Axis.YP.rotationDegrees((float) (45 * l)));
                        matrices.mulPose(Axis.ZP.rotationDegrees((float) (-40 * l)));
                        matrices.mulPose(Axis.XP.rotationDegrees(30.0F));
                        this.hmiAltSwing(matrices, arm, swingProgress, item);
                        matrices.scale(0.9F, 0.9F, 0.9F);
                        renderPlayerArm(matrices, nodeCollector, light, 0.0F, 0.0F, arm);
                    }

                    matrices.translate(-0.3 * (double) l, 0.65, -0.1);
                    matrices.mulPose(Axis.YP.rotationDegrees((float) (-65 * l)));
                    matrices.mulPose(Axis.XP.rotationDegrees(10.0F));
                    if (item.is(ItemTags.WOOL_CARPETS)) {
                        matrices.translate(0.2 * (double) l, -0.1, (double) 0.0F);
                    }

                    if (Block.byItem(item.getItem()) != Blocks.AIR && item.getUseAnimation() != ItemUseAnimation.EAT && !item.is(ConventionalItemTags.BUCKETS)) {
                        if (item.getHoverName().toString().toLowerCase().contains("torch")) {
                            matrices.scale(1.5F, 1.5F, 1.5F);
                            matrices.mulPose(Axis.YN.rotationDegrees((float) (25 * l)));
                            matrices.mulPose(Axis.XP.rotationDegrees(5.0F));
                            matrices.mulPose(Axis.ZP.rotationDegrees((float) (75 * l)));
                            matrices.translate(0.2 * (double) l, 0.2, 0.05);
                        } else if ((item.is(Items.STRING) || item.is(Items.REDSTONE) || item.is(Items.LEVER) || item.is(Items.TRIPWIRE_HOOK) || Block.byItem(item.getItem()).defaultBlockState().is(ConventionalBlockTags.GLASS_PANES) || Block.byItem(item.getItem()).defaultBlockState().is(BlockTags.RAILS) || Block.byItem(item.getItem()).defaultBlockState().is(BlockTags.CLIMBABLE) || item.is(ItemTags.DOORS)) && !Block.byItem(item.getItem()).defaultBlockState().is(BlockTags.LEAVES) && !Block.byItem(item.getItem()).defaultBlockState().is(BlockTags.COMBINATION_STEP_SOUND_BLOCKS) && !Block.byItem(item.getItem()).defaultBlockState().is(BlockTags.BANNERS)) {
                            matrices.translate((double) 0.0F, (double) 0.0F, -0.1);
                            matrices.mulPose(Axis.YN.rotationDegrees((float) (5 * l)));
                            matrices.mulPose(Axis.XP.rotationDegrees(15.0F));
                            matrices.mulPose(Axis.ZP.rotationDegrees((float) (75 * l)));
                        } else if (!item.is(Items.LANTERN) && !item.is(Items.SOUL_LANTERN) && !item.is(ItemTags.HANGING_SIGNS)) {
                            matrices.mulPose(Axis.YN.rotationDegrees((float) (25 * l)));
                            matrices.mulPose(Axis.XP.rotationDegrees(5.0F));
                            matrices.mulPose(Axis.ZP.rotationDegrees((float) (75 * l)));
                            matrices.translate(0.2 * (double) l, 0.2, 0.05);
                            if (Block.byItem(item.getItem()).defaultBlockState().is(BlockTags.BANNERS)) {
                                matrices.translate(-0.2 * (double) l, (double) 0.0F, (double) 0.0F);
                                matrices.scale(1.1F, 1.1F, 1.1F);
                            }
                        } else {
                            float yawDelta = player.getYRot() - player.yHeadRot;
                            float pitchDelta = player.getXRot() - player.getXRot();
                            this.hmi$swingVelocityY += yawDelta * 0.015F * (float) tt;
                            this.hmi$swingVelocityY += swingProgress * 2.0F * (float) tt;
                            this.hmi$swingVelocityX += pitchDelta * 0.015F * (float) tt;
                            this.hmi$swingVelocityY -= 0.1F * this.hmi$swingAngleY * (float) tt;
                            this.hmi$swingVelocityX -= 0.1F * this.hmi$swingAngleX * (float) tt;
                            this.hmi$swingVelocityY = (float) ((double) this.hmi$swingVelocityY * Math.pow((double) 0.88F, tt));
                            this.hmi$swingVelocityX = (float) ((double) this.hmi$swingVelocityX * Math.pow((double) 0.88F, tt));
                            this.hmi$swingAngleY += this.hmi$swingVelocityY * (float) tt;
                            this.hmi$swingAngleX += this.hmi$swingVelocityX * (float) tt;
                            double currentSpeed = player.getDeltaMovement().length();
                            this.hmi$swingVelocityZ = (float) ((double) this.hmi$swingVelocityZ + (bl ? (currentSpeed * (double) -1.0F * (double) 15.0F - (double) this.hmi$swingVelocityZ) * (double) 0.1F * tt : (currentSpeed * (double) 15.0F - (double) this.hmi$swingVelocityZ) * (double) 0.1F * tt));
                            if ((currentSpeed > 0.09 && player.onGround() || player.isSwimming() || player.onClimbable() && !player.onGround()) && (Boolean) this.minecraft.options.bobView().get()) {
                                Random random = new Random();
                                boolean randomBoolean = random.nextBoolean();
                                this.hmi$swingVelocityY += (float) (randomBoolean ? (double) -5.5F * currentSpeed * tt : (double) 5.5F * currentSpeed * tt);
                            }

                            matrices.translate((double) 0.0F, (double) 0.0F, -0.1);
                            matrices.mulPose(Axis.YN.rotationDegrees((float) (35 * l) + this.hmi$swingAngleY));
                            matrices.mulPose(Axis.XP.rotationDegrees(15.0F + this.hmi$swingAngleX));
                            matrices.mulPose(Axis.ZP.rotationDegrees((float) (75 * l) + this.hmi$swingVelocityZ));
                            if (item.is(ItemTags.HANGING_SIGNS)) {
                                matrices.translate((double) 0.0F, -0.1, (double) 0.0F);
                                matrices.mulPose(Axis.YP.rotationDegrees((float) (-45 * l)));
                            }

                            matrices.translate(0.3 * (double) l, -0.35, (double) 0.0F);
                            matrices.translate((double) 0.0F, (double) 0.0F, 0.1);
                            matrices.scale(1.5F, 1.5F, 1.5F);
                        }
                    } else {
                        if ((!item.is(ConventionalItemTags.TOOLS) || item.is(ItemTags.TRIMMABLE_ARMOR) || item.is(ItemTags.BOOKSHELF_BOOKS) || item.getUseAnimation() == ItemUseAnimation.EAT || !item.isEnchantable()) && item.getUseAnimation() != ItemUseAnimation.BOW && item.getUseAnimation() != ItemUseAnimation.SPYGLASS && this.hmiGetAttackDamage(item) == 0.0F && item.getUseAnimation() != ItemUseAnimation.BLOCK && !item.is(Items.WARPED_FUNGUS_ON_A_STICK) && !item.is(Items.CARROT_ON_A_STICK) && !item.is(Items.FISHING_ROD) && !item.is(Items.SHEARS) && !item.is(ItemTags.HOES) && !config.mb3DCompat.getValue()) {
                            if (item.getUseAnimation() == ItemUseAnimation.BRUSH) {
                                matrices.mulPose(Axis.XN.rotationDegrees(25.0F));
                                matrices.translate(bl ? (double) 0.0F : 0.35, bl ? (double) 0.0F : (double) 0.25F, bl ? (double) 0.0F : 0.37);
                                if (!bl) {
                                    matrices.scale(0.75F, 0.75F, 0.75F);
                                }

                                matrices.mulPose(Axis.ZN.rotationDegrees((float) (-75 * l)));
                                matrices.mulPose(Axis.XN.rotationDegrees(35.0F));
                                matrices.translate(bl ? -0.05 : 0.85, bl ? (double) 0.0F : 0.05, bl ? 0.08 : -0.2);
                            } else {
                                matrices.mulPose(Axis.YN.rotationDegrees((float) (5 * l)));
                                matrices.mulPose(Axis.XP.rotationDegrees(15.0F));
                                matrices.mulPose(Axis.ZP.rotationDegrees((float) (75 * l)));
                                matrices.translate((double) 0.0F, -0.05, -0.1);
                                matrices.scale(0.7F, 0.7F, 0.7F);
                            }

                            if (item.is(Items.FEATHER) || item.is(Items.SLIME_BALL) || item.is(Items.PUFFERFISH)) {
                                this.hmi$vertVelocityYSlime = (float) ((double) this.hmi$vertVelocityYSlime + (double) swingProgress * 0.03 * (float) tt);
                                if ((player.getDeltaMovement().length() > 0.09 && player.onGround() || player.isSwimming() || hmi$isCrawling(player) || player.onClimbable() && !player.onGround()) && (Boolean) this.minecraft.options.bobView().get()) {
                                    Random random = new Random();
                                    boolean randomBoolean = random.nextBoolean();
                                    this.hmi$vertVelocityYSlime += (float) (-0.05 * player.getDeltaMovement().length() * (float) tt);
                                }

                                matrices.scale(1.0F, 1.0F + this.hmi$vertAngleYSlime * -2.0F, 1.0F);
                            }
                        } else if (item.getUseAnimation() == ItemUseAnimation.BLOCK && item.getUseAnimation() != ItemUseAnimation.TRIDENT) {
                            matrices.mulPose(Axis.ZP.rotationDegrees((float) (160 * l)));
                            matrices.mulPose(Axis.YP.rotationDegrees((float) (-60 * l)));
                            matrices.mulPose(Axis.XP.rotationDegrees(-70.0F));
                            matrices.scale(0.75F, 0.75F, 0.75F);
                            matrices.translate(0.15 * (double) l, bl ? 0.35 : 0.45, bl ? -0.15 : -0.1);
                            matrices.translate(0.17 * (double) l, (double) 0.0F, 0.3);
                            matrices.mulPose(Axis.YP.rotationDegrees((float) (-90 * l)));
                        } else if (item.getUseAnimation() == ItemUseAnimation.TRIDENT) {
                            matrices.mulPose(Axis.YN.rotationDegrees((float) (75 * l)));
                            matrices.mulPose(Axis.XP.rotationDegrees(90.0F));
                            matrices.mulPose(Axis.ZP.rotationDegrees((float) (45 * l)));
                            matrices.translate(-0.3F * (float) l, 0.0F, 0.0F);
                        } else {
                            matrices.mulPose(Axis.YN.rotationDegrees((float) (75 * l)));
                            matrices.mulPose(Axis.XP.rotationDegrees(70.0F));
                            matrices.mulPose(Axis.ZP.rotationDegrees((float) (45 * l)));
                        }

                        if (item.getUseAnimation() != ItemUseAnimation.BLOCK) {
                            matrices.scale(1.2F, 1.2F, 1.2F);
                        }

                        if (item.getUseAnimation() == ItemUseAnimation.BOW && !player.isUsingItem()) {
                            matrices.translate(-0.1 * (double) l, -0.2, (double) 0.0F);
                        }

                        if (item.is(Items.MACE)) {
                            if (config.mb3DCompat.getValue()) {
                                matrices.translate(-0.08, 0.17, (double) 0.0F);
                                matrices.mulPose(Axis.XP.rotationDegrees(40.0F));
                            }

                            matrices.translate(0.1 * (double) l, (double) 0.0F, (double) 0.0F);
                            matrices.scale(0.9F, 0.9F, 0.9F);
                        }
                    }

                    if (item.getItem() instanceof BlockItem && (!item.is(ConventionalItemTags.BUCKETS) && item.getUseAnimation() != ItemUseAnimation.EAT && !item.is(ItemTags.BANNERS) && !item.is(Items.STRING) && !item.is(Items.REDSTONE) && !item.is(Items.LEVER) && !item.is(Items.TRIPWIRE_HOOK) && !Block.byItem(item.getItem()).defaultBlockState().is(ConventionalBlockTags.GLASS_PANES) && !Block.byItem(item.getItem()).defaultBlockState().is(BlockTags.RAILS) && !Block.byItem(item.getItem()).defaultBlockState().is(BlockTags.CLIMBABLE) && !item.is(ItemTags.DOORS) || Block.byItem(item.getItem()).defaultBlockState().is(BlockTags.LEAVES)) && !Block.byItem(item.getItem()).defaultBlockState().is(BlockTags.COMBINATION_STEP_SOUND_BLOCKS)) {
                        BlockItem blockItem = (BlockItem) item.getItem();
                        BlockRenderDispatcher blockRenderDispatcher = Minecraft.getInstance().getBlockRenderer();
                        blockRenderDispatcher.getBlockModel(blockItem.getBlock().defaultBlockState());
                        matrices.pushPose();
                        if (!bl2) {
                            matrices.translate(-0.4F, 0.0F, 0.0F);
                        }

                        matrices.scale(0.4F, 0.4F, 0.4F);
                        matrices.translate(-0.9 * (double) l, -0.45, (double) -0.5F);
                        if (Block.byItem(item.getItem()).defaultBlockState().is(BlockTags.BUTTONS)) {
                            matrices.translate(0.2 * (double) l, -0.15, -0.2);
                        }

                        if (Block.byItem(item.getItem()).defaultBlockState().is(BlockTags.PRESSURE_PLATES)) {
                            matrices.translate((double) 0.0F, 0.1, (double) 0.0F);
                        }

                        if (item.is(Items.SLIME_BLOCK) || item.is(Items.HONEY_BLOCK) || Block.byItem(item.getItem()).defaultBlockState().is(BlockTags.FLOWERS) || Block.byItem(item.getItem()).defaultBlockState().is(BlockTags.LEAVES) || Block.byItem(item.getItem()).defaultBlockState().is(BlockTags.SAPLINGS) || Block.byItem(item.getItem()).defaultBlockState().is(BlockTags.SWORD_EFFICIENT)) {
                            this.hmi$vertVelocityYSlime = (float) ((double) this.hmi$vertVelocityYSlime + (double) swingProgress * 0.03 * (float) tt);
                            if ((player.getDeltaMovement().length() > 0.09 && player.onGround() || player.isSwimming() || hmi$isCrawling(player) || player.onClimbable() && !player.onGround()) && (Boolean) this.minecraft.options.bobView().get()) {
                                Random random = new Random();
                                boolean randomBoolean = random.nextBoolean();
                                this.hmi$vertVelocityYSlime += (float) (-0.05 * player.getDeltaMovement().length() * (float) tt);
                            }

                            matrices.scale(1.0F, 1.0F + this.hmi$vertAngleYSlime * -2.0F, 1.0F);
                        }

                        BlockState blockState = blockItem.getBlock().defaultBlockState();
                        if ((float) player.tickCount - this.hmi$prevAge >= 100.0F) {
                            this.hmi$repPower = !this.hmi$repPower;
                            this.hmi$prevAge = (float) player.tickCount;
                        }

                        if (blockItem.getBlock() == Blocks.REPEATER && this.hmi$repPower) {
                            blockState = blockState.setValue(RepeaterBlock.POWERED, true);
                        }

                        if (blockItem.getBlock() == Blocks.COMPARATOR && this.hmi$repPower) {
                            blockState = blockState.setValue(ComparatorBlock.POWERED, true);
                        }

                        if (blockItem.getBlock() == Blocks.REDSTONE_TORCH && player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER)) {
                            blockState = blockState.setValue(RedstoneTorchBlock.LIT, false);
                        }

                        if ((blockItem.getBlock() == Blocks.CAMPFIRE || blockItem.getBlock() == Blocks.SOUL_CAMPFIRE) && player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER)) {
                            blockState = blockState.setValue(CampfireBlock.LIT, false);
                        }

                        if (item.is(ItemTags.BEDS)) {
                            if (bl) {
                                matrices.translate(0.9, (double) 0.0F, 0.8);
                            }

                            matrices.mulPose(Axis.YP.rotationDegrees((float) (90 * l)));
                        }

                        blockRenderDispatcher.renderSingleBlock(blockState, matrices, Minecraft.getInstance().renderBuffers().bufferSource(), light, OverlayTexture.NO_OVERLAY);
                        matrices.popPose();
                    } else {
                        if (item.is(ConventionalItemTags.TOOLS) && !item.is(ItemTags.TRIMMABLE_ARMOR) && !item.is(ItemTags.BOOKSHELF_BOOKS) && item.getUseAnimation() != ItemUseAnimation.EAT && item.isEnchantable() || item.getUseAnimation() == ItemUseAnimation.BOW || item.getUseAnimation() == ItemUseAnimation.SPYGLASS || this.hmiGetAttackDamage(item) != 0.0F || item.getUseAnimation() == ItemUseAnimation.BLOCK || item.is(Items.WARPED_FUNGUS_ON_A_STICK) || item.is(Items.CARROT_ON_A_STICK) || item.is(Items.FISHING_ROD) || item.is(Items.SHEARS)) {
                            if (item.is(ItemTags.SWORDS) && !sharpSword) {
                                matrices.mulPose(Axis.XP.rotationDegrees(-60.0F * swing));
                                matrices.translate((double) 0.0F, 0.1 * (double) swing, -0.1 * (double) swing);
                            }

                            if (item.is(ItemTags.SHOVELS)) {
                                matrices.mulPose(Axis.XP.rotationDegrees(-80.0F * swing_rot));
                                matrices.mulPose(Axis.XP.rotationDegrees(30.0F * swing));
                            } else if (item.getUseAnimation() == ItemUseAnimation.TRIDENT) {
                                matrices.mulPose(Axis.XP.rotationDegrees(-40.0F * swing_rot));
                                matrices.translate((double) 0.0F, 0.1 * (double) swing_rot, -0.1 * (double) swing_rot);
                            } else if (item.getUseAnimation() != ItemUseAnimation.BLOCK) {
                                matrices.mulPose(Axis.XP.rotationDegrees(-25.0F * swing));
                                matrices.translate((double) 0.0F, 0.05 * (double) swing, -0.05 * (double) swing);
                            }
                        }

                        if (!item.is(Items.NETHER_STAR) && (!item.is(Items.END_CRYSTAL) || !config.mb3DCompat.getValue())) {
                            this.hmi$netherCounter = 0.0F;
                        } else {
                            this.hmi$netherCounter = (float) ((double) this.hmi$netherCounter + 0.9 * tt);
                            matrices.translate((double) 0.0F, (double) 0.25F + 0.02 * (double) Mth.sin(this.hmi$netherCounter * 0.1F), (double) 0.0F);
                            matrices.mulPose(Axis.XP.rotationDegrees(3.0F * Mth.sin(this.hmi$netherCounter * 0.2F)));
                            matrices.scale(1.0F + 0.01F * Mth.sin(this.hmi$netherCounter), 1.0F + 0.01F * Mth.sin(this.hmi$netherCounter), 1.0F + 0.01F * Mth.sin(this.hmi$netherCounter));
                        }

                        if (config.mb3DCompat.getValue()) {
                            if (item.is(ItemTags.SWORDS)) {
                                matrices.translate((double) 0.0F, 0.2, (double) 0.0F);
                            }

                            if (item.is(Items.FEATHER) || item.is(Items.SLIME_BALL) || item.is(Items.PUFFERFISH)) {
                                this.hmi$vertVelocityYSlime = (float) ((double) this.hmi$vertVelocityYSlime + (double) swingProgress * 0.03 * (float) tt);
                                if ((player.getDeltaMovement().length() > 0.09 && player.onGround() || player.isSwimming() || hmi$isCrawling(player) || player.onClimbable() && !player.onGround()) && (Boolean) this.minecraft.options.bobView().get()) {
                                    Random random = new Random();
                                    boolean randomBoolean = random.nextBoolean();
                                    this.hmi$vertVelocityYSlime += (float) (-0.05 * player.getDeltaMovement().length() * (float) tt);
                                }

                                matrices.scale(1.0F, 1.0F + this.hmi$vertAngleYSlime * -2.0F, 1.0F);
                            }
                        }

                        if (item.is(ItemTags.SHOVELS)) {
                            matrices.translate(0.07 * (double) l, (double) 0.0F, 0.05);
                            matrices.mulPose(Axis.YP.rotationDegrees((float) (90 * l)));
                            matrices.mulPose(Axis.XP.rotationDegrees(-15.0F));
                        }

                        if (item.is(Items.TORCH)) {

                        }

                        renderItem(player, item, bl2 ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, matrices, nodeCollector, light);
                    }
                }

                matrices.popPose();
                matrices.popPose();
                this.hmi$isAttacking = this.minecraft.options.keyAttack.isDown();
            }

        }
    }

}
