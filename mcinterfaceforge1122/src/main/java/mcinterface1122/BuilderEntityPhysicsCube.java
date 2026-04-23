package mcinterface1122;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.entities.instances.EntityPhysicsCube;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;

/**
 * Builder for the physics cube entity.  This builder creates a simple cube entity
 * that can be spawned and interacted with for testing rigid body physics.
 *
 * @author clanka
 */
@EventBusSubscriber
public class BuilderEntityPhysicsCube extends ABuilderEntityBase {
    
    /**
     * Current entity we are built around.  This MAY be null if we haven't loaded NBT from the server yet.
     */
    protected EntityPhysicsCube entity;
    
    /**
     * Bounding box for interaction.
     */
    private AxisAlignedBB interactAttackBox;
    
    public BuilderEntityPhysicsCube(World world) {
        super(world);
    }
    
    @Override
    public void onEntityUpdate() {
        super.onEntityUpdate();
        
        //If our entity isn't null, update it and our position.
        if (entity != null) {
            //Check if we are still valid, or need to be set dead.
            if (!entity.isValid) {
                setDead();
            } else {
                //Set the new position directly to force tracker updates
                posX = entity.position.x;
                posY = entity.position.y;
                posZ = entity.position.z;
                
                // Debug: print position on client
                if (world.isRemote && entity.ticksExisted % 20 == 0) {
                    System.out.println("BuilderPhysicsCube CLIENT tick " + entity.ticksExisted + ": builder pos=(" + posX + ", " + posY + ", " + posZ + "), entity pos=" + entity.position);
                    // Print first row of rotation matrix to see if it's changing
                    System.out.println("  Orientation: [" + entity.orientation.m00 + ", " + entity.orientation.m01 + ", " + entity.orientation.m02 + "]");
                }
                
                //Update AABB for interaction.
                BoundingBox box = entity.interactionBox;
                interactAttackBox = new AxisAlignedBB(
                    box.globalCenter.x - box.widthRadius,
                    box.globalCenter.y - box.heightRadius,
                    box.globalCenter.z - box.depthRadius,
                    box.globalCenter.x + box.widthRadius,
                    box.globalCenter.y + box.heightRadius,
                    box.globalCenter.z + box.depthRadius
                );
                setEntityBoundingBox(interactAttackBox);
            }
        } else {
            //If we have NBT, and haven't loaded it, do so now.
            if (!loadedFromSavedNBT && loadFromSavedNBT) {
                WrapperWorld worldWrapper = WrapperWorld.getWrapperFor(world);
                try {
                    WrapperNBT data = new WrapperNBT(lastLoadedNBT);
                    entity = new EntityPhysicsCube(worldWrapper, data);
                    entity.world.addEntity(entity);
                    loadedFromSavedNBT = true;
                    lastLoadedNBT = null;
                } catch (Exception e) {
                    InterfaceManager.coreInterface.logError("Failed to load physics cube entity from saved NBT.");
                    InterfaceManager.coreInterface.logError(e.getMessage());
                    setDead();
                }
            }
        }
    }
    
    @Override
    public void setDead() {
        super.setDead();
        //Notify internal entity of it being invalid.
        if (entity != null) {
            entity.remove();
        }
    }
    
    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (!world.isRemote && entity != null) {
            Entity attacker = source.getTrueSource();
            if (attacker instanceof EntityPlayer) {
                System.out.println("PhysicsCube: Attacked by player!");
                entity.attack(WrapperPlayer.getWrapperFor((EntityPlayer) attacker));
                return true;
            }
        }
        return false;
    }
    
    @Override
    public boolean processInitialInteract(EntityPlayer player, net.minecraft.util.EnumHand hand) {
        if (!world.isRemote && entity != null) {
            System.out.println("PhysicsCube: Right-clicked by player!");
            return entity.interact(WrapperPlayer.getWrapperFor(player));
        }
        return false;
    }
    
    @Override
    public boolean canBeCollidedWith() {
        // Make the entity collidable so players can interact with it
        return true;
    }
    
    @Override
    public boolean canBePushed() {
        // Prevent vanilla pushing mechanics
        return false;
    }
    
    @Override
    public boolean shouldRenderInPass(int pass) {
        // Render the physics cube in the normal render pass
        return pass == 0;
    }
    
    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        lastLoadedNBT = tag;
        loadFromSavedNBT = true;
    }
    
    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        if (entity != null) {
            WrapperNBT data = new WrapperNBT(tag);
            entity.save(data);
        }
    }
    
    /**
     * Called to register this entity.
     */
    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        event.getRegistry().register(EntityEntryBuilder.create().entity(BuilderEntityPhysicsCube.class).id(new ResourceLocation(InterfaceManager.coreModID, "mts_physics_cube"), 3).tracker(32 * 16, 1, true).name("mts_physics_cube").build());
    }
}
