package com.ultraop.nametag.api;

import java.util.Collection;
import java.util.Optional;

public interface PlayerResolver {
    Optional<OnlinePlayer> findOnline(String name);

    Collection<OnlinePlayer> onlinePlayers();
}
