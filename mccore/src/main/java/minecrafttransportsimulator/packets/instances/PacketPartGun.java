package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.entities.instances.EntityBullet;
import minecrafttransportsimulator.entities.instances.PartGun;
import minecrafttransportsimulator.items.instances.ItemBullet;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Packet used to send signals to guns.  This can be either to change the state of the gun,
 * or to re-load the gun with the specified bullets.  If we are doing state commands, then
 * this packet first gets sent to the server from the client who requested the command.  After this,
 * it is send to all players tracking the gun (if applicable).  If this packet is for re-loading bullets, then it will
 * only appear on clients after the server has verified the bullets can in fact be loaded.
 *
 * @author don_bruce
 */
public class PacketPartGun extends APacketEntity<PartGun> {
    private final Request stateRequest;
    private final ItemBullet bulletItem;
    private final int bulletQty;

    // For LONG_RANGE_BULLET_SPAWN request
    private final Point3D spawnPosition;
    private final Point3D spawnVelocity;
    private final RotationMatrix spawnOrientation;
    private final int bulletNumber;

    public PacketPartGun(PartGun gun, Request stateRequest) {
        super(gun);
        this.stateRequest = stateRequest;
        this.bulletItem = null;
        this.bulletQty = 0;
        this.spawnPosition = null;
        this.spawnVelocity = null;
        this.spawnOrientation = null;
        this.bulletNumber = 0;
    }

    public PacketPartGun(PartGun gun, ItemBullet bullet, int bulletQty) {
        super(gun);
        this.stateRequest = Request.RELOAD_ONCLIENT;
        this.bulletItem = bullet;
        this.bulletQty = bulletQty;
        this.spawnPosition = null;
        this.spawnVelocity = null;
        this.spawnOrientation = null;
        this.bulletNumber = 0;
    }

    public PacketPartGun(PartGun gun, Point3D position, Point3D velocity, RotationMatrix orientation, int bulletNumber) {
        super(gun);
        this.stateRequest = Request.LONG_RANGE_BULLET_SPAWN;
        this.bulletItem = null;
        this.bulletQty = 0;
        this.spawnPosition = position;
        this.spawnVelocity = velocity;
        this.spawnOrientation = orientation;
        this.bulletNumber = bulletNumber;
    }

    public PacketPartGun(ByteBuf buf) {
        super(buf);
        this.stateRequest = Request.values()[buf.readByte()];
        if (stateRequest == Request.RELOAD_ONCLIENT) {
            this.bulletItem = readItemFromBuffer(buf);
            this.bulletQty = buf.readInt();
            this.spawnPosition = null;
            this.spawnVelocity = null;
            this.spawnOrientation = null;
            this.bulletNumber = 0;
        } else if (stateRequest == Request.LONG_RANGE_BULLET_SPAWN) {
            this.bulletItem = null;
            this.bulletQty = 0;
            this.spawnPosition = readPoint3dFromBuffer(buf);
            this.spawnVelocity = readPoint3dFromBuffer(buf);
            this.spawnOrientation = new RotationMatrix();
            spawnOrientation.angles.x = buf.readDouble();
            spawnOrientation.angles.y = buf.readDouble();
            spawnOrientation.angles.z = buf.readDouble();
            this.bulletNumber = buf.readInt();
        } else {
            this.bulletItem = null;
            this.bulletQty = 0;
            this.spawnPosition = null;
            this.spawnVelocity = null;
            this.spawnOrientation = null;
            this.bulletNumber = 0;
        }
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        buf.writeByte(stateRequest.ordinal());
        if (stateRequest == Request.RELOAD_ONCLIENT) {
            writeItemToBuffer(bulletItem, buf);
            buf.writeInt(bulletQty);
        } else if (stateRequest == Request.LONG_RANGE_BULLET_SPAWN) {
            writePoint3dToBuffer(spawnPosition, buf);
            writePoint3dToBuffer(spawnVelocity, buf);
            buf.writeDouble(spawnOrientation.angles.x);
            buf.writeDouble(spawnOrientation.angles.y);
            buf.writeDouble(spawnOrientation.angles.z);
            buf.writeInt(bulletNumber);
        }
    }

    @Override
    public boolean handle(AWrapperWorld world, PartGun gun) {
        switch (stateRequest) {
            case CLEAR_ONCLIENT: {
                gun.clearBullets();
                break;
            }
            case RELOAD_ONCLIENT: {
                gun.setReloadVars(bulletItem, bulletQty);
                break;
            }
            case RELOAD_HAND: {
                gun.isHandHeldGunReloadRequested = true;
                break;
            }
            case TRIGGER_ON: {
                gun.playerHoldingTrigger = true;
                gun.playerPressedTrigger = true;
                break;
            }
            case TRIGGER_OFF: {
                gun.playerHoldingTrigger = false;
                break;
            }
            case AIM_ON: {
                gun.isHandHeldGunAimed = true;
                break;
            }
            case AIM_OFF: {
                gun.isHandHeldGunAimed = false;
                break;
            }
            case BULLETS_OUT: {
                gun.bulletsPresentOnServer = false;
                break;
            }
            case BULLETS_PRESENT: {
                gun.bulletsPresentOnServer = true;
                break;
            }
            case HANDHELD_MOVEMENTS: {
                gun.performGunHandheldMovements();
                break;
            }
            case LONG_RANGE_BULLET_SPAWN: {
                // Spawn the bullet on client for rendering
                // The bullet item comes from the gun's loaded ammo
                ItemBullet bulletItem = gun.lastLoadedBullet;
                if (bulletItem != null) {
                    gun.lastLoadedBullet = bulletItem;
                    EntityBullet newBullet = new EntityBullet(spawnPosition, spawnVelocity, spawnOrientation, gun, bulletNumber);
                    world.addEntity(newBullet);
                }
                break;
            }
        }
        return stateRequest.sendToClients;
    }

    public static enum Request {
        CLEAR_ONCLIENT(false),
        RELOAD_ONCLIENT(false),
        RELOAD_HAND(false),
        TRIGGER_ON(true),
        TRIGGER_OFF(true),
        AIM_ON(true),
        AIM_OFF(true),
        BULLETS_OUT(false),
        BULLETS_PRESENT(false),
        HANDHELD_MOVEMENTS(true),
        LONG_RANGE_BULLET_SPAWN(false);

        private final boolean sendToClients;

        private Request(boolean sendToClients) {
            this.sendToClients = sendToClients;
        }
    }
}
