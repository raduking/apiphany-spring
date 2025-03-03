package org.apiphany.spring.collections;

import java.util.Map;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.morphix.lang.JavaObjects;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Utility methods for non standard maps. This class is only here because Spring adds some new multi-value maps.
 * <p>
 * Use {@link org.apiphany.lang.collections.Maps} for any other purposes.
 *
 * @author Radu Sebastian LAZIN
 */
public abstract class ExtendedMaps {

	private static final MultiValueMap<?, ?> EMPTY_MULTI_VALUE_MAP = new LinkedMultiValueMap<>(0);
	private static final MultiValuedMap<?, ?> EMPTY_MULTI_VALUED_MAP = new ArrayListValuedHashMap<>(0);

	/**
	 * Private constructor.
	 */
	private ExtendedMaps() {
		throw new UnsupportedOperationException("This class should not be instantiated");
	}

	/**
	 * Returns an empty multi value map.
	 *
	 * @param <K> key type
	 * @param <V> value type
	 * @return an empty multi value map
	 */
	public static <K, V> MultiValueMap<K, V> emptyMultiValueMap() {
		return JavaObjects.cast(EMPTY_MULTI_VALUE_MAP);
	}

	/**
	 * Returns a new {@link MultiValueMap} from a {@link Map}.
	 *
	 * @param map entry map
	 * @return a new multi value map
	 */
	public static <K, V> MultiValueMap<K, V> multiValueMap(final Map<K, V> map) {
		if (map == null) {
			return emptyMultiValueMap();
		}
		MultiValueMap<K, V> multiValueMap = new LinkedMultiValueMap<>();
		for (Map.Entry<K, V> entry : map.entrySet()) {
			multiValueMap.add(entry.getKey(), entry.getValue());
		}
		return multiValueMap;
	}

	/**
	 * Returns a new {@link MultiValueMap} from a key and value.
	 *
	 * @param key map entry key
	 * @param value map entry value
	 * @return a new multi value map
	 */
	public static <K, V> MultiValueMap<K, V> multiValueMap(final K key, final V value) {
		MultiValueMap<K, V> multiValueMap = new LinkedMultiValueMap<>();
		multiValueMap.add(key, value);
		return multiValueMap;
	}

	/**
	 * Returns an empty multivalued map (Apache collection type).
	 *
	 * @param <K> key type
	 * @param <V> value type
	 * @return an empty multi values map
	 */
	public static <K, V> MultiValuedMap<K, V> emptyMultiValuedMap() {
		return JavaObjects.cast(EMPTY_MULTI_VALUED_MAP);
	}

}
