package org.apiphany.spring;

import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.core5.pool.ConnPoolControl;
import org.apiphany.lang.builder.PropertyNameBuilder;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.httpcomponents.hc5.PoolingHttpClientConnectionManagerMetricsBinder;

/**
 * Utility methods to register connection manager metrics. This is similar to
 * {@link PoolingHttpClientConnectionManagerMetricsBinder} but it doesn't use tags.
 * <p>
 * The following metrics will be published:
 *
 * <pre>
 * 	httpcomponents.httpclient.${clientName}.pool.total.max
 * 	httpcomponents.httpclient.${clientName}.pool.total.connections.available
 * 	httpcomponents.httpclient.${clientName}.pool.total.connections.leased
 * 	httpcomponents.httpclient.${clientName}.pool.total.pending
 * 	httpcomponents.httpclient.${clientName}.pool.route.max.default
 * </pre>
 *
 * where {@code ${clientName}} is the parameter given when constructing the binder.
 *
 * @author Radu Sebastian LAZIN
 */
public class ConnectionManagerMetricsBinder implements MeterBinder {

	public static final String METRIC_HTTP_CLIENT_PREFIX = "httpcomponents.httpclient";
	public static final String METRIC_POOL_TOTAL_PREFIX = "pool.total";
	public static final String METRIC_POOL_ROUTE_PREFIX = "pool.route";

	private final ConnPoolControl<HttpRoute> connPoolControl;
	private final String clientName;

	private ConnectionManagerMetricsBinder(final ConnPoolControl<HttpRoute> connPoolControl, final String clientName) {
		this.connPoolControl = connPoolControl;
		this.clientName = clientName;
	}

	public static ConnectionManagerMetricsBinder of(final ConnPoolControl<HttpRoute> connPoolControl, final String clientName) {
		return new ConnectionManagerMetricsBinder(connPoolControl, clientName);
	}

	@Override
	public void bindTo(final MeterRegistry registry) {
		registerTotalMetrics(registry);
	}

	private void registerTotalMetrics(final MeterRegistry registry) {
		// httpcomponents.httpclient.${clientName}.pool.total.max
		Gauge.builder(metricName(METRIC_HTTP_CLIENT_PREFIX, clientName, METRIC_POOL_TOTAL_PREFIX, "max"), connPoolControl,
				cpc -> cpc.getTotalStats().getMax())
				.description("The configured maximum number of allowed persistent connections for all routes.")
				.register(registry);

		// httpcomponents.httpclient.${clientName}.pool.total.connections.available
		Gauge.builder(metricName(METRIC_HTTP_CLIENT_PREFIX, clientName, METRIC_POOL_TOTAL_PREFIX, "connections", "available"), connPoolControl,
				cpc -> cpc.getTotalStats().getAvailable())
				.description("The number of persistent and available connections for all routes.")
				.register(registry);

		// httpcomponents.httpclient.${clientName}.pool.total.connections.leased
		Gauge.builder(metricName(METRIC_HTTP_CLIENT_PREFIX, clientName, METRIC_POOL_TOTAL_PREFIX, "connections", "leased"), connPoolControl,
				cpc -> cpc.getTotalStats().getLeased())
				.description("The number of persistent and leased connections for all routes.")
				.register(registry);

		// httpcomponents.httpclient.${clientName}.pool.total.pending
		Gauge.builder(metricName(METRIC_HTTP_CLIENT_PREFIX, clientName, METRIC_POOL_TOTAL_PREFIX, "pending"), connPoolControl,
				cpc -> cpc.getTotalStats().getPending())
				.description("The number of connection requests being blocked awaiting a free connection for all routes.")
				.register(registry);

		// httpcomponents.httpclient.${clientName}.pool.route.max.default
		Gauge.builder(metricName(METRIC_HTTP_CLIENT_PREFIX, clientName, METRIC_POOL_ROUTE_PREFIX, "max", "default"), connPoolControl,
				ConnPoolControl::getDefaultMaxPerRoute)
				.description("The configured default maximum number of allowed persistent connections per route.")
				.register(registry);
	}

	private static String metricName(final String... paths) {
		return PropertyNameBuilder.builder()
				.path(paths)
				.build();
	}

}
