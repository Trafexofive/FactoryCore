package com.example.factorycore.client.renderer;

import com.example.factorycore.block.entity.ElectricalPoleBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class ElectricalPoleRenderer implements BlockEntityRenderer<ElectricalPoleBlockEntity> {

    public ElectricalPoleRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(ElectricalPoleBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        
        if (be.getLevel() == null) return;

        BlockPos origin = be.getBlockPos();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS));
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(ResourceLocation.withDefaultNamespace("block/white_concrete"));

        Vec3 start = new Vec3(0.5, 2.8, 0.5); // Pole Top

        poseStack.pushPose();
        Matrix4f pose = poseStack.last().pose();

        // 1. Render Cable to Floor
        BlockPos floor = be.getConnectedFloor();
        if (floor != null) {
            Vec3 floorEnd = new Vec3(floor.getX() - origin.getX() + 0.5, floor.getY() - origin.getY() + 1.0, floor.getZ() - origin.getZ() + 0.5);
            drawCatenary(consumer, pose, start, floorEnd, 0.07f, packedLight, packedOverlay, sprite);
        }

        // 2. Render ALL connections
        for (BlockPos target : be.getConnections()) {
            double dx = target.getX() - origin.getX();
            double dy = target.getY() - origin.getY();
            double dz = target.getZ() - origin.getZ();

            // Simple logic: If it's a pole block at that position, connect to its top (2.8).
            // Otherwise (Machine), connect to its center (0.5).
            boolean isPoleAtTarget = be.getLevel().getBlockState(target).getBlock() instanceof com.example.factorycore.block.ElectricalPoleBlock;
            
            // Avoid double-drawing if both are poles
            if (isPoleAtTarget && origin.asLong() > target.asLong()) continue;

            Vec3 end = new Vec3(dx + 0.5, dy + (isPoleAtTarget ? 2.8 : 0.5), dz + 0.5);
            drawCatenary(consumer, pose, start, end, 0.07f, packedLight, packedOverlay, sprite);
        }

        poseStack.popPose();
    }

    private void drawCatenary(VertexConsumer consumer, Matrix4f pose, Vec3 start, Vec3 end, 
                              float width, int light, int overlay, TextureAtlasSprite sprite) {
        int segments = 8;
        float sag = 0.5f;
        Vec3 prev = start;
        for (int i = 1; i <= segments; i++) {
            float t = (float) i / segments;
            double lx = start.x + (end.x - start.x) * t;
            double ly = start.y + (end.y - start.y) * t;
            double lz = start.z + (end.z - start.z) * t;
            double droop = 4 * sag * t * (1 - t);
            Vec3 current = new Vec3(lx, ly - droop, lz);
            drawCrossSegment(consumer, pose, prev, current, width, light, overlay, sprite);
            prev = current;
        }
    }

    private void drawCrossSegment(VertexConsumer consumer, Matrix4f pose, Vec3 startVec, Vec3 endVec, 
                                  float width, int light, int overlay, TextureAtlasSprite sprite) {
        Vector3f start = new Vector3f((float)startVec.x, (float)startVec.y, (float)startVec.z);
        Vector3f end = new Vector3f((float)endVec.x, (float)endVec.y, (float)endVec.z);
        Vector3f dir = new Vector3f();
        end.sub(start, dir);
        if (dir.lengthSquared() < 1.0E-5) return;
        dir.normalize();
        Vector3f up = new Vector3f(0, 1, 0);
        Vector3f right = new Vector3f();
        dir.cross(up, right);
        if (right.lengthSquared() < 1.0E-5) right.set(1, 0, 0);
        right.normalize().mul(width);
        Vector3f localUp = new Vector3f();
        right.cross(dir, localUp);
        localUp.normalize().mul(width);
        addDoubleSidedQuad(consumer, pose, start, end, right, localUp, 0.15f, 0.15f, 0.15f, light, overlay, sprite);
        addDoubleSidedQuad(consumer, pose, start, end, localUp, right, 0.15f, 0.15f, 0.15f, light, overlay, sprite);
    }

    private void addDoubleSidedQuad(VertexConsumer consumer, Matrix4f pose, Vector3f p1, Vector3f p2, Vector3f widthVec, Vector3f normalVec,
                         float r, float g, float b, int light, int overlay, TextureAtlasSprite sprite) {
        float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();
        float x1 = p1.x - widthVec.x, y1 = p1.y - widthVec.y, z1 = p1.z - widthVec.z;
        float x2 = p1.x + widthVec.x, y2 = p1.y + widthVec.y, z2 = p1.z + widthVec.z;
        float x3 = p2.x + widthVec.x, y3 = p2.y + widthVec.y, z3 = p2.z + widthVec.z;
        float x4 = p2.x - widthVec.x, y4 = p2.y - widthVec.y, z4 = p2.z - widthVec.z;
        float nx = normalVec.x, ny = normalVec.y, nz = normalVec.z;
        consumer.addVertex(pose, x1, y1, z1).setColor(r, g, b, 1.0f).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(nx, ny, nz);
        consumer.addVertex(pose, x2, y2, z2).setColor(r, g, b, 1.0f).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(nx, ny, nz);
        consumer.addVertex(pose, x3, y3, z3).setColor(r, g, b, 1.0f).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(nx, ny, nz);
        consumer.addVertex(pose, x4, y4, z4).setColor(r, g, b, 1.0f).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(nx, ny, nz);
        consumer.addVertex(pose, x4, y4, z4).setColor(r, g, b, 1.0f).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(-nx, -ny, -nz);
        consumer.addVertex(pose, x3, y3, z3).setColor(r, g, b, 1.0f).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(-nx, -ny, -nz);
        consumer.addVertex(pose, x2, y2, z2).setColor(r, g, b, 1.0f).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(-nx, -ny, -nz);
        consumer.addVertex(pose, x1, y1, z1).setColor(r, g, b, 1.0f).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(-nx, -ny, -nz);
    }

    @Override public boolean shouldRenderOffScreen(ElectricalPoleBlockEntity be) { return true; }
    @Override public int getViewDistance() { return 96; }
}