package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.entities.instances.EntityPhysicsCube;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Packet used to sync physics cube position and orientation from server to client.
 * Sends absolute position and rotation to keep the client in sync with server physics.
 *
 * @author clanka
 */
public class PacketPhysicsCubeSync extends APacketEntity<EntityPhysicsCube> {
    private final Point3D position;
    private final RotationMatrix orientation;

    public PacketPhysicsCubeSync(EntityPhysicsCube cube) {
        super(cube);
        this.position = cube.position.copy();
        this.orientation = new RotationMatrix().set(cube.orientation);
    }

    public PacketPhysicsCubeSync(ByteBuf buf) {
        super(buf);
        this.position = readPoint3dFromBuffer(buf);
        this.orientation = readRotationMatrixFromBuffer(buf);
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writePoint3dToBuffer(position, buf);
        writeRotationMatrixToBuffer(orientation, buf);
    }

    @Override
    public boolean handle(AWrapperWorld world, EntityPhysicsCube cube) {
        cube.position.set(position);
        cube.orientation.set(orientation);
        return false;
    }
    
    /**
     * Helper method to write a RotationMatrix to the buffer.
     * Writes all 9 matrix elements as doubles.
     */
    private static void writeRotationMatrixToBuffer(RotationMatrix matrix, ByteBuf buf) {
        buf.writeDouble(matrix.m00);
        buf.writeDouble(matrix.m01);
        buf.writeDouble(matrix.m02);
        buf.writeDouble(matrix.m10);
        buf.writeDouble(matrix.m11);
        buf.writeDouble(matrix.m12);
        buf.writeDouble(matrix.m20);
        buf.writeDouble(matrix.m21);
        buf.writeDouble(matrix.m22);
    }
    
    /**
     * Helper method to read a RotationMatrix from the buffer.
     */
    private static RotationMatrix readRotationMatrixFromBuffer(ByteBuf buf) {
        RotationMatrix matrix = new RotationMatrix();
        matrix.m00 = buf.readDouble();
        matrix.m01 = buf.readDouble();
        matrix.m02 = buf.readDouble();
        matrix.m10 = buf.readDouble();
        matrix.m11 = buf.readDouble();
        matrix.m12 = buf.readDouble();
        matrix.m20 = buf.readDouble();
        matrix.m21 = buf.readDouble();
        matrix.m22 = buf.readDouble();
        return matrix;
    }
}
