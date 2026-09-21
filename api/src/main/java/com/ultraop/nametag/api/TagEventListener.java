package com.ultraop.nametag.api;

@FunctionalInterface
public interface TagEventListener {
    void onEvent(TagEvent event);
}
