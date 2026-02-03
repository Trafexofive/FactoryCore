package com.example.factorycore.client.renderer;

import com.example.factorycore.block.entity.AbstractFactoryMultiblockBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class FactoryMultiblockRenderer implements BlockEntityRenderer<AbstractFactoryMultiblockBlockEntity> {
    public FactoryMultiblockRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(AbstractFactoryMultiblockBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockPos floor = be.getConnectedFloor();
        if (floor == null) return;

        BlockPos origin = be.getBlockPos();
        TextureAtlasSprite sprite = net.minecraft.client.Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(net.minecraft.resources.ResourceLocation.withDefaultNamespace("block/black_concrete"));
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.solid());

        Vec3 start = new Vec3(0.5, 0.5, 0.5); // Center of controller
        double fdx = floor.getX() - origin.getX();
        double fdy = floor.getY() - origin.getY();
        double fdz = floor.getZ() - origin.getZ();
        Vec3 floorEnd = new Vec3(fdx + 0.5, fdy + 1.0, fdz + 0.5);

        poseStack.pushPose();
        Matrix4f pose = poseStack.last().pose();
        drawCatenary(consumer, pose, start, floorEnd, 0.05f, packedLight, packedOverlay, sprite);
        poseStack.popPose();
    }

    private void drawCatenary(VertexConsumer consumer, Matrix4f pose, Vec3 start, Vec3 end, float radius, int light, int overlay, TextureAtlasSprite sprite) {
        int segments = 8;
        Vec3 prev = start;
        for (int i = 1; i <= segments; i++) {
            double t = i / (double) segments;
            double x = start.x + (end.x - start.x) * t;
            double z = start.z + (end.z - start.z) * t;
            double y = start.y + (end.y - start.y) * t;
            // Add a slight "sag"
            y -= Math.sin(t * Math.PI) * 0.2;
            Vec3 current = new Vec3(x, y, z);
            drawSegment(consumer, pose, prev, current, radius, light, overlay, sprite);
            prev = current;
        }
    }

    private void drawSegment(VertexConsumer consumer, Matrix4f pose, Vec3 start, Vec3 end, float radius, int light, int overlay, TextureAtlasSprite sprite) {
        float u1 = sprite.getU0();
        float u2 = sprite.getU1();
        float v1 = sprite.getV0();
        float v2 = sprite.getV1();

        Vector3f s = new Vector3f((float)start.x, (float)start.y, (float)start.z);
        Vector3f e = new Vector3f((float)end.x, (float)end.y, (float)end.z);

        consumer.addVertex(pose, s.x, s.y - radius, s.z).setColor(255, 255, 255, 255).setUv(u1, v1).setLight(light).setOverlay(overlay).setNormal(0, -1, 0);
        consumer.addVertex(pose, e.x, e.y - radius, e.z).setColor(255, 255, 255, 255).setUv(u1, v2).setLight(light).setOverlay(overlay).setNormal(0, -1, 0);
        consumer.addVertex(pose, e.x, e.y + radius, e.z).setColor(255, 255, 255, 255).setUv(u2, v2).setLight(light).setOverlay(overlay).setNormal(0, 1, 0);
        consumer.addVertex(pose, s.x, s.y + radius, s.z).setColor(255, 255, 255, 255).setUv(u2, v1).setLight(light).setOverlay(overlay).setNormal(0, 1, 0);
    }
}
