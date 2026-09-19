package com.ultraop.nametag.api;

import com.ultraop.nametag.core.model.Tag;

import java.util.UUID;

public interface NameplateRenderer {
    void apply(UUID playerUuid, String playerName, Tag activeTag);
    void clear(UUID playerUuid);
}
