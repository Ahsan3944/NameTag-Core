package com.ultraop.nametag.api;

import java.util.Collection;

public interface NameTagCommandHandler {
    void execute(CommandContext context);

    default Collection<String> suggest(CommandContext context) {
        return java.util.List.of();
    }
}
