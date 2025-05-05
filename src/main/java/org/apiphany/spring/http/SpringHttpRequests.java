package org.apiphany.spring.http;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.hc.core5.http.Header;
import org.morphix.reflection.Constructors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Utility methods for HTTP requests in a Spring context.
 *
 * @author Radu Sebastian LAZIN
 */
public class SpringHttpRequests {

	/**
	 * Returns the current HTTP servlet request. This method will return a non-empty optional when called within the context
	 * of an actual HTTP request.
	 *
	 * @return the current HTTP servlet request
	 */
	public static Optional<HttpServletRequest> getCurrentHttpRequest() {
		return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
				.filter(o -> ServletRequestAttributes.class.isAssignableFrom(o.getClass()))
				.map(ServletRequestAttributes.class::cast)
				.map(ServletRequestAttributes::getRequest);
	}

	/**
	 * Encodes the given request parameters.
	 *
	 * @param requestParameters HTTP request parameters
	 * @return encoded request parameters
	 */
	public static Map<String, String> encodeRequestParameters(final Map<String, String> requestParameters) {
		Map<String, String> params = HashMap.newHashMap(requestParameters.size());
		var uriComponentsBuilder = UriComponentsBuilder.newInstance();
		requestParameters.forEach(uriComponentsBuilder::queryParam);
		Map<String, List<String>> multiParams = uriComponentsBuilder
				.encode()
				.build()
				.getQueryParams();
		requestParameters.forEach((key, value) -> params.put(key, multiParams.get(key).getFirst()));
		return params;
	}

	/**
	 * Transforms an array of {@link Header}s to {@link HttpHeaders}.
	 *
	 * @param headers source headers
	 * @return HTTP headers
	 */
	public static HttpHeaders toHttpHeaders(final Header[] headers) {
		HttpHeaders httpHeaders = new HttpHeaders();
		for (Header header : headers) {
			httpHeaders.add(header.getName(), header.getValue());
		}
		return httpHeaders;
	}

	/**
	 * Returns a Spring HTTP method {@link HttpMethod} object.
	 *
	 * @param method string HTTP method
	 * @return HTTP method
	 */
	public static HttpMethod getHttpMethod(final String method) {
		return HttpMethod.valueOf(method);
	}

	/**
	 * Hide constructor.
	 */
	private SpringHttpRequests() {
		throw Constructors.unsupportedOperationException();
	}
}
