package org.apiphany.spring.client;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;

import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpHost;
import org.apiphany.ApiRequest;
import org.apiphany.ApiResponse;
import org.apiphany.client.ClientProperties;
import org.apiphany.client.ExchangeClient;
import org.apiphany.client.http.AbstractHttpExchangeClient;
import org.apiphany.client.http.ApacheHC5ExchangeClient;
import org.apiphany.client.http.PoolingHttpClients;
import org.apiphany.http.CloseableHttpResponseInputStream;
import org.apiphany.http.HttpException;
import org.apiphany.http.HttpMethod;
import org.apiphany.http.HttpStatus;
import org.apiphany.lang.collections.Maps;
import org.apiphany.meters.ConnectionManagerMetricsBinder;
import org.apiphany.spring.BeanFinder;
import org.apiphany.spring.RestTemplates;
import org.apiphany.spring.collections.ExtendedMaps;
import org.apiphany.spring.http.GZipCompressingRequestInterceptor;
import org.apiphany.spring.http.SpringHttpRequests;
import org.morphix.lang.JavaObjects;
import org.morphix.lang.Nullables;
import org.morphix.reflection.Methods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.DeserializationFeature;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Exchange client implemented with {@link RestTemplate}.
 *
 * @author Radu Sebastian LAZIN
 */
public class RestTemplateExchangeClient extends AbstractHttpExchangeClient implements ApplicationContextAware, BeanFinder {

	/**
	 * Class logger.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(RestTemplateExchangeClient.class);

	/**
	 * The underlying REST template.
	 */
	private RestTemplate restTemplate;

	/**
	 * The underlying HTTP client.
	 */
	private CloseableHttpClient httpClient;

	/**
	 * The connection manager.
	 */
	private PoolingHttpClientConnectionManager connectionManager;

	/**
	 * The application context.
	 */
	private ApplicationContext ctx;

	/**
	 * Constructor, see also {@link #initialize()}.
	 *
	 * @param clientProperties client properties
	 */
	public RestTemplateExchangeClient(final ClientProperties clientProperties) {
		super(clientProperties);
	}

	/**
	 * Initializes the client after the object was constructed. If you implement this class and use it as a non bean make
	 * sure you use an initializer that calls all methods that are annotated with {@link PostConstruct}.
	 *
	 * @see Methods.IgnoreAccess#invokeWithAnnotation(Object, Class)
	 */
	@PostConstruct
	private void initialize() {
		ClientProperties clientProperties = getClientProperties();

		this.httpClient = PoolingHttpClients.createClient(clientProperties, PoolingHttpClients.noCustomizer(),
				this::customize, PoolingHttpClients.noCustomizer());
		this.restTemplate = RestTemplates.create(httpClient, getApplicationContext());

		if (clientProperties.getCompression().isGzip()) {
			restTemplate.getInterceptors().add(new GZipCompressingRequestInterceptor());
		}
		RestTemplates.getObjectMapper(restTemplate)
				.ifPresent(mapper -> mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true));
	}

	/**
	 * Customizes the connection manager.
	 *
	 * @param connectionManager pooling HTTP client connection manager
	 */
	private void customize(final PoolingHttpClientConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
		registerMetrics(connectionManager);
	}

	/**
	 * Registers the metrics for the connection manager.
	 *
	 * @param connectionManager pooling HTTP client connection manager
	 */
	protected void registerMetrics(final PoolingHttpClientConnectionManager connectionManager) {
		MeterRegistry meterRegistry = getBean(MeterRegistry.class);
		if (null != meterRegistry) {
			ConnectionManagerMetricsBinder.of(connectionManager, getClass().getSimpleName())
					.bindTo(meterRegistry);
		}
	}

	/**
	 * @see #close()
	 */
	@Override
	public void close() throws Exception {
		if (null != httpClient) {
			httpClient.close();
		}
	}

	/**
	 * @see ExchangeClient#exchange(ApiRequest)
	 */
	@Override
	public <T, U> ApiResponse<U> exchange(final ApiRequest<T> request) {
		URI uri = getUriComponentsBuilder(request.getUrl(), request.getParams()).build().toUri();
		HttpEntity<?> httpEntity = buildHttpEntity(request);
		HttpMethod httpMethod = request.getMethod();
		if (request.isStream()) {
			return download(uri, httpMethod, httpEntity);
		}
		var springHttpMethod = SpringHttpRequests.getHttpMethod(httpMethod.value());

		ResponseEntity<U> responseEntity = null;
		if (request.hasGenericType()) {
			Type genericType = request.getGenericResponseType().getType();
			ParameterizedTypeReference<U> parameterizedResponseType = ParameterizedTypeReference.forType(genericType);
			responseEntity = restTemplate.exchange(uri, springHttpMethod, httpEntity, parameterizedResponseType);
		} else {
			responseEntity = restTemplate.exchange(uri, springHttpMethod, httpEntity, request.getClassResponseType());
		}
		return ApiResponse.create(responseEntity.getBody())
				.status(responseEntity.getStatusCode().value(), HttpStatus::from)
				.headers(responseEntity.getHeaders())
				.exchangeClient(this)
				.build();
	}

	/**
	 * Performs a download HTTP operation. The response entity body will be an {@link InputStream}.
	 *
	 * @param <T> response entity type
	 * @param <U> request entity type
	 * @param uri URI to call
	 * @param method HTTP method
	 * @param requestEntity request HTTP entity
	 * @return response entity
	 */
	public <T, U> ApiResponse<T> download(final URI uri, final HttpMethod method, final HttpEntity<U> requestEntity) {
		HttpHost httpHost = HttpHost.create(uri);
		HttpUriRequest httpRequest = ApacheHC5ExchangeClient.toHttpUriRequest(uri, method);
		ApiResponse.Builder<T> apiResponseBuilder = ApiResponse.<T>builder().exchangeClient(this);
		try {
			ClassicHttpResponse httpResponse = httpClient.executeOpen(httpHost, httpRequest, null);
			HttpStatus status = HttpStatus.from(httpResponse.getCode());
			if (status.isError()) {
				throw new HttpException(status, "Failed to download content.");
			}
			HttpHeaders headers = SpringHttpRequests.toHttpHeaders(httpResponse.getHeaders());
			InputStream inputStream = CloseableHttpResponseInputStream.of(httpResponse);
			apiResponseBuilder
					.body(JavaObjects.cast(inputStream))
					.status(status)
					.headers(headers);
		} catch (IOException e) {
			LOGGER.error("Failed to download content.", e);
			apiResponseBuilder.status(HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return apiResponseBuilder.build();
	}

	/**
	 * Returns the {@link UriComponentsBuilder} with the give url and request parameters.
	 *
	 * @param url URL
	 * @param requestParams request parameters
	 * @return a new {@link UriComponentsBuilder}
	 */
	protected static UriComponentsBuilder getUriComponentsBuilder(final String url, final Map<String, String> requestParams) {
		Map<String, String> queryParams = Nullables.apply(requestParams, HashMap::new, HashMap::new);
		return UriComponentsBuilder.fromUriString(url)
				.queryParams(ExtendedMaps.multiValueMap(queryParams));
	}

	/**
	 * Builds the HTTP request entity from the API request object. This method also adds: {@link HttpHeaders#CONTENT_TYPE} as
	 * {@link MediaType#APPLICATION_JSON} and {@link HttpHeaders#ACCEPT} with {@link MediaType#APPLICATION_JSON} if none are
	 * present in the request.
	 *
	 * @param <T> request entity type
	 *
	 * @param request the request object
	 * @return the request entity
	 */
	protected <T> HttpEntity<T> buildHttpEntity(final ApiRequest<T> request) {
		HttpHeaders headers = new HttpHeaders();
		Map<String, List<String>> existingHeaders = request.getHeaders();
		if (Maps.isNotEmpty(existingHeaders)) {
			existingHeaders.forEach(headers::addAll);
		}
		return new HttpEntity<>(request.getBody(), headers);
	}

	/**
	 * Sets the application context.
	 *
	 * @param applicationContext the application context
	 * @throws BeansException when the application context cannot be set
	 */
	@Override
	public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
		this.ctx = applicationContext;
	}

	/**
	 * Returns the application context.
	 *
	 * @return the application context
	 */
	@Override
	public ApplicationContext getApplicationContext() {
		return ctx;
	}

	/**
	 * Returns the HTTP client.
	 *
	 * @return the HTTP client
	 */
	protected CloseableHttpClient getHttpClient() {
		return httpClient;
	}

	/**
	 * Returns the pooling HTTP client connection manager.
	 *
	 * @return the pooling HTTP client connection manager
	 */
	protected PoolingHttpClientConnectionManager getConnectionManager() {
		return connectionManager;
	}
}
