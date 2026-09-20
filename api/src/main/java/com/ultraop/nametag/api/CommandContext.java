package com.ultraop.nametag.api;

import java.util.Arrays;
import java.util.Objects;

public record CommandContext(CommandSource source, String[] args) {
    public CommandContext {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(args, "args");
        args = Arrays.copyOf(args, args.length);
    }

    @Override
    public String[] args() {
        return Arrays.copyOf(args, args.length);
    }
}
