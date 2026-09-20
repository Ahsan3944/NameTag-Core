package com.ultraop.nametag.common;

import java.util.Map;

@FunctionalInterface
public interface StorageMigration {
    Map<String, Object> migrate(Map<String, Object> document);
}