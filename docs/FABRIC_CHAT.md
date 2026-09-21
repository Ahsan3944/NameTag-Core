# Fabric 1.21.11 Chat Integration

Fabric 1.21.11 chat integration uses Fabric API's server-side ServerMessageDecoratorEvent at the content phase.

## Behavior

- Chat is unchanged when global chatEnabled is false.
- Chat is unchanged when the sender is not an operator. Fabric 1.21.11 has no built-in named permission-node API in this project, so OP is the administrative fallback, matching the existing Fabric command gate.
- Chat is unchanged when the sender has no enabled/chat-enabled active tag.
- Otherwise the configured chatFormat is rendered with {tag}, {player} and {message} placeholders.
- The tag uses the same color and text-style model as the Paper renderer.
- Glitch effects intentionally remain stable in chat; the tag's configured display name is used instead of attempting frame animation.
- The original message component is preserved for the {message} placeholder.

## Secure chat boundary

The Fabric server-side decorator changes the displayed chat component. Minecraft's 1.21.11 message-decorator API documents that changed text can no longer be verified as the original signed message. The renderer therefore keeps its output deterministic for the same sender, active tag and message inputs and does not perform time-based animation in chat.

NameTag-Core does not cancel chat when the feature is disabled or when a sender does not meet the Fabric chat conditions; vanilla/other-mod chat handling remains untouched.

## Scope

This milestone only adds Fabric 1.21.11 chat integration. Fabric player lifecycle and server GameTest coverage are implemented. Server GameTests now execute automatically through the normal Gradle `build` task.
