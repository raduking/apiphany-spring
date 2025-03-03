package org.apiphany.spring.tracing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiphany.spring.tracing.otel.B3OtelSingleHeaderConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Annotation to enable B3 single header.
 *
 * @author Radu Sebastian LAZIN
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({ B3OtelSingleHeaderConfiguration.class })
public @interface EnableTracingB3SingleHeader {

	// empty

}
