#version 330

// Standstill's one inverted frame (DESIGN §3.26): every colour flipped, as the clock stops. Params.x = Amount (it's held for one frame).

uniform sampler2D DiffuseSampler;

layout(std140) uniform KegareFx {
    vec4 Params;
    vec4 Size;
};

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec3 original = texture(DiffuseSampler, texCoord).rgb;
    fragColor = vec4(mix(original, vec3(1.0) - original, Params.x), 1.0);
}
