package mcinterface1211;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.instances.EntityPhysicsCube;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Builder for the physics cube entity.  This builder creates a simple cube entity
 * that can be spawned and interacted with for testing rigid body physics.
 *
 * @author clanka
 */
public class BuilderEntityPhysicsCube extends ABuilderEntityBase {
    public static DeferredHolder<EntityType<?>, EntityType<BuilderEntityPhysicsCube>> E_TYPE_PHYSICS_CUBE;
    private EntityDimensions mutableDims = EntityDimensions.scalable(1.0F, 1.0F);
    
    /**
     * Current entity we are built around.  This MAY be null if we haven't loaded NBT from the server yet.
     */
    protected EntityPhysicsCube entity;
    
    /**
     * Collective for collision boxes.  These are used by this entity to make things interact and attack it.
     */
    private net.minecraft.world.phys.AABB interactAttackBox;
    
    public BuilderEntityPhysicsCube(EntityType<? extends BuilderEntityPhysicsCube> eType, Level world) {
        super(eType, world);
    }
    
    @Override
    public void baseTick() {
        super.baseTick();
        
        //If our entity isn't null, update it and our position.
        if (entity != null) {
            //Check if we are still valid, or need to be set dead.
            if (!entity.isValid) {
                discard();
            } else {
                //Set the new position.
                setPos(entity.position.x, entity.position.y, entity.position.z);
                
                //Update AABB for interaction.
                if (interactAttackBox == null) {
                    BoundingBox box = entity.interactionBox;
                    interactAttackBox = new net.minecraft.world.phys.AABB(
                        box.globalCenter.x - box.widthRadius,
                        box.globalCenter.y - box.heightRadius,
                        box.globalCenter.z - box.depthRadius,
                        box.globalCenter.x + box.widthRadius,
                        box.globalCenter.y + box.heightRadius,
                        box.globalCenter.z + box.depthRadius
                    );
                    setBoundingBox(interactAttackBox);
                } else {
                    // Update the bounding box position
                    BoundingBox box = entity.interactionBox;
                    interactAttackBox = new net.minecraft.world.phys.AABB(
                        box.globalCenter.x - box.widthRadius,
                        box.globalCenter.y - box.heightRadius,
                        box.globalCenter.z - box.depthRadius,
                        box.globalCenter.x + box.widthRadius,
                        box.globalCenter.y + box.heightRadius,
                        box.globalCenter.z + box.depthRadius
                    );
                    setBoundingBox(interactAttackBox);
                }
            }
        } else {
            //If we have NBT, and haven't loaded it, do so now.
            if (!loadedFromSavedNBT && loadFromSavedNBT) {
                WrapperWorld worldWrapper = WrapperWorld.getWrapperFor(level());
                try {
                    WrapperNBT data = new WrapperNBT(lastLoadedNBT);
                    entity = new EntityPhysicsCube(worldWrapper, data);
                    entity.world.addEntity(entity);
                    loadedFromSavedNBT = true;
                    lastLoadedNBT = null;
                } catch (Exception e) {
                    InterfaceManager.coreInterface.logError("Failed to load physics cube entity from saved NBT.");
                    InterfaceManager.coreInterface.logError(e.getMessage());
                    discard();
                }
            }
        }
    }
    
    @Override
    public EntityDimensions getDimensions(Pose pPose) {
        return mutableDims;
    }
    
    @Override
    public void onRemovedFromLevel() {
        super.onRemovedFromLevel();
        //Notify internal entity of it being invalid.
        if (entity != null) {
            entity.remove();
        }
    }
    
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        lastLoadedNBT = tag;
        loadFromSavedNBT = true;
    }
    
    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (entity != null) {
            WrapperNBT data = new WrapperNBT(tag);
            entity.save(data);
        }
    }
}
