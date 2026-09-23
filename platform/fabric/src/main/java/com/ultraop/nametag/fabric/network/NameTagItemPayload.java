package com.ultraop.nametag.fabric.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record NameTagItemPayload(
        int entityId,
        String itemId,
        boolean active,
        boolean rotate,
        int speed,
        byte effects
) implements CustomPayload {
    public static final CustomPayload.Id<NameTagItemPayload> ID =
            CustomPayload.id("nametag-core:item_nameplate");

    public static final PacketCodec<RegistryByteBuf, NameTagItemPayload> CODEC =
            PacketCodec.of(
                    (payload, buf) -> {
                        buf.writeVarInt(payload.entityId());
                        buf.writeString(payload.itemId(), 256);
                        buf.writeBoolean(payload.active());
                        buf.writeBoolean(payload.rotate());
                        buf.writeVarInt(payload.speed());
                        buf.writeByte(payload.effects());
                    },
                    buf -> new NameTagItemPayload(
                            buf.readVarInt(),
                            buf.readString(256),
                            buf.readBoolean(),
                            buf.readBoolean(),
                            buf.readVarInt(),
                            buf.readByte()
                    )
            );

    @Override
    public CustomPayload.Id<NameTagItemPayload> getId() {
        return ID;
    }

    public static final byte EFFECT_BLINK = 1;
    public static final byte EFFECT_NEON = 1 << 1;
    public static final byte EFFECT_WAVE = 1 << 2;
}
