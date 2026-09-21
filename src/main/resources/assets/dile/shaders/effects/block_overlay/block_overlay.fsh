#version 150

in vec2 texCoord;
out vec4 fragColor;

layout(std140) uniform BlockOverlayData {
    vec2 iResolution;
    float iTime;
    float iAlpha;
};

float random(in vec2 _st) {
    return fract(sin(dot(_st.xy,
                         vec2(12.9898, 78.233))) *
        43758.5453123);
}

float noise(in vec2 _st) {
    vec2 i = floor(_st);
    vec2 f = fract(_st);

    float a = random(i);
    float b = random(i + vec2(1.0, 0.0));
    float c = random(i + vec2(0.0, 1.0));
    float d = random(i + vec2(1.0, 1.0));

    vec2 u = f * f * (3.0 - 2.0 * f);

    return mix(a, b, u.x) +
            (c - a) * u.y * (1.0 - u.x) +
            (d - b) * u.x * u.y;
}

#define NUM_OCTAVES 8

float fbm(in vec2 _st) {
    float v = 0.0;
    float a = 0.5;
    vec2 shift = vec2(100.0);
    mat2 rot = mat2(cos(0.5), sin(0.5),
                    -sin(0.5), cos(0.50));
    for (int i = 0; i < NUM_OCTAVES; ++i) {
        v += a * noise(_st);
        _st = rot * _st * 2.0 + shift;
        a *= 0.63;
    }
    return v;
}

void main() {
    vec2 st = (texCoord * iResolution) / iResolution.y;
    st.x *= 0.3;

    vec3 color = vec3(0.0);

    vec2 q = vec2(0.0);
    q.x = fbm(st + 0.00 * iTime);
    q.y = fbm(st + vec2(1.0));

    vec2 r = vec2(0.0);
    r.x = fbm(st + 1.0 * q + vec2(1.7, 9.2) + 0.15 * iTime);
    r.y = fbm(st + 1.0 * q + vec2(8.3, 2.8) + 0.126 * iTime);

    float f = fbm(st + r);

    color = mix(vec3(0.101961, 0.19608, 0.666667),
                vec3(0.666667, 0.666667, 0.48039),
                clamp((f * f) * 4.0, 0.0, 1.0));

    color = mix(color,
                vec3(0.0, 0.0, 0.164706),
                clamp(length(q), 0.0, 1.0));

    color = mix(color,
                vec3(0.666667, 1.0, 1.0),
                clamp(length(r.x), 0.0, 1.0));

    fragColor = vec4((f * f * f + 0.6 * f * f + 0.5 * f) * color, iAlpha);
}
