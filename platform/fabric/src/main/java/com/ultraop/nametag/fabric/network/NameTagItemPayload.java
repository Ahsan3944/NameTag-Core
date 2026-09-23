package com.ultraop.nametag.fabric.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record NameTagItemPayload(
        int entityId,
        String itemId,
        boolean active,
        boolean rotate,
        int speed,
        byte effects
) implements CustomPayload {
    public static final Identifier ID = Identifier.of("nametag-core", "item_nameplate");
    public static final CustomPayload.Type<NameTagItemPayload> TYPE = new CustomPayload.Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, NameTagItemPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.entityId());
                buf.writeUtf(payload.itemId(), 256);
                buf.writeBoolean(payload.active());
                buf.writeBoolean(payload.rotate());
                buf.writeVarInt(payload.speed());
                buf.writeByte(payload.effects());
            },
            buf -> new NameTagItemPayload(
                    buf.readVarInt(),
                    buf.readUtf(256),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readVarInt(),
                    buf.readByte()
            )
    );

    @Override
    public Type<? extends CustomPayload> getType() {
        return TYPE;
    }

    public static final byte EFFECT_BLINK = 1;
    public static final byte EFFECT_NEON = 1 << 1;
    public static final byte EFFECT_WAVE = 1 << 2;
}
