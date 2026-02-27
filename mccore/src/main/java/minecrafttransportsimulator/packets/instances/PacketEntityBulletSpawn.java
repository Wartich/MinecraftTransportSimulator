package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import java.util.UUID;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.entities.instances.EntityBullet;
import minecrafttransportsimulator.entities.instances.PartGun;
import minecrafttransportsimulator.items.instances.ItemBullet;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketBase;

/**
 * Packet used to sync isLongRange bullet spawns from server to client.
 * This is needed because isLongRange bullets do their hit detection on the server,
 * but they still need to be rendered on the client.
 * 
 * @author don_bruce
 */
public class PacketEntityBulletSpawn extends APacketBase {
    private final UUID gunID;
    private final int bulletNumber;
    private final Point3D position;
    private final Point3D velocity;
    private final RotationMatrix orientation;
    private final TargetType targetType;
    private final Point3D targetPosition;
    private final UUID targetUUID;

    private static enum TargetType {
        NONE,
        POSITION,
        UUID
    }

    public PacketEntityBulletSpawn(PartGun gun, int bulletNumber, Point3D position, Point3D velocity, RotationMatrix orientation, Point3D targetPosition, UUID targetUUID) {
        super(null);
        this.gunID = gun.uniqueUUID;
        this.bulletNumber = bulletNumber;
        this.position = position;
        this.velocity = velocity;
        this.orientation = orientation;
        
        if (targetUUID != null) {
            this.targetType = TargetType.UUID;
            this.targetPosition = null;
            this.targetUUID = targetUUID;
        } else if (targetPosition != null) {
            this.targetType = TargetType.POSITION;
            this.targetPosition = targetPosition;
            this.targetUUID = null;
        } else {
            this.targetType = TargetType.NONE;
            this.targetPosition = null;
            this.targetUUID = null;
        }
    }

    public PacketEntityBulletSpawn(ByteBuf buf) {
        super(buf);
        this.gunID = readUUIDFromBuffer(buf);
        this.bulletNumber = buf.readInt();
        this.position = readPoint3dFromBuffer(buf);
        this.velocity = readPoint3dFromBuffer(buf);
        this.orientation = new RotationMatrix();
        orientation.angles.x = buf.readDouble();
        orientation.angles.y = buf.readDouble();
        orientation.angles.z = buf.readDouble();
        
        this.targetType = TargetType.values()[buf.readByte()];
        switch (targetType) {
            case UUID:
                this.targetUUID = readUUIDFromBuffer(buf);
                this.targetPosition = null;
                break;
            case POSITION:
                this.targetPosition = readPoint3dFromBuffer(buf);
                this.targetUUID = null;
                break;
            default:
                this.targetPosition = null;
                this.targetUUID = null;
                break;
        }
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        writeUUIDToBuffer(gunID, buf);
        buf.writeInt(bulletNumber);
        writePoint3dToBuffer(position, buf);
        writePoint3dToBuffer(velocity, buf);
        buf.writeDouble(orientation.angles.x);
        buf.writeDouble(orientation.angles.y);
        buf.writeDouble(orientation.angles.z);
        
        buf.writeByte(targetType.ordinal());
        switch (targetType) {
            case UUID:
                writeUUIDToBuffer(targetUUID, buf);
                break;
            case POSITION:
                writePoint3dToBuffer(targetPosition, buf);
                break;
            default:
                break;
        }
    }

    @Override
    public void handle(AWrapperWorld world) {
        PartGun gun = world.getBulletGun(gunID);
        if (gun != null) {
            // Get the bullet item from the gun's loaded ammo on the client
            ItemBullet bulletItem = gun.lastLoadedBullet;
            if (bulletItem == null) {
                return; // Can't spawn without bullet data
            }

            gun.lastLoadedBullet = bulletItem;
            
            EntityBullet newBullet;
            switch (targetType) {
                case UUID:
                    newBullet = new EntityBullet(position, velocity, orientation, gun, bulletNumber, targetUUID);
                    break;
                case POSITION:
                    newBullet = new EntityBullet(position, velocity, orientation, gun, bulletNumber, targetPosition);
                    break;
                default:
                    newBullet = new EntityBullet(position, velocity, orientation, gun, bulletNumber);
                    break;
            }
            world.addEntity(newBullet);
        }
    }
}
