package com.ultraop.nametag.common;

import com.ultraop.nametag.api.EffectProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DefaultEffectRegistryTest {
    @Test
    void registersFindsListsAndUnregistersProviders() {
        DefaultEffectRegistry registry = new DefaultEffectRegistry();
        EffectProvider provider = new EffectProvider() {
            @Override
            public String id() {
                return "example";
            }

            @Override
            public void apply(Object renderContext, com.ultraop.nametag.core.model.TagEffect effect) {
            }
        };

        registry.register(provider);

        assertSame(provider, registry.find("example").orElseThrow());
        assertEquals(1, registry.list().size());
        assertTrue(registry.unregister("example"));
        assertTrue(registry.find("example").isEmpty());
        assertFalse(registry.unregister("example"));
    }

    @Test
    void rejectsDuplicateBlankAndNullProviders() {
        DefaultEffectRegistry registry = new DefaultEffectRegistry();
        EffectProvider first = provider("example");

        registry.register(first);
        assertThrows(IllegalArgumentException.class, () -> registry.register(provider("example")));
        assertThrows(IllegalArgumentException.class, () -> registry.register(provider("")));
        assertThrows(NullPointerException.class, () -> registry.register(null));
    }

    private static EffectProvider provider(String id) {
        return new EffectProvider() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public void apply(Object renderContext, com.ultraop.nametag.core.model.TagEffect effect) {
            }
        };
    }
}
