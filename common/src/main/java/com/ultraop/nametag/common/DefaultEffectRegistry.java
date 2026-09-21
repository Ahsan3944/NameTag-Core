package com.ultraop.nametag.common;

import com.ultraop.nametag.api.EffectProvider;
import com.ultraop.nametag.api.EffectRegistry;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultEffectRegistry implements EffectRegistry {
    private final ConcurrentHashMap<String, EffectProvider> providers = new ConcurrentHashMap<>();

    @Override
    public void register(EffectProvider provider) {
        Objects.requireNonNull(provider, "provider");
        String id = Objects.requireNonNull(provider.id(), "provider.id()");
        if (id.isBlank()) {
            throw new IllegalArgumentException("Effect provider id cannot be blank");
        }
        EffectProvider previous = providers.putIfAbsent(id, provider);
        if (previous != null) {
            throw new IllegalArgumentException("Effect provider already registered: " + id);
        }
    }

    @Override
    public boolean unregister(String id) {
        return providers.remove(Objects.requireNonNull(id, "id")) != null;
    }

    @Override
    public Optional<EffectProvider> find(String id) {
        return Optional.ofNullable(providers.get(Objects.requireNonNull(id, "id")));
    }

    @Override
    public Collection<EffectProvider> list() {
        return List.copyOf(providers.values());
    }
}
