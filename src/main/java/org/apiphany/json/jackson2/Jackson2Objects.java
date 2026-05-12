package org.apiphany.json.jackson2;

import java.util.List;
import java.util.Optional;

import org.morphix.reflection.Constructors;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility class for working with Jackson 2 objects.
 *
 * @author Radu Sebastian LAZIN
 */
public class Jackson2Objects {

	/**
	 * Returns the optional {@link ObjectMapper} object for the given {@link RestTemplate}.
	 *
	 * @param restTemplate the rest template to get the object mapper from
	 * @return object mapper
	 */
	public static Optional<ObjectMapper> getObjectMapper(final RestTemplate restTemplate) {
		List<HttpMessageConverter<?>> messageConverters = restTemplate.getMessageConverters();
		ObjectMapper objectMapper = null;
		for (HttpMessageConverter<?> httpMessageConverter : messageConverters) {
			if (httpMessageConverter instanceof MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter) {
				objectMapper = mappingJackson2HttpMessageConverter.getObjectMapper();
				break;
			}
		}
		return Optional.ofNullable(objectMapper);
	}

	/**
	 * Private constructor.
	 */
	private Jackson2Objects() {
		throw Constructors.unsupportedOperationException();
	}
}
