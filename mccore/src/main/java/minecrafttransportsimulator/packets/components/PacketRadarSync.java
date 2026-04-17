package minecrafttransportsimulator.packets.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.EntityManager;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.entities.instances.EntityBullet;
import minecrafttransportsimulator.entities.instances.EntityBulletGhost;
import minecrafttransportsimulator.items.instances.ItemBullet;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;

/**
 * Packet used to sync radar contact data from server to client.
 * This allows radar to detect vehicles that are in loaded chunks but outside
 * client render distance.
 * 
 * @author don_bruce
 */
public class PacketRadarSync extends APacketBase {
    //Static map to track ghost bullets by real bullet UUID
    private static final Map<UUID, EntityBulletGhost> ghostBulletMap = new HashMap<>();
    
    private final UUID radarEntityUUID;
    private final Point3D radarPosition;
    private final List<RadarContactData> aircraftContacts;
    private final List<RadarContactData> grounderContacts;
    private final List<UUID> trackedVehicleUUIDs;
    //Missile/gun lock-on data - synced from server to client for missile_* variables
    private final List<MissileLockData> missilesIncomingData;
    private final int gunsLockedOnCount;

    // Private constructor - use factory methods instead
    private PacketRadarSync(UUID radarEntityUUID, Point3D radarPosition, List<RadarContactData> aircraftContacts, List<RadarContactData> grounderContacts, List<UUID> trackedVehicleUUIDs, List<MissileLockData> missilesIncomingData, int gunsLockedOnCount) {
        super(null);
        this.radarEntityUUID = radarEntityUUID;
        this.radarPosition = radarPosition;
        this.aircraftContacts = aircraftContacts;
        this.grounderContacts = grounderContacts;
        this.trackedVehicleUUIDs = trackedVehicleUUIDs;
        this.missilesIncomingData = missilesIncomingData;
        this.gunsLockedOnCount = gunsLockedOnCount;
    }
    
    // Factory method for GLOBAL packets (with full position data)
    public static PacketRadarSync createGlobalPacket(List<RadarContactData> aircraftContacts, List<RadarContactData> grounderContacts) {
        return new PacketRadarSync(null, null, aircraftContacts, grounderContacts, new ArrayList<>(), new ArrayList<>(), 0);
    }
    
    // Factory method for PER-RADAR packets (only UUIDs, clients look up positions from global cache)
    public static PacketRadarSync createRadarPacket(UUID radarEntityUUID, Point3D radarPosition, List<UUID> aircraftUUIDs, List<UUID> grounderUUIDs, List<UUID> trackedVehicleUUIDs, List<MissileLockData> missilesIncomingData, int gunsLockedOnCount) {
        // Convert UUIDs to RadarContactData with null positions (will be looked up from cache)
        List<RadarContactData> aircraftContacts = new ArrayList<>();
        for (UUID uuid : aircraftUUIDs) {
            aircraftContacts.add(new RadarContactData(uuid, null, 0, null));
        }
        List<RadarContactData> grounderContacts = new ArrayList<>();
        for (UUID uuid : grounderUUIDs) {
            grounderContacts.add(new RadarContactData(uuid, null, 0, null));
        }
        return new PacketRadarSync(radarEntityUUID, radarPosition, aircraftContacts, grounderContacts, trackedVehicleUUIDs, missilesIncomingData, gunsLockedOnCount);
    }
    
    // Factory method for BULLET SELF-SYNC packets (isLongRange bullets syncing their hit data)
    public static PacketRadarSync createBulletSyncPacket(UUID bulletUUID, Point3D bulletPosition, List<MissileLockData> missileData) {
        return new PacketRadarSync(bulletUUID, bulletPosition, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), missileData, 0);
    }

    public PacketRadarSync(ByteBuf buf) {
        super(buf);
        
        //Read flag to check if this is a global packet
        boolean isGlobalPacket = buf.readBoolean();
        
        if (isGlobalPacket) {
            this.radarEntityUUID = null;
            this.radarPosition = null;
        } else {
            this.radarEntityUUID = readUUIDFromBuffer(buf);
            this.radarPosition = readPoint3dFromBuffer(buf);
        }

        int aircraftCount = buf.readInt();
        this.aircraftContacts = new ArrayList<>(aircraftCount);
        for (int i = 0; i < aircraftCount; i++) {
            UUID uuid = readUUIDFromBuffer(buf);
            boolean hasPositionData = buf.readBoolean();
            if (hasPositionData) {
                // Global packet - has full position data
                aircraftContacts.add(new RadarContactData(uuid, readPoint3dFromBuffer(buf), buf.readDouble(), readPoint3dFromBuffer(buf)));
            } else {
                // Per-radar packet - only UUID (position looked up from cache)
                aircraftContacts.add(new RadarContactData(uuid, null, 0, null));
            }
        }
        
        int grounderCount = buf.readInt();
        this.grounderContacts = new ArrayList<>(grounderCount);
        for (int i = 0; i < grounderCount; i++) {
            UUID uuid = readUUIDFromBuffer(buf);
            boolean hasPositionData = buf.readBoolean();
            if (hasPositionData) {
                // Global packet - has full position data
                grounderContacts.add(new RadarContactData(uuid, readPoint3dFromBuffer(buf), buf.readDouble(), readPoint3dFromBuffer(buf)));
            } else {
                // Per-radar packet - only UUID (position looked up from cache)
                grounderContacts.add(new RadarContactData(uuid, null, 0, null));
            }
        }

        int trackedCount = buf.readInt();
        this.trackedVehicleUUIDs = new ArrayList<>(trackedCount);
        for (int i = 0; i < trackedCount; i++) {
            trackedVehicleUUIDs.add(readUUIDFromBuffer(buf));
        }

        //Read missile lock-on data
        int missileCount = buf.readInt();
        this.missilesIncomingData = new ArrayList<>(missileCount);
        for (int i = 0; i < missileCount; i++) {
            UUID uuid = readUUIDFromBuffer(buf);
            Point3D position = readPoint3dFromBuffer(buf);
            Point3D motion = readPoint3dFromBuffer(buf);
            RotationMatrix orientation = new RotationMatrix();
            orientation.angles.x = buf.readDouble();
            orientation.angles.y = buf.readDouble();
            orientation.angles.z = buf.readDouble();
            orientation.updateToAngles();
            double targetDistance = buf.readDouble();
            ItemBullet bulletItem = readItemFromBuffer(buf);
            long ticksExisted = buf.readLong();
            EntityBullet.HitType lastHit = buf.readBoolean() ? EntityBullet.HitType.values()[buf.readByte()] : null;
            Axis sideHit = buf.readBoolean() ? Axis.values()[buf.readByte()] : null;
            missilesIncomingData.add(new MissileLockData(uuid, position, motion, orientation, targetDistance, bulletItem, ticksExisted, lastHit, sideHit));
        }

        //Read guns locked on count
        this.gunsLockedOnCount = buf.readInt();
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        
        //Write flag to indicate if this is a global packet (radarEntityUUID is null)
        boolean isGlobalPacket = (radarEntityUUID == null);
        buf.writeBoolean(isGlobalPacket);
        
        if (!isGlobalPacket) {
            writeUUIDToBuffer(radarEntityUUID, buf);
            writePoint3dToBuffer(radarPosition, buf);
        }

        buf.writeInt(aircraftContacts.size());
        for (RadarContactData contact : aircraftContacts) {
            writeUUIDToBuffer(contact.uuid, buf);
            // For per-radar packets, position is null (only UUID sent)
            // For global packets, position has data
            boolean hasPositionData = (contact.position != null);
            buf.writeBoolean(hasPositionData);
            if (hasPositionData) {
                writePoint3dToBuffer(contact.position, buf);
                buf.writeDouble(contact.velocity);
                writePoint3dToBuffer(contact.motion, buf);
            }
        }
        
        buf.writeInt(grounderContacts.size());
        for (RadarContactData contact : grounderContacts) {
            writeUUIDToBuffer(contact.uuid, buf);
            boolean hasPositionData = (contact.position != null);
            buf.writeBoolean(hasPositionData);
            if (hasPositionData) {
                writePoint3dToBuffer(contact.position, buf);
                buf.writeDouble(contact.velocity);
                writePoint3dToBuffer(contact.motion, buf);
            }
        }

        buf.writeInt(trackedVehicleUUIDs.size());
        for (UUID trackedUUID : trackedVehicleUUIDs) {
            writeUUIDToBuffer(trackedUUID, buf);
        }

        //Write missile lock-on data
        buf.writeInt(missilesIncomingData.size());
        for (MissileLockData missile : missilesIncomingData) {
            writeUUIDToBuffer(missile.uuid, buf);
            writePoint3dToBuffer(missile.position, buf);
            writePoint3dToBuffer(missile.motion, buf);
            buf.writeDouble(missile.orientation.angles.x);
            buf.writeDouble(missile.orientation.angles.y);
            buf.writeDouble(missile.orientation.angles.z);
            buf.writeDouble(missile.targetDistance);
            writeItemToBuffer(missile.bulletItem, buf);
            buf.writeLong(missile.ticksExisted);
            buf.writeBoolean(missile.lastHit != null);
            if (missile.lastHit != null) {
                buf.writeByte(missile.lastHit.ordinal());
            }
            buf.writeBoolean(missile.sideHit != null);
            if (missile.sideHit != null) {
                buf.writeByte(missile.sideHit.ordinal());
            }
        }

        //Write guns locked on count
        buf.writeInt(gunsLockedOnCount);
    }

    @Override
    public void handle(AWrapperWorld world) {
        //Check if this is global vehicle data (radarEntityUUID is null)
        if (radarEntityUUID == null) {
            //Global vehicle position sync - update world's global vehicle cache
            //This allows guns without radars to lock distant targets
            long currentTime = world.getTime();
            
            //Update cache with aircraft data
            for (RadarContactData contact : aircraftContacts) {
                EntityManager.GlobalVehicleData data = new EntityManager.GlobalVehicleData(
                    contact.uuid,
                    contact.position.copy(),
                    contact.motion.copy(),
                    true, // isAircraft
                    currentTime
                );
                world.globalVehicleCache.put(contact.uuid, data);
            }
            
            //Update cache with grounder data
            for (RadarContactData contact : grounderContacts) {
                EntityManager.GlobalVehicleData data = new EntityManager.GlobalVehicleData(
                    contact.uuid,
                    contact.position.copy(),
                    contact.motion.copy(),
                    false, // isAircraft
                    currentTime
                );
                world.globalVehicleCache.put(contact.uuid, data);
            }
            
            //Clean up stale entries (not updated in last 20 ticks / 1 second)
            world.globalVehicleCache.entrySet().removeIf(
                entry -> currentTime - entry.getValue().lastUpdateTick >= 20
            );
            
            return;
        }
        
        //Find the radar entity and update its radar contacts (per-radar data for RWR)
        AEntityD_Definable<?> entity = world.getEntity(radarEntityUUID);
        if (entity != null) {
            entity.setRadarContacts(aircraftContacts, grounderContacts);
            entity.setMissileContacts(missilesIncomingData, gunsLockedOnCount);
        } else if (!missilesIncomingData.isEmpty() && world.isClient()) {
            //No entity found, but we have missile data
            //This happens when bullets self-sync (artillery, etc.)
            //Create/update ghost bullets directly in the world
            //Only do this on client side
            for (MissileLockData missileData : missilesIncomingData) {
                //Check if we already have a ghost for this bullet UUID
                EntityBulletGhost ghost = ghostBulletMap.get(missileData.uuid);
                
                if (ghost != null && ghost.isValid) {
                    //Update existing ghost
                    ghost.onPositionSync(missileData.position, missileData.motion);
                    ghost.orientation.set(missileData.orientation);
                    ghost.ticksExisted = missileData.ticksExisted;
                    ghost.lastHit = missileData.lastHit;
                    ghost.sideHit = missileData.sideHit;
                } else {
                    //Check if bullet is within client render distance
                    //If it is, the client will receive PacketPartGun.LONG_RANGE_BULLET_SPAWN and create the real bullet
                    //So we shouldn't create a ghost
                    IWrapperPlayer clientPlayer = InterfaceManager.clientInterface.getClientPlayer();
                    double distanceToClient = clientPlayer.getPosition().distanceTo(missileData.position);
                    //Use 128 blocks as render distance threshold (typical entity render distance)
                    boolean withinRenderDistance = distanceToClient < 128;
                    
                    if (!withinRenderDistance) {
                        //Beyond render distance - create ghost for rendering
                        ghost = new EntityBulletGhost(world, missileData.position.copy(), missileData.motion.copy(), missileData.orientation, missileData.bulletItem);
                        ghost.ticksExisted = missileData.ticksExisted;
                        ghost.lastHit = missileData.lastHit;
                        ghost.sideHit = missileData.sideHit;
                        world.addEntity(ghost);
                        ghostBulletMap.put(missileData.uuid, ghost);
                    }
                    //If within render distance, don't create ghost - real bullet will spawn via PacketPartGun
                }
            }
            
            //Clean up invalid ghosts from map
            ghostBulletMap.entrySet().removeIf(entry -> !entry.getValue().isValid);
        }

        //Update tracking info for each tracked vehicle
        for (UUID trackedUUID : trackedVehicleUUIDs) {
            AEntityD_Definable<?> trackedVehicle = world.getEntity(trackedUUID);
            if (trackedVehicle != null) {
                trackedVehicle.addRadarTrackingThis(radarEntityUUID, radarPosition);
            }
        }
    }
    
    /**
     * Simple data class to hold radar contact information.
     * Used to sync data from server to client without needing full entity references.
     */
    public static class RadarContactData {
        public final UUID uuid;
        public final Point3D position;
        public final double velocity;
        public final Point3D motion;

        public RadarContactData(UUID uuid, Point3D position, double velocity, Point3D motion) {
            this.uuid = uuid;
            this.position = position;
            this.velocity = velocity;
            this.motion = motion;
        }
    }

    /**
     * Simple data class to hold missile lock-on information.
     * Used to sync missile data from server to client for missile_* variables.
     */
    public static class MissileLockData {
        public final UUID uuid;
        public final Point3D position;
        public final Point3D motion;
        public final RotationMatrix orientation;
        public final double targetDistance;
        public final ItemBullet bulletItem;
        public final long ticksExisted;
        public final EntityBullet.HitType lastHit;
        public final Axis sideHit;

        public MissileLockData(UUID uuid, Point3D position, Point3D motion, RotationMatrix orientation, double targetDistance, ItemBullet bulletItem, long ticksExisted, EntityBullet.HitType lastHit, Axis sideHit) {
            this.uuid = uuid;
            this.position = position;
            this.motion = motion;
            this.orientation = orientation;
            this.targetDistance = targetDistance;
            this.bulletItem = bulletItem;
            this.ticksExisted = ticksExisted;
            this.lastHit = lastHit;
            this.sideHit = sideHit;
        }
    }
}