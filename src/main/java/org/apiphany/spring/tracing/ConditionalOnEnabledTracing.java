package org.apiphany.spring.tracing;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Conditional;

/**
 * {@link Conditional @Conditional} that checks whether tracing is enabled.
 * <p>
 * It matches if {@code management.tracing.enabled} property is {@code true} or if it
 * is not present in application properties.
 *
 * @author Radu Sebastian LAZIN
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@ConditionalOnProperty(prefix = "management.tracing", name = "enabled", matchIfMissing = true)
public @interface ConditionalOnEnabledTracing {

	// empty

}
