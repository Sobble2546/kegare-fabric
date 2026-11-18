#version 330

// The impact frame (DESIGN §3.23): one black-and-white, high-contrast frame on the biggest hits. Brightness is cut hard into black
// or white, low, so the open world goes white and bodies and shadows go black; it's read through a slight blur so a block's texture
// doesn't turn to grain; and the outlines of shapes are inked black over it, like a frame drawn in a hurry. Params.x = Amount mixes it in.

uniform sampler2D DiffuseSampler;

layout(std140) uniform KegareFx {
    vec4 Params;
    vec4 Size;
};

in vec2 texCoord;

out vec4 fragColor;

float luma(vec2 uv) {
    return dot(texture(DiffuseSampler, uv).rgb, vec3(0.299, 0.587, 0.114));
}

// Brightness averaged over a few texels round uv: shapes, not texture.
float soft(vec2 uv, vec2 oneTexel) {
    vec2 r = oneTexel * 3.0;
    float sum = luma(uv) * 2.0;
    sum += luma(uv + vec2(r.x, 0.0)) + luma(uv - vec2(r.x, 0.0));
    sum += luma(uv + vec2(0.0, r.y)) + luma(uv - vec2(0.0, r.y));
    sum += luma(uv + r) + luma(uv - r);
    sum += luma(uv + vec2(r.x, -r.y)) + luma(uv + vec2(-r.x, r.y));
    return sum / 10.0;
}

void main() {
    vec2 oneTexel = Size.zw;
    vec3 original = texture(DiffuseSampler, texCoord).rgb;
    float l = soft(texCoord, oneTexel);
    vec2 e = oneTexel * 4.0;
    float gx = soft(texCoord + vec2(e.x, 0.0), oneTexel) - soft(texCoord - vec2(e.x, 0.0), oneTexel);
    float gy = soft(texCoord + vec2(0.0, e.y), oneTexel) - soft(texCoord - vec2(0.0, e.y), oneTexel);
    float edge = length(vec2(gx, gy));
    float ink = smoothstep(0.29, 0.31, l);       // black or white, nothing between: mid-tones go white, shapes go black
    ink *= 1.0 - smoothstep(0.08, 0.10, edge);  // the outlines of shapes, inked black
    fragColor = vec4(mix(original, vec3(ink), Params.x), 1.0);
}
