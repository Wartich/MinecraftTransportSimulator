package minecrafttransportsimulator.physics;

import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.narrowphase.ManifoldPoint;
import com.bulletphysics.collision.narrowphase.PersistentManifold;
import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.Transform;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;

import javax.vecmath.Vector3f;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles collision between physics objects and Minecraft blocks.
 * Uses Bullet's collision detection to add blocks as static collision objects.
 * 
 * @author clanka
 */
public class BlockCollisionManager {
    private final AWrapperWorld world;
    private final PhysicsWorld physicsWorld;
    
    // Block physics properties
    private static final float BLOCK_FRICTION = 0.7f; // How much blocks resist sliding (0 = ice, 1+ = rubber)
    private static final float BLOCK_RESTITUTION = 0.3f; // How bouncy blocks are (0 = no bounce, 1 = perfect bounce)
    
    // Cache of block positions to their rigid bodies
    private final Map<Long, RigidBody> blockBodies = new HashMap<>();
    
    // Reusable objects to avoid allocations
    private final Point3D tempPosition = new Point3D();
    
    public BlockCollisionManager(AWrapperWorld world, PhysicsWorld physicsWorld) {
        this.world = world;
        this.physicsWorld = physicsWorld;
    }
    
    /**
     * Updates the block collision bodies around a position.
     * Adds/removes static rigid bodies for blocks as needed.
     * 
     * @param position Center position to check around
     * @param radius Radius to check for blocks
     */
    public void updateBlockBodies(Point3D position, double radius) {
        int minX = (int) Math.floor(position.x - radius);
        int maxX = (int) Math.ceil(position.x + radius);
        int minY = (int) Math.floor(position.y - radius);
        int maxY = (int) Math.ceil(position.y + radius);
        int minZ = (int) Math.floor(position.z - radius);
        int maxZ = (int) Math.ceil(position.z + radius);
        
        // Check each block in range
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Point3D blockPos = new Point3D(x, y, z);
                    long key = packPosition(x, y, z);
                    
                    List<BoundingBox> blockBoxes = world.getBlockCollisionBoxes(blockPos);
                    
                    if (blockBoxes != null && !blockBoxes.isEmpty()) {
                        // Block has collision - ensure it has a rigid body
                        if (!blockBodies.containsKey(key)) {
                            addBlockBody(blockPos, blockBoxes, key);
                        }
                    } else {
                        // No collision - remove body if it exists
                        removeBlockBody(key);
                    }
                }
            }
        }
    }
    
    /**
     * Adds a static rigid body for a block.
     */
    private void addBlockBody(Point3D blockPos, List<BoundingBox> blockBoxes, long key) {
        // For now, use the first bounding box
        // TODO: Handle multiple boxes per block (stairs, fences, etc.)
        BoundingBox box = blockBoxes.get(0);
        
        // Create box shape
        Vector3f halfExtents = new Vector3f(
            (float) box.widthRadius,
            (float) box.heightRadius,
            (float) box.depthRadius
        );
        CollisionShape shape = new BoxShape(halfExtents);
        
        // Create transform at block center
        Transform transform = new Transform();
        transform.setIdentity();
        transform.origin.set(
            (float) box.globalCenter.x,
            (float) box.globalCenter.y,
            (float) box.globalCenter.z
        );
        
        // Create static rigid body (mass = 0)
        RigidBody body = new RigidBody(0, null, shape, new Vector3f(0, 0, 0));
        body.setWorldTransform(transform);
        body.setFriction(BLOCK_FRICTION);
        body.setRestitution(BLOCK_RESTITUTION);
        
        // Disable deactivation for static bodies
        body.setActivationState(CollisionObject.DISABLE_DEACTIVATION);
        
        // Add to physics world
        physicsWorld.addRigidBody(body);
        blockBodies.put(key, body);
    }
    
    /**
     * Removes a block's rigid body.
     */
    private void removeBlockBody(long key) {
        RigidBody body = blockBodies.remove(key);
        if (body != null) {
            physicsWorld.removeRigidBody(body);
        }
    }
    
    /**
     * Packs block coordinates into a long for use as a map key.
     */
    private long packPosition(int x, int y, int z) {
        return ((long) x & 0x3FFFFF) | (((long) y & 0xFFF) << 22) | (((long) z & 0x3FFFFF) << 34);
    }
    
    /**
     * Clears all block bodies.
     */
    public void clear() {
        for (RigidBody body : blockBodies.values()) {
            physicsWorld.removeRigidBody(body);
        }
        blockBodies.clear();
    }
}
