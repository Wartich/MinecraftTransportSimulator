package mcinterface1211;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * Renderer for the physics cube entity.  Renders a simple wireframe cube.
 *
 * @author clanka
 */
public class RenderPhysicsCube extends EntityRenderer<BuilderEntityPhysicsCube> {
    
    public RenderPhysicsCube(EntityRendererProvider.Context context) {
        super(context);
    }
    
    @Override
    public ResourceLocation getTextureLocation(BuilderEntityPhysicsCube entity) {
        return null;
    }
    
    @Override
    public void render(BuilderEntityPhysicsCube builder, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (builder.entity == null) {
            return;
        }
        
        poseStack.pushPose();
        
        // Get the cube size
        double size = builder.entity.getCubeSize();
        float halfSize = (float) (size / 2.0);
        
        // Get the vertex consumer for rendering lines
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.lines());
        Matrix4f matrix = poseStack.last().pose();
        
        // Define the 8 corners of the cube
        float[][] corners = {
            {-halfSize, -halfSize, -halfSize}, // 0: bottom-back-left
            { halfSize, -halfSize, -halfSize}, // 1: bottom-back-right
            { halfSize, -halfSize,  halfSize}, // 2: bottom-front-right
            {-halfSize, -halfSize,  halfSize}, // 3: bottom-front-left
            {-halfSize,  halfSize, -halfSize}, // 4: top-back-left
            { halfSize,  halfSize, -halfSize}, // 5: top-back-right
            { halfSize,  halfSize,  halfSize}, // 6: top-front-right
            {-halfSize,  halfSize,  halfSize}  // 7: top-front-left
        };
        
        // Draw the 12 edges of the cube
        // Bottom face
        drawLine(vertexConsumer, matrix, corners[0], corners[1]);
        drawLine(vertexConsumer, matrix, corners[1], corners[2]);
        drawLine(vertexConsumer, matrix, corners[2], corners[3]);
        drawLine(vertexConsumer, matrix, corners[3], corners[0]);
        
        // Top face
        drawLine(vertexConsumer, matrix, corners[4], corners[5]);
        drawLine(vertexConsumer, matrix, corners[5], corners[6]);
        drawLine(vertexConsumer, matrix, corners[6], corners[7]);
        drawLine(vertexConsumer, matrix, corners[7], corners[4]);
        
        // Vertical edges
        drawLine(vertexConsumer, matrix, corners[0], corners[4]);
        drawLine(vertexConsumer, matrix, corners[1], corners[5]);
        drawLine(vertexConsumer, matrix, corners[2], corners[6]);
        drawLine(vertexConsumer, matrix, corners[3], corners[7]);
        
        poseStack.popPose();
        
        super.render(builder, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
    
    private void drawLine(VertexConsumer consumer, Matrix4f matrix, float[] start, float[] end) {
        // Draw line from start to end with white color
        consumer.addVertex(matrix, start[0], start[1], start[2])
                .setColor(255, 255, 255, 255)
                .setNormal(0, 1, 0);
        consumer.addVertex(matrix, end[0], end[1], end[2])
                .setColor(255, 255, 255, 255)
                .setNormal(0, 1, 0);
    }
}
