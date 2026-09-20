package com.ultraop.nametag.api;

import java.util.Map;

public interface MessageService {
    String message(String key);

    default String format(String key, Map<String, ?> values) {
        String result = message(key);
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            result = result.replace(
                    "{" + entry.getKey() + "}",
                    String.valueOf(entry.getValue())
            );
        }
        return result;
    }
}
