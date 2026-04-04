package minecrafttransportsimulator.entities.instances;

import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.items.instances.ItemBullet;
import minecrafttransportsimulator.jsondefs.JSONBullet;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;

/**
 * Ghost bullet entity for rendering isLongRange bullets on clients beyond render distance.
 * This entity doesn't do physics or collision checks - it just renders the bullet model,
 * particles, and sounds at a position synced from the server via radar system.
 * 
 * @author don_bruce
 */
public class EntityBulletGhost extends AEntityD_Definable<JSONBullet> {
    //Synced state from real bullet
    public EntityBullet.HitType lastHit;
    public Axis sideHit;
    
    //For smooth interpolation between sync updates
    private final Point3D lastSyncPosition = new Point3D();
    private final Point3D lastSyncMotion = new Point3D();
    private long lastSyncTick = 0;
    
    //Despawn timer after impact
    private int impactDespawnTimer = -1;
    
    public EntityBulletGhost(AWrapperWorld world, Point3D position, Point3D motion, RotationMatrix orientation, ItemBullet bulletItem) {
        super(world, position, motion, new Point3D(), bulletItem);
        this.orientation.set(orientation);
        this.lastSyncPosition.set(position);
        this.lastSyncMotion.set(motion);
        this.lastSyncTick = ticksExisted;
    }

    @Override
    public void update() {
        super.update();
        
        //Ghost bullets don't do physics or collision - but they DO interpolate position
        //between sync updates for smooth movement
        if (world.isClient()) {
            //If bullet has hit something, start despawn timer
            if (lastHit != null) {
                //Start despawn timer if not already started
                if (impactDespawnTimer < 0) {
                    impactDespawnTimer = definition.bullet.impactDespawnTime;
                }
                //Count down and remove when timer expires
                if (impactDespawnTimer-- == 0) {
                    remove();
                }
                return;
            }
            
            //Calculate ticks since last sync
            long ticksSinceSync = ticksExisted - lastSyncTick;
            
            //If we haven't received a sync in a while (>60 ticks = 3 seconds), remove ghost
            if (ticksSinceSync > 60) {
                remove();
                return;
            }
            
            //Interpolate position based on last synced motion
            //This makes bullets move smoothly between 1-second sync updates
            if (ticksSinceSync > 0 && ticksSinceSync < 20) {
                position.set(lastSyncPosition).add(lastSyncMotion.copy().scale(ticksSinceSync));
            }
        }
    }
    
    /**
     * Called when position is synced from server.
     * Updates the interpolation base values.
     */
    public void onPositionSync(Point3D newPosition, Point3D newMotion) {
        this.lastSyncPosition.set(newPosition);
        this.lastSyncMotion.set(newMotion);
        this.lastSyncTick = ticksExisted;
        this.position.set(newPosition);
        this.motion.set(newMotion);
    }

    @Override
    public ComputedVariable createComputedVariable(String variable, boolean createDefaultIfNotPresent) {
        //Override bullet-specific variables to use synced state
        switch (variable) {
            case ("bullet_hit"):
                return new ComputedVariable(this, variable, partialTicks -> lastHit != null ? 1 : 0, false);
            case ("bullet_burntime"):
                return new ComputedVariable(this, variable, partialTicks -> ticksExisted > definition.bullet.burnTime ? 0 : definition.bullet.burnTime - ticksExisted, false);
            case ("bullet_hit_block"):
                return new ComputedVariable(this, variable, partialTicks -> EntityBullet.HitType.BLOCK == lastHit ? 1 : 0, false);
            case ("bullet_hit_entity"):
                return new ComputedVariable(this, variable, partialTicks -> EntityBullet.HitType.ENTITY == lastHit ? 1 : 0, false);
            case ("bullet_hit_vehicle"):
                return new ComputedVariable(this, variable, partialTicks -> EntityBullet.HitType.VEHICLE == lastHit ? 1 : 0, false);
            case ("bullet_hit_armor"):
                return new ComputedVariable(this, variable, partialTicks -> EntityBullet.HitType.ARMOR == lastHit ? 1 : 0, false);
            case ("bullet_hit_burst"):
                return new ComputedVariable(this, variable, partialTicks -> EntityBullet.HitType.BURST == lastHit ? 1 : 0, false);
            default:
                return super.createComputedVariable(variable, createDefaultIfNotPresent);
        }
    }

    @Override
    public boolean requiresDeltaUpdates() {
        return true;
    }

    @Override
    public boolean shouldSync() {
        return false; //Never sync - position comes from radar stubs
    }

    @Override
    public boolean shouldSavePosition() {
        return false; //Never save
    }

    @Override
    public IWrapperNBT save(IWrapperNBT data) {
        super.save(data);
        //Don't save ghost bullets to world
        return data;
    }
}
