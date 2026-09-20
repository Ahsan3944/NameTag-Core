package com.ultraop.nametag.paper;

import com.ultraop.nametag.api.OnlinePlayer;
import com.ultraop.nametag.api.PlayerResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public final class PaperPlayerResolver implements PlayerResolver {
    @Override
    public Optional<OnlinePlayer> findOnline(String name) {
        Player player = Bukkit.getPlayerExact(name);
        return player == null
                ? Optional.empty()
                : Optional.of(new OnlinePlayer(player.getUniqueId(), player.getName()));
    }

    @Override
    public Collection<OnlinePlayer> onlinePlayers() {
        return List.copyOf(Bukkit.getOnlinePlayers()).stream()
                .map(player -> new OnlinePlayer(player.getUniqueId(), player.getName()))
                .toList();
    }
}
