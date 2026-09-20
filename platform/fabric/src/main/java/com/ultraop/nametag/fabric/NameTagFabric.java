package com.ultraop.nametag.fabric;

import com.ultraop.nametag.api.TagService;
import com.ultraop.nametag.common.DefaultTagService;
import com.ultraop.nametag.common.InMemoryPlayerAssignmentRepository;
import com.ultraop.nametag.common.InMemoryTagRepository;
import com.ultraop.nametag.fabric.v1_21_11.Fabric2111Adapter;

public final class NameTagFabric {
    private NameTagFabric() {}

    public static void bootstrap() {
        TagService service = new DefaultTagService(
                new InMemoryTagRepository(),
                new InMemoryPlayerAssignmentRepository()
        );
        new Fabric2111Adapter(service).registerCommands();
    }
}
