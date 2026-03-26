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
    
    public EntityBulletGhost(AWrapperWorld world, Point3D position, Point3D motion, RotationMatrix orientation, ItemBullet bulletItem) {
        super(world, position, motion, new Point3D(), bulletItem);
        this.orientation.set(orientation);
    }

    @Override
    public void update() {
        super.update();
        //Ghost bullets don't do physics or collision - position is updated externally
        //Just update particles and sounds via the parent class
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
