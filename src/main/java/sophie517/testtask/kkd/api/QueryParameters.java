package sophie517.testtask.kkd.api;

import java.util.Set;
import org.springframework.util.MultiValueMap;
import sophie517.testtask.kkd.api.exception.InvalidParamsException;

public final class QueryParameters {
    private final MultiValueMap<String, String> parameters;

    public QueryParameters(MultiValueMap<String, String> parameters, Set<String> allowed) {
        this.parameters = parameters;
        parameters.forEach((key, values) -> {
            if (!allowed.contains(key)) throw new InvalidParamsException("Unknown parameter: " + key);
            if (values.size() != 1) throw new InvalidParamsException("Parameter must occur once: " + key);
        });
    }

    public String required(String name) {
        String value = optional(name);
        if (value == null) throw new InvalidParamsException(name + " is required");
        return value;
    }

    public String optional(String name) {
        String value = parameters.getFirst(name);
        if (parameters.containsKey(name) && (value == null || value.isBlank() || value.length() > 100)) {
            throw new InvalidParamsException("Invalid " + name);
        }
        return value;
    }

    public int integer(String name, int defaultValue, int min, int max) {
        String value = optional(name);
        if (value == null) return defaultValue;
        try {
            int number = Integer.parseInt(value);
            if (number >= min && number <= max) return number;
        } catch (NumberFormatException ignored) {
            // The API uses 422 for malformed and out-of-range parameters alike.
        }
        throw new InvalidParamsException("Invalid " + name);
    }
}
