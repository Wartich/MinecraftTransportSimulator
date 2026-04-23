package minecrafttransportsimulator.entities.instances;

import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.Transform;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.PhysicsConversions;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.entities.components.AEntityC_Renderable;
import minecrafttransportsimulator.jsondefs.JSONCollisionGroup.CollisionType;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketPhysicsCubeSync;
import minecrafttransportsimulator.physics.BlockCollisionManager;
import minecrafttransportsimulator.physics.PhysicsWorld;

import javax.vecmath.Matrix3f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import java.util.HashSet;
import java.util.Set;

/**
 * Physics cube entity for testing rigid body dynamics.
 * This entity is a simple 1x1x1 cube that can be spawned and interacted with.
 * 
 * @author clanka
 */
public class EntityPhysicsCube extends AEntityC_Renderable {
    
    private static final double CUBE_SIZE = 1.0;
    private static final double CUBE_RADIUS = CUBE_SIZE / 2.0;
    private static final float MASS = 1.0f; // 1 kg
    private static final float RESTITUTION = 0.5f; // Bounciness
    
    public final BoundingBox interactionBox;
    
    // Physics components
    private PhysicsWorld physicsWorld;
    private RigidBody rigidBody;
    private CollisionShape collisionShape;
    private BlockCollisionManager blockCollisionManager;
    
    /**
     * Constructor for synced entities loaded from NBT
     */
    public EntityPhysicsCube(AWrapperWorld world, IWrapperNBT data) {
        super(world, data);
        
        // Create interaction bounding box
        Set<CollisionType> collisionTypes = new HashSet<>();
        collisionTypes.add(CollisionType.CLICK);
        collisionTypes.add(CollisionType.ATTACK);
        this.interactionBox = new BoundingBox(new Point3D(), position, CUBE_RADIUS, CUBE_RADIUS, CUBE_RADIUS, false, collisionTypes);
        
        // Initialize physics
        initPhysics();
    }
    
    /**
     * Constructor for new entities spawned in the world
     */
    public EntityPhysicsCube(AWrapperWorld world, Point3D position) {
        super(world, position, new Point3D(), new Point3D());
        
        // Create interaction bounding box
        Set<CollisionType> collisionTypes = new HashSet<>();
        collisionTypes.add(CollisionType.CLICK);
        collisionTypes.add(CollisionType.ATTACK);
        this.interactionBox = new BoundingBox(new Point3D(), this.position, CUBE_RADIUS, CUBE_RADIUS, CUBE_RADIUS, false, collisionTypes);
        
        // Initialize physics
        initPhysics();
    }
    
    /**
     * Initializes the physics components for this cube.
     */
    private void initPhysics() {
        // Only initialize physics on the server
        if (world.isClient()) {
            return;
        }
        
        // Create physics world if it doesn't exist
        // TODO: This should be managed globally, not per-entity
        physicsWorld = new PhysicsWorld();
        
        // Create box collision shape (half-extents)
        Vector3f halfExtents = new Vector3f((float)CUBE_RADIUS, (float)CUBE_RADIUS, (float)CUBE_RADIUS);
        collisionShape = new BoxShape(halfExtents);
        
        // Create initial transform
        Transform startTransform = new Transform();
        startTransform.setIdentity();
        PhysicsConversions.point3DToVector3f(position, startTransform.origin);
        PhysicsConversions.rotationMatrixToMatrix3f(orientation, startTransform.basis);
        
        System.out.println("PhysicsCube: Initial position: " + position);
        
        // Calculate local inertia
        Vector3f localInertia = new Vector3f(0, 0, 0);
        collisionShape.calculateLocalInertia(MASS, localInertia);
        
        // Create motion state
        DefaultMotionState motionState = new DefaultMotionState(startTransform);
        
        // Create rigid body
        RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(MASS, motionState, collisionShape, localInertia);
        rbInfo.restitution = RESTITUTION;
        rigidBody = new RigidBody(rbInfo);
        
        // Make sure the body is active and doesn't deactivate
        rigidBody.setActivationState(RigidBody.DISABLE_DEACTIVATION);
        rigidBody.activate(true);
        
        // Add to physics world
        physicsWorld.addRigidBody(rigidBody);
        
        // Create block collision manager (needs PhysicsWorld)
        blockCollisionManager = new BlockCollisionManager(world, physicsWorld);
        
        System.out.println("PhysicsCube: Physics initialized, mass=" + MASS + ", inertia=" + localInertia);
    }
    
    @Override
    public void update() {
        super.update();
        
        // Step physics simulation (20 TPS = 0.05 seconds per tick)
        if (!world.isClient()) {
            Point3D oldPos = position.copy();
            
            // Update block collision bodies around the cube
            blockCollisionManager.updateBlockBodies(position, 2.0);
            
            // Step physics - Bullet will handle all collisions
            physicsWorld.step(0.05f);
            
            // Sync physics transform to entity
            syncPhysicsToEntity();
            
            // Send position update to clients every tick
            InterfaceManager.packetInterface.sendToAllClients(new PacketPhysicsCubeSync(this));
            
            // Debug: print position change
            if (ticksExisted % 20 == 0) { // Every second
                System.out.println("PhysicsCube tick " + ticksExisted + ": pos=" + position + " (delta=" + position.distanceTo(oldPos) + ")");
                // Print angular velocity and orientation
                Vector3f angVel = rigidBody.getAngularVelocity(new Vector3f());
                System.out.println("  AngularVel: " + angVel + ", Orientation: [" + orientation.m00 + ", " + orientation.m01 + ", " + orientation.m02 + "]");
            }
        }
        
        // Update bounding box position to match entity
        interactionBox.globalCenter.set(position);
    }
    
    /**
     * Syncs the physics rigid body transform to the entity's position and orientation.
     */
    private void syncPhysicsToEntity() {
        Transform transform = new Transform();
        rigidBody.getMotionState().getWorldTransform(transform);
        
        // Convert position
        PhysicsConversions.vector3fToPoint3D(transform.origin, position);
        
        // Convert rotation
        PhysicsConversions.matrix3fToRotationMatrix(transform.basis, orientation);
    }
    
    /**
     * Cleanup physics resources when entity is removed.
     */
    @Override
    public void remove() {
        super.remove();
        if (physicsWorld != null && rigidBody != null) {
            physicsWorld.removeRigidBody(rigidBody);
            if (blockCollisionManager != null) {
                blockCollisionManager.clear();
            }
            physicsWorld.dispose();
        }
    }
    
    @Override
    public boolean shouldSync() {
        return true;
    }
    
    @Override
    public boolean shouldSavePosition() {
        return true;
    }
    
    /**
     * Called when a player interacts with this cube
     */
    public boolean interact(IWrapperPlayer player) {
        if (!world.isClient()) {
            // For now, just remove the cube when clicked
            this.isValid = false;
        }
        return true;
    }
    
    /**
     * Called when a player attacks this cube
     */
    public void attack(IWrapperPlayer player) {
        if (!world.isClient()) {
            System.out.println("PhysicsCube: Attacked by " + player.getName());
            
            // Get the player's look direction and position
            Point3D playerPos = player.getPosition();
            playerPos.y += player.getEyeHeight();
            
            // Calculate direction from player to cube
            Point3D direction = position.copy().subtract(playerPos);
            direction.normalize();
            
            // Apply impulse at the hit point
            // Raycast from player to find exact hit point on cube
            Point3D hitPoint = calculateHitPoint(playerPos, direction);
            
            System.out.println("PhysicsCube: Hit point=" + hitPoint + ", direction=" + direction);
            
            // Apply force (punch strength)
            float punchForce = 5.0f;
            Point3D impulse = direction.copy().scale(punchForce);
            
            applyImpulseAtPoint(impulse, hitPoint);
        }
    }
    
    /**
     * Calculates where the player's raycast hits the cube.
     */
    private Point3D calculateHitPoint(Point3D rayStart, Point3D rayDir) {
        // Simple AABB ray intersection
        // Find intersection with cube faces
        double tMin = Double.NEGATIVE_INFINITY;
        double tMax = Double.POSITIVE_INFINITY;
        
        Point3D cubeMin = position.copy().add(-CUBE_RADIUS, -CUBE_RADIUS, -CUBE_RADIUS);
        Point3D cubeMax = position.copy().add(CUBE_RADIUS, CUBE_RADIUS, CUBE_RADIUS);
        
        // Check each axis
        for (int i = 0; i < 3; i++) {
            double rayOrigin = i == 0 ? rayStart.x : (i == 1 ? rayStart.y : rayStart.z);
            double rayDirection = i == 0 ? rayDir.x : (i == 1 ? rayDir.y : rayDir.z);
            double boxMin = i == 0 ? cubeMin.x : (i == 1 ? cubeMin.y : cubeMin.z);
            double boxMax = i == 0 ? cubeMax.x : (i == 1 ? cubeMax.y : cubeMax.z);
            
            if (Math.abs(rayDirection) < 0.0001) {
                // Ray is parallel to slab
                if (rayOrigin < boxMin || rayOrigin > boxMax) {
                    return position.copy(); // No hit, return center
                }
            } else {
                double t1 = (boxMin - rayOrigin) / rayDirection;
                double t2 = (boxMax - rayOrigin) / rayDirection;
                
                if (t1 > t2) {
                    double temp = t1;
                    t1 = t2;
                    t2 = temp;
                }
                
                tMin = Math.max(tMin, t1);
                tMax = Math.min(tMax, t2);
                
                if (tMin > tMax) {
                    return position.copy(); // No hit, return center
                }
            }
        }
        
        // Calculate hit point
        double t = tMin > 0 ? tMin : tMax;
        return rayStart.copy().add(rayDir.x * t, rayDir.y * t, rayDir.z * t);
    }
    
    /**
     * Applies an impulse at a specific world-space point.
     * This creates both linear and angular motion.
     */
    public void applyImpulseAtPoint(Point3D impulse, Point3D worldPoint) {
        if (rigidBody == null) {
            System.out.println("PhysicsCube: Cannot apply impulse - rigidBody is null!");
            return;
        }
        
        // Convert to JBullet vectors
        Vector3f impulseVec = new Vector3f((float) impulse.x, (float) impulse.y, (float) impulse.z);
        Vector3f relativePos = new Vector3f(
            (float) (worldPoint.x - position.x),
            (float) (worldPoint.y - position.y),
            (float) (worldPoint.z - position.z)
        );
        
        System.out.println("PhysicsCube: Applying impulse=" + impulseVec + " at offset=" + relativePos);
        
        // Apply linear impulse
        rigidBody.applyCentralImpulse(impulseVec);
        
        // Calculate and apply angular impulse (torque = r × F)
        Vector3f torque = new Vector3f();
        torque.cross(relativePos, impulseVec);
        rigidBody.applyTorqueImpulse(torque);
        
        System.out.println("PhysicsCube: Applied torque=" + torque);
        
        // Wake up the body
        rigidBody.activate(true);
    }
    
    @Override
    public IWrapperNBT save(IWrapperNBT data) {
        super.save(data);
        // No additional data to save yet
        return data;
    }
    
    /**
     * Get the cube size for rendering
     */
    public double getCubeSize() {
        return CUBE_SIZE;
    }
    
    @Override
    protected void renderModel(TransformationMatrix transform, boolean blendingEnabled, float partialTicks) {
        // Rendering is handled by the builder's custom renderer
        // This method is required by AEntityC_Renderable but we don't use it
        // since we have a custom wireframe renderer in RenderPhysicsCube
    }
}
