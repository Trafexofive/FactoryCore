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

        // EXTRA SAFETY: Only render for the bottom block.
        // This handles legacy worlds where middle/top blocks might still have BEs.
        if (be.getBlockState().hasProperty(com.example.factorycore.block.ElectricalPoleBlock.PART) &&
            be.getBlockState().getValue(com.example.factorycore.block.ElectricalPoleBlock.PART) != com.example.factorycore.block.ElectricalPoleBlock.PolePart.BOTTOM) {
            return;
        }

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

        // 1. Render Cable to Floor Node
        BlockPos floor = be.getConnectedFloor();
        if (floor != null) {
            double fdx = floor.getX() - origin.getX();
            double fdy = floor.getY() - origin.getY();
            double fdz = floor.getZ() - origin.getZ();
            Vec3 floorEnd = new Vec3(fdx + 0.5, fdy + 1.0, fdz + 0.5); // Center-top of floor
            drawCatenary(consumer, pose, start, floorEnd, 0.07f, packedLight, packedOverlay, sprite);
        }

        // 2. Render Cables to other Poles
        for (BlockPos target : be.getConnections()) {
            // Draw if Origin Long ID < Target Long ID to avoid double-drawing
            if (origin.asLong() < target.asLong()) {
                
                // Match MAX_RANGE_SQR (16.1)
                double distSqr = origin.distSqr(target);
                if (distSqr > 16.1) continue; 

                if (be.getLevel().getBlockEntity(target) instanceof com.example.factorycore.block.entity.ElectricalPoleBlockEntity) {
                    
                    double dx = target.getX() - origin.getX();
                    double dy = target.getY() - origin.getY();
                    double dz = target.getZ() - origin.getZ();

                    // Target connection point
                    Vec3 end = new Vec3(dx + 0.5, dy + 2.8, dz + 0.5);

                    // Draw Catenary Curve (Drooping Wire)
                    drawCatenary(consumer, pose, start, end, 0.07f, packedLight, packedOverlay, sprite);
                }
            }
        }

        poseStack.popPose();
    }

    private void drawCatenary(VertexConsumer consumer, Matrix4f pose, Vec3 start, Vec3 end, 
                              float width, int light, int overlay, TextureAtlasSprite sprite) {
        int segments = 8;
        float sag = 0.5f; // Maximum droop in blocks
        
        Vec3 prev = start;
        for (int i = 1; i <= segments; i++) {
            float t = (float) i / segments;
            
            // Linear interpolation
            double lx = start.x + (end.x - start.x) * t;
            double ly = start.y + (end.y - start.y) * t;
            double lz = start.z + (end.z - start.z) * t;
            
            // Add droop (parabola approximation is sufficient and cheaper than cosh)
            // 4 * sag * t * (1-t) gives a symmetric parabola with peak at t=0.5
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
        // Correct normals for each face
        
        // Plane 1: Spans 'right', Normal is 'localUp'
        addDoubleSidedQuad(consumer, pose, start, end, right, localUp, r, g, b, light, overlay, sprite);
        
        // Plane 2: Spans 'localUp', Normal is 'right'
        addDoubleSidedQuad(consumer, pose, start, end, localUp, right, r, g, b, light, overlay, sprite);
    }

    private void addDoubleSidedQuad(VertexConsumer consumer, Matrix4f pose, Vector3f p1, Vector3f p2, Vector3f widthVec, Vector3f normalVec,
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

        float nx = normalVec.x; float ny = normalVec.y; float nz = normalVec.z;

        // Front Face (Normal +N)
        consumer.addVertex(pose, x1, y1, z1).setColor(r, g, b, 1.0f).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(nx, ny, nz);
        consumer.addVertex(pose, x2, y2, z2).setColor(r, g, b, 1.0f).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(nx, ny, nz);
        consumer.addVertex(pose, x3, y3, z3).setColor(r, g, b, 1.0f).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(nx, ny, nz);
        consumer.addVertex(pose, x4, y4, z4).setColor(r, g, b, 1.0f).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(nx, ny, nz);

        // Back Face (Normal -N)
        consumer.addVertex(pose, x4, y4, z4).setColor(r, g, b, 1.0f).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(-nx, -ny, -nz);
        consumer.addVertex(pose, x3, y3, z3).setColor(r, g, b, 1.0f).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(-nx, -ny, -nz);
        consumer.addVertex(pose, x2, y2, z2).setColor(r, g, b, 1.0f).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(-nx, -ny, -nz);
        consumer.addVertex(pose, x1, y1, z1).setColor(r, g, b, 1.0f).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(-nx, -ny, -nz);
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
