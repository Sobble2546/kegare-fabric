#version 330

// A Severed player's senses (DESIGN §3.27): a faint, pale outline round every body they sense, through walls. The mask holds each
// body's silhouette, its brightness how near it is; the outline is the two pixels just outside it, so the body itself is left as it
// is. Dim and neutral: never a team or Malice color, never a glow. Drawn alpha-blended over the picture (alpha = how strong the
// outline is), which is the same as mixing the pale colour into it. Params.x = Strength.

uniform sampler2D MaskSampler;

layout(std140) uniform KegareFx {
    vec4 Params;
    vec4 Size;
};

in vec2 texCoord;

out vec4 fragColor;

float m(vec2 offset) {
    return texture(MaskSampler, texCoord + offset).r;
}

void main() {
    vec2 px = Size.zw;
    float ring = 0.0;
    for (int i = 1; i <= 2; i++) {
        float d = float(i);
        ring = max(ring, max(max(m(vec2(px.x * d, 0.0)), m(vec2(-px.x * d, 0.0))), max(m(vec2(0.0, px.y * d)), m(vec2(0.0, -px.y * d)))));
    }
    float edge = m(vec2(0.0)) > 0.0 ? 0.0 : ring;
    vec3 pale = vec3(0.84, 0.86, 0.88);
    fragColor = vec4(pale, edge * Params.x);
}
