package mcinterface1122;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * Renderer for the physics cube entity.  Renders a simple wireframe cube.
 *
 * @author clanka
 */
public class RenderPhysicsCube extends Render<BuilderEntityPhysicsCube> {
    
    public RenderPhysicsCube(RenderManager renderManager) {
        super(renderManager);
    }
    
    @Override
    protected ResourceLocation getEntityTexture(BuilderEntityPhysicsCube entity) {
        return null;
    }
    
    @Override
    public void doRender(BuilderEntityPhysicsCube builder, double x, double y, double z, float entityYaw, float partialTicks) {
        if (builder.entity == null) {
            return;
        }
        
        System.out.println("RenderPhysicsCube: Rendering at (" + x + ", " + y + ", " + z + ") with orientation [" + builder.entity.orientation.m00 + ", " + builder.entity.orientation.m01 + ", " + builder.entity.orientation.m02 + "]");
        
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        
        // Apply rotation from the orientation matrix
        // The orientation matrix is a 3x3 rotation matrix that needs to be converted to OpenGL's 4x4 format
        minecrafttransportsimulator.baseclasses.RotationMatrix orientation = builder.entity.orientation;
        
        // Create a 4x4 matrix in column-major order (OpenGL format)
        float[] matrix = new float[16];
        matrix[0] = (float) orientation.m00;
        matrix[1] = (float) orientation.m10;
        matrix[2] = (float) orientation.m20;
        matrix[3] = 0;
        matrix[4] = (float) orientation.m01;
        matrix[5] = (float) orientation.m11;
        matrix[6] = (float) orientation.m21;
        matrix[7] = 0;
        matrix[8] = (float) orientation.m02;
        matrix[9] = (float) orientation.m12;
        matrix[10] = (float) orientation.m22;
        matrix[11] = 0;
        matrix[12] = 0;
        matrix[13] = 0;
        matrix[14] = 0;
        matrix[15] = 1;
        
        // Apply the rotation matrix
        java.nio.FloatBuffer matrixBuffer = org.lwjgl.BufferUtils.createFloatBuffer(16);
        matrixBuffer.put(matrix);
        matrixBuffer.flip();
        GlStateManager.multMatrix(matrixBuffer);
        
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        
        // Get the cube size
        double size = builder.entity.getCubeSize();
        float halfSize = (float) (size / 2.0);
        
        // Set up for line rendering
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        
        // Draw colored wireframe to see rotation
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        
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
        
        // Draw the 12 edges of the cube with different colors
        // Bottom face - RED
        drawColoredLine(buffer, corners[0], corners[1], 1.0f, 0.0f, 0.0f);
        drawColoredLine(buffer, corners[1], corners[2], 1.0f, 0.0f, 0.0f);
        drawColoredLine(buffer, corners[2], corners[3], 1.0f, 0.0f, 0.0f);
        drawColoredLine(buffer, corners[3], corners[0], 1.0f, 0.0f, 0.0f);
        
        // Top face - GREEN
        drawColoredLine(buffer, corners[4], corners[5], 0.0f, 1.0f, 0.0f);
        drawColoredLine(buffer, corners[5], corners[6], 0.0f, 1.0f, 0.0f);
        drawColoredLine(buffer, corners[6], corners[7], 0.0f, 1.0f, 0.0f);
        drawColoredLine(buffer, corners[7], corners[4], 0.0f, 1.0f, 0.0f);
        
        // Vertical edges - BLUE
        drawColoredLine(buffer, corners[0], corners[4], 0.0f, 0.0f, 1.0f);
        drawColoredLine(buffer, corners[1], corners[5], 0.0f, 0.0f, 1.0f);
        drawColoredLine(buffer, corners[2], corners[6], 0.0f, 0.0f, 1.0f);
        drawColoredLine(buffer, corners[3], corners[7], 0.0f, 0.0f, 1.0f);
        
        tessellator.draw();
        
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }
    
    private void drawColoredLine(BufferBuilder buffer, float[] start, float[] end, float r, float g, float b) {
        buffer.pos(start[0], start[1], start[2]).color(r, g, b, 1.0f).endVertex();
        buffer.pos(end[0], end[1], end[2]).color(r, g, b, 1.0f).endVertex();
    }
}
