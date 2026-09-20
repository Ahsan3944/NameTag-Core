package com.ultraop.nametag.api;

import com.ultraop.nametag.core.model.NameTagConfiguration;

public interface ConfigurationService {
    /**
     * Returns the immutable configuration snapshot currently owned by the service.
     *
     * Reloading is intentionally provided by a separate lifecycle service.
     */
    NameTagConfiguration current();
}
