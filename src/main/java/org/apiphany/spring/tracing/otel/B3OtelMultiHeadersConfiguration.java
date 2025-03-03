package org.apiphany.spring.tracing.otel;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.opentelemetry.extension.trace.propagation.B3Propagator;

@Configuration
@ConditionalOnEnabledTracing
@ConditionalOnClass(B3Propagator.class)
public class B3OtelMultiHeadersConfiguration {

	@Bean
	B3Propagator b3Propagator() {
		return B3Propagator.injectingMultiHeaders();
	}
}
