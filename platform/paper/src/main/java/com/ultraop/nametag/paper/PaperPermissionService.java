package com.ultraop.nametag.paper;

import com.ultraop.nametag.api.PermissionService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.UUID;

public final class PaperPermissionService implements PermissionService {
    @Override
    public boolean has(UUID playerUuid, String permission) {
        Player player = Bukkit.getPlayer(playerUuid);
        return player != null && (player.isOp() || player.hasPermission(permission));
    }
}
