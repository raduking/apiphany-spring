package org.apiphany.spring;

import java.util.List;

import org.apache.hc.client5.http.classic.HttpClient;
import org.morphix.lang.Nullables;
import org.springframework.boot.actuate.metrics.web.client.ObservationRestTemplateCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Utility class for working with rest templates.
 *
 * @author Radu Sebastian LAZIN
 */
public abstract class RestTemplates {

	/**
	 * Names of the interceptor beans that can be added to the created rest template.
	 */
	private static final List<String> INTERCEPTOR_BEAN_NAMES = List.of(
	// empty for now, can be populated with the names of the interceptor beans that should be added
	);

	/**
	 * Private constructor.
	 */
	private RestTemplates() {
		throw new UnsupportedOperationException("This class should not be instantiated!");
	}

	/**
	 * Returns a new {@link RestTemplate} configured with the given HTTP client.
	 * <p>
	 * This method automatically adds tracing information if tracing is present.
	 *
	 * @param httpClient HTTP client this rest template will use
	 * @param ctx application context
	 * @return a new rest template
	 */
	public static RestTemplate create(final HttpClient httpClient, final ApplicationContext ctx) {
		RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
		if (null == ctx) {
			return restTemplate;
		}
		for (String beanName : INTERCEPTOR_BEAN_NAMES) {
			Nullables.whenNotNull(Beans.<ClientHttpRequestInterceptor>getBean(beanName, ctx, Beans.nullOnError()))
					.then(restTemplate.getInterceptors()::add);
		}
		ObservationRestTemplateCustomizer observationCustomizer =
				Beans.getBean(ObservationRestTemplateCustomizer.class, ctx, Beans.nullOnError());
		Nullables.whenNotNull(observationCustomizer).then(customizer -> customizer.customize(restTemplate));
		return restTemplate;
	}
}
