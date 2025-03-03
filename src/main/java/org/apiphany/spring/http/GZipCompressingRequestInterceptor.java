package org.apiphany.spring.http;

import java.io.IOException;

import org.apiphany.http.ContentEncoding;
import org.apiphany.lang.gzip.GZip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * GZIP compression interceptor.
 *
 * @author Radu Sebastian LAZIN
 */
public class GZipCompressingRequestInterceptor implements ClientHttpRequestInterceptor {

	private static final Logger LOGGER = LoggerFactory.getLogger(GZipCompressingRequestInterceptor.class);

	/**
	 * Used in conjunction with compression configuration in clients.
	 */
	public static final String SKIP_COMPRESSION = "Skip-Compression";

	/**
	 * Compress a request body using GZIP and add HTTP headers.
	 *
	 * @param httpRequest HTTP request
	 * @param body request body
	 * @param exec client request execution object
	 * @return HTTP response
	 * @throws IOException on error
	 */
	@Override
	public ClientHttpResponse intercept(final HttpRequest httpRequest, final byte[] body, final ClientHttpRequestExecution exec)
			throws IOException {
		HttpHeaders httpHeaders = httpRequest.getHeaders();
		byte[] bytes = body;

		if (!httpHeaders.containsKey(SKIP_COMPRESSION)) {
			LOGGER.debug("Compressing request: URI: {}", httpRequest.getURI());
			httpHeaders.add(HttpHeaders.CONTENT_ENCODING, ContentEncoding.GZIP.value());
			httpHeaders.add(HttpHeaders.ACCEPT_ENCODING, ContentEncoding.GZIP.value());
			bytes = GZip.compress(body);
			httpHeaders.set(HttpHeaders.CONTENT_LENGTH, Integer.toString(bytes.length));
			LOGGER.debug("Request compressed, sending...");
		} else {
			LOGGER.debug("Compression skipped via {} custom HTTP header", SKIP_COMPRESSION);
		}
		return exec.execute(httpRequest, bytes);
	}

}
