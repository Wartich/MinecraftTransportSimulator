package cz.advel.stack;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation stub for JBullet's StaticAlloc.
 * The original was used with bytecode instrumentation for stack-based allocation.
 * This stub is just a marker annotation with no runtime effect.
 * 
 * @author clanka
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface StaticAlloc {
}
