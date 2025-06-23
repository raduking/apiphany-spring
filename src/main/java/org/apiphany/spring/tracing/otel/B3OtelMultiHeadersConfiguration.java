package org.apiphany.spring.tracing.otel;

import org.apiphany.spring.tracing.ConditionalOnEnabledTracing;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.opentelemetry.extension.trace.propagation.B3Propagator;

/**
 * Configure OTEL multi-header tracing.
 *
 * @author Radu Sebastian LAZIN
 */
@Configuration
@ConditionalOnEnabledTracing
@ConditionalOnClass(B3Propagator.class)
public class B3OtelMultiHeadersConfiguration {

	/**
	 * Default constructor.
	 */
	public B3OtelMultiHeadersConfiguration() {
		// empty
	}

	/**
	 * Returns the OpenTelemetry B3 multi-header propagator bean.
	 *
	 * @return the OpenTelemetry B3 multi-header propagator bean
	 */
	@Bean
	B3Propagator b3Propagator() {
		return B3Propagator.injectingMultiHeaders();
	}
}
