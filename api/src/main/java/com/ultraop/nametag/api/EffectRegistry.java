package com.ultraop.nametag.api;

import java.util.Collection;
import java.util.Optional;

public interface EffectRegistry {
    void register(EffectProvider provider);
    boolean unregister(String id);
    Optional<EffectProvider> find(String id);
    Collection<EffectProvider> list();
}
