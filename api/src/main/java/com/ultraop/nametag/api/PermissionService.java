package com.ultraop.nametag.api;

import java.util.UUID;

public interface PermissionService {
    boolean has(UUID playerUuid, String permission);
}
