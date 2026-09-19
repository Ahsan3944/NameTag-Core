package com.ultraop.nametag.api;

import com.ultraop.nametag.core.model.TagEffect;

public interface EffectProvider {
    String id();
    void apply(Object renderContext, TagEffect effect);
}
