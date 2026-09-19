package com.ultraop.nametag.api;

import com.ultraop.nametag.core.model.Tag;

import java.util.UUID;

public interface ChatTagRenderer {
    String render(UUID playerUuid, String playerName, String message, Tag activeTag);
}
