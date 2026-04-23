package minecrafttransportsimulator.physics;

import com.bulletphysics.collision.broadphase.BroadphaseInterface;
import com.bulletphysics.collision.broadphase.DbvtBroadphase;
import com.bulletphysics.collision.dispatch.CollisionConfiguration;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.DynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.constraintsolver.ConstraintSolver;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;

import javax.vecmath.Vector3f;

/**
 * Manages the JBullet physics world for dynamic physics simulation.
 * Handles world creation, stepping, and rigid body management.
 * 
 * @author clanka
 */
public class PhysicsWorld {
    private final DynamicsWorld dynamicsWorld;
    private final CollisionConfiguration collisionConfiguration;
    private final CollisionDispatcher dispatcher;
    private final BroadphaseInterface broadphase;
    private final ConstraintSolver solver;
    
    /**
     * Creates a new physics world with standard gravity (-9.8 m/s^2 on Y axis).
     */
    public PhysicsWorld() {
        // Collision configuration
        collisionConfiguration = new DefaultCollisionConfiguration();
        
        // Collision dispatcher
        dispatcher = new CollisionDispatcher(collisionConfiguration);
        
        // Broadphase for collision detection optimization
        broadphase = new DbvtBroadphase();
        
        // Constraint solver
        solver = new SequentialImpulseConstraintSolver();
        
        // Create dynamics world
        dynamicsWorld = new DiscreteDynamicsWorld(dispatcher, broadphase, solver, collisionConfiguration);
        
        // Set gravity (Minecraft: -9.8 m/s^2 on Y axis)
        dynamicsWorld.setGravity(new Vector3f(0, -9.8f, 0));
    }
    
    /**
     * Steps the physics simulation forward by the given time step.
     * 
     * @param timeStep Time step in seconds (typically 0.05 for 20 TPS)
     */
    public void step(float timeStep) {
        dynamicsWorld.stepSimulation(timeStep, 10);
    }
    
    /**
     * Adds a rigid body to the physics world.
     * 
     * @param body The rigid body to add
     */
    public void addRigidBody(RigidBody body) {
        dynamicsWorld.addRigidBody(body);
    }
    
    /**
     * Removes a rigid body from the physics world.
     * 
     * @param body The rigid body to remove
     */
    public void removeRigidBody(RigidBody body) {
        dynamicsWorld.removeRigidBody(body);
    }
    
    /**
     * Gets the underlying JBullet dynamics world.
     * 
     * @return The dynamics world
     */
    public DynamicsWorld getDynamicsWorld() {
        return dynamicsWorld;
    }
    
    /**
     * Cleans up physics world resources.
     */
    public void dispose() {
        // Remove all rigid bodies
        for (int i = dynamicsWorld.getNumCollisionObjects() - 1; i >= 0; i--) {
            dynamicsWorld.removeCollisionObject(dynamicsWorld.getCollisionObjectArray().get(i));
        }
    }
}
