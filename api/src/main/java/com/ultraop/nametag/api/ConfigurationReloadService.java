package com.ultraop.nametag.api;

public interface ConfigurationReloadService {
    /**
     * Reloads configuration from persistent storage.
     *
     * <p>The implementation must validate the complete replacement before publishing it.
     * If loading or validation fails, the previously published snapshot remains active.</p>
     */
    ConfigurationReloadResult reload();
}
