package cz.advel.stack;

/**
 * Stub implementation of Stack allocation for JBullet.
 * The original uses bytecode instrumentation for stack-based allocation.
 * This stub creates new heap instances instead, which is less performant but simpler.
 * 
 * @author clanka
 */
public class Stack {
    /**
     * Allocates a new instance of the given class on the heap.
     * Original would use stack allocation for performance.
     */
    public static <T> T alloc(Class<T> clazz) {
        try {
            return clazz.newInstance();
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Returns a copy of the given object.
     * Original would use stack allocation for performance.
     */
    @SuppressWarnings("unchecked")
    public static <T> T alloc(T obj) {
        if (obj == null) return null;
        
        try {
            // Try to clone if possible
            if (obj instanceof Cloneable) {
                return (T) obj.getClass().getMethod("clone").invoke(obj);
            }
            // Otherwise create new instance and copy (vecmath types have copy constructors)
            return (T) obj.getClass().getConstructor(obj.getClass()).newInstance(obj);
        } catch (Exception e) {
            // Fallback: just return the object itself
            return obj;
        }
    }
    
    /**
     * Library allocation - same as regular alloc.
     */
    public static <T> T libraryAlloc(Class<T> clazz) {
        return alloc(clazz);
    }
    
    /**
     * Library allocation - same as regular alloc.
     */
    public static <T> T libraryAlloc(T obj) {
        return alloc(obj);
    }
    
    /**
     * No-op implementation for library cleanup.
     */
    public static void libraryCleanCurrentThread() {
    }
}
