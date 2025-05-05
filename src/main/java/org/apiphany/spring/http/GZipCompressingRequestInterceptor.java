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
import org.springframework.util.ObjectUtils;

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

		if (!ObjectUtils.isEmpty(bytes)) {
			if (!httpHeaders.containsKey(SKIP_COMPRESSION)) {
				LOGGER.debug("Compressing request: URI: {}", httpRequest.getURI());
				bytes = compress(bytes, httpHeaders);
				LOGGER.debug("Request compressed, sending...");
			} else {
				LOGGER.debug("Compression skipped via {} custom HTTP header", SKIP_COMPRESSION);
			}
		} else {
			LOGGER.debug("Compression skipped because the body was empty");
		}
		return exec.execute(httpRequest, bytes);
	}

	/**
	 * Compresses the given bytes and adds the corresponding compression headers to the request.
	 *
	 * @param body bytes to compress
	 * @param httpHeaders the HTTP headers of the request
	 * @return compressed byte array
	 * @throws IOException when compression fails
	 */
	private static byte[] compress(final byte[] body, final HttpHeaders httpHeaders) throws IOException {
		byte[] bytes = GZip.compress(body);
		httpHeaders.add(HttpHeaders.CONTENT_ENCODING, ContentEncoding.GZIP.value());
		httpHeaders.add(HttpHeaders.ACCEPT_ENCODING, ContentEncoding.GZIP.value());
		httpHeaders.set(HttpHeaders.CONTENT_LENGTH, Integer.toString(bytes.length));
		return bytes;
	}

}
