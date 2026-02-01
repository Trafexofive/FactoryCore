package com.example.factorycore.client.renderer;

import com.example.factorycore.block.entity.ElectricalPoleBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class ElectricalPoleRenderer implements BlockEntityRenderer<ElectricalPoleBlockEntity> {

    public ElectricalPoleRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ElectricalPoleBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        
        if (be.getLevel() == null || be.getConnections().isEmpty()) return;

        BlockPos origin = be.getBlockPos();
        // Use entityCutout for sharp edges on the wire
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS));
        
        // Use white_concrete texture, tinted dark gray
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(ResourceLocation.withDefaultNamespace("block/white_concrete"));

        // Connection point: Top of the 3rd block (Y + 2.8) relative to BE origin
        Vec3 start = new Vec3(0.5, 2.8, 0.5);

        poseStack.pushPose();
        Matrix4f pose = poseStack.last().pose();

        for (BlockPos target : be.getConnections()) {
            // STRICT DEDUPLICATION: Only draw if Origin ID < Target ID.
            // This prevents drawing the wire twice (once from A->B and again from B->A).
            if (origin.asLong() < target.asLong()) {
                
                // SAFETY: Don't draw lines to infinity. Max 25 blocks (squared = 625).
                if (origin.distSqr(target) > 625) continue;

                // Verify target exists and is a pole to avoid ghost lines
                if (be.getLevel().getBlockEntity(target) instanceof ElectricalPoleBlockEntity) {
                    
                    double dx = target.getX() - origin.getX();
                    double dy = target.getY() - origin.getY();
                    double dz = target.getZ() - origin.getZ();

                    // Target connection point
                    Vec3 end = new Vec3(dx + 0.5, dy + 2.8, dz + 0.5);

                    // Draw Single Straight Segment
                    // Use a slightly thicker width (0.07f) for better visibility
                    drawCrossSegment(consumer, pose, start, end, 0.07f, packedLight, packedOverlay, sprite);
                }
            }
        }

        poseStack.popPose();
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
        
        // Calculate Right vector
        dir.cross(up, right);
        if (right.lengthSquared() < 1.0E-5) {
            right.set(1, 0, 0); // Handle vertical case
        }
        right.normalize().mul(width);

        // Calculate Local Up vector (perpendicular to wire)
        Vector3f localUp = new Vector3f();
        right.cross(dir, localUp);
        localUp.normalize().mul(width);

        float r = 0.15f, g = 0.15f, b = 0.15f; // Dark Gray Wire

        // Draw 2 intersecting planes (Cross shape)
        addDoubleSidedQuad(consumer, pose, start, end, right, r, g, b, light, overlay, sprite);
        addDoubleSidedQuad(consumer, pose, start, end, localUp, r, g, b, light, overlay, sprite);
    }

    private void addDoubleSidedQuad(VertexConsumer consumer, Matrix4f pose, Vector3f p1, Vector3f p2, Vector3f widthVec, 
                         float r, float g, float b, int light, int overlay, TextureAtlasSprite sprite) {
        
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        // Calculate vertices
        float x1 = p1.x - widthVec.x; float y1 = p1.y - widthVec.y; float z1 = p1.z - widthVec.z;
        float x2 = p1.x + widthVec.x; float y2 = p1.y + widthVec.y; float z2 = p1.z + widthVec.z;
        float x3 = p2.x + widthVec.x; float y3 = p2.y + widthVec.y; float z3 = p2.z + widthVec.z;
        float x4 = p2.x - widthVec.x; float y4 = p2.y - widthVec.y; float z4 = p2.z - widthVec.z;

        // Front Face
        consumer.addVertex(pose, x1, y1, z1).setColor(r, g, b, 1.0f).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
        consumer.addVertex(pose, x2, y2, z2).setColor(r, g, b, 1.0f).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
        consumer.addVertex(pose, x3, y3, z3).setColor(r, g, b, 1.0f).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
        consumer.addVertex(pose, x4, y4, z4).setColor(r, g, b, 1.0f).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);

        // Back Face (Inverse winding order)
        consumer.addVertex(pose, x4, y4, z4).setColor(r, g, b, 1.0f).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
        consumer.addVertex(pose, x3, y3, z3).setColor(r, g, b, 1.0f).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
        consumer.addVertex(pose, x2, y2, z2).setColor(r, g, b, 1.0f).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
        consumer.addVertex(pose, x1, y1, z1).setColor(r, g, b, 1.0f).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
    }

    @Override
    public boolean shouldRenderOffScreen(ElectricalPoleBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
