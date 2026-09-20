package com.ultraop.nametag.fabric;

import com.ultraop.nametag.api.TagService;
import com.ultraop.nametag.common.DefaultTagService;
import com.ultraop.nametag.common.YamlPlayerAssignmentRepository;
import com.ultraop.nametag.common.YamlTagRepository;
import net.fabricmc.loader.api.FabricLoader;
import com.ultraop.nametag.fabric.v1_21_11.Fabric2111Adapter;

public final class NameTagFabric {
    private NameTagFabric() {}

    public static void bootstrap() {
        java.nio.file.Path dataDirectory = FabricLoader.getInstance()
                .getConfigDir()
                .resolve("nametag-core");
        TagService service = new DefaultTagService(
                new YamlTagRepository(dataDirectory.resolve("tags.yml")),
                new YamlPlayerAssignmentRepository(dataDirectory.resolve("assignments.yml"))
        );
        new Fabric2111Adapter(service).registerCommands();
    }
}
