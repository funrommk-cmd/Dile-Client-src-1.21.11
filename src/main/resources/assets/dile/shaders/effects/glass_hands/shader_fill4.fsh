#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D CaptureSampler;
uniform sampler2D SceneSampler;
uniform sampler2D DepthSampler;

layout(std140) uniform GlassHandsData {
    vec4 params0;
    vec4 params1;
    vec4 params2;
    vec4 params3;
};

#define NUM_OCTAVES 36
// by galaxy-empty-nagibutzker

mat3 rotX(float a) {
    float c = cos(a);
    float s = sin(a);
    return mat3(
        1, 0, 0,
        0, c, -s,
        0, s, c
    );
}

mat3 rotY(float a) {
    float c = cos(a);
    float s = sin(a);
    return mat3(
        c, 0, -s,
        0, 2, 0,
        s, 0, c
    );
}

float random(vec2 pos) {
    return fract(sin(dot(pos.xy, vec2(1399.9898, 78.233))) * 43758.5453123);
}

float noise(vec2 pos) {
    vec2 i = floor(pos);
    vec2 f = fract(pos);
    float a = random(i + vec2(0.0, 0.0));
    float b = random(i + vec2(1.0, 0.0));
    float c = random(i + vec2(0.0, 1.0));
    float d = random(i + vec2(1.0, 1.0));
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

float fbm(vec2 pos) {
    float v = 0.0;
    float a = 0.5;
    vec2 shift = vec2(100.0);
    mat2 rot = mat2(cos(0.5), sin(0.5), -sin(0.5), cos(0.5));
    for (int i = 0; i < NUM_OCTAVES; i++) {
        v += a * noise(pos);
        pos = rot * pos * 2.0 + shift;
        a *= 0.5;
    }
    return v;
}

float diffMask(vec2 uv) {
    vec4 before = texture(CaptureSampler, uv);
    vec4 after = texture(SceneSampler, uv);
    vec3 delta = abs(after.rgb - before.rgb);
    float peak = max(max(delta.r, delta.g), delta.b);
    float luma = dot(delta, vec3(0.299, 0.587, 0.114));
    float diffValue = peak * 0.78 + luma * 0.88 + abs(after.a - before.a);
    return smoothstep(0.012, 0.085, diffValue);
}

float dilatedMask(vec2 uv, vec2 px) {
    float m = diffMask(uv);
    for (int i = 0; i < 8; i++) {
        vec2 dirs[8] = vec2[](
            vec2( 1.0,  0.0), vec2( 0.707,  0.707),
            vec2( 0.0,  1.0), vec2(-0.707,  0.707),
            vec2(-1.0,  0.0), vec2(-0.707, -0.707),
            vec2( 0.0, -1.0), vec2( 0.707, -0.707)
        );
        m = max(m, diffMask(uv + dirs[i] * px * 3.0));
    }
    return clamp(m * 3.0, 0.0, 1.0);
}

float handDepthMask(vec2 uv) {
    return texture(DepthSampler, uv).r < 0.9999 ? 1.0 : 0.0;
}

void main() {
    float removeHand = params3.y;
    float time = params3.z;
    float shaderTransparency = params2.y;
    vec2 resolution = params2.zw;
    vec2 px = vec2(1.0 / max(resolution.x, 1.0), 1.0 / max(resolution.y, 1.0));

    vec4 beforeColor = texture(CaptureSampler, texCoord);
    vec4 afterColor = texture(SceneSampler, texCoord);

    float mask = diffMask(texCoord);
    if (removeHand > 0.5) {
        mask = step(0.15, dilatedMask(texCoord, px));
    }
    mask *= handDepthMask(texCoord);

    if (mask < 0.01) {
        fragColor = vec4(afterColor.rgb, 1.0);
        return;
    }

    vec2 p = (gl_FragCoord.xy * 0.9 - resolution.xy) / min(resolution.x, resolution.y);

    float t = 0.0, d;

    float time2 = 0.6 * time / 2.5;

    vec2 q = vec2(0.0);
    q.x = fbm(p + 0.30 * time2);
    q.y = fbm(p + vec2(1.0));
    vec2 r = vec2(0.0);
    r.x = fbm(p + 1.0 * q + vec2(1.2, 3.2) + 0.135 * time2);
    r.y = fbm(p + 1.0 * q + vec2(8.8, 2.8) + 0.126 * time2);
    float f = fbm(p + r);
    vec3 color = mix(
        vec3(0.0, 0.0, 0),
        vec3(0, 0, 1.0),
        clamp((f * f) * 8.0, 0.0, 5.0)
    );

    color = mix(
        color,
        vec3(1, 0, 2),
        clamp(length(q), 0.0, 1.0)
    );

    color = mix(
        color,
        vec3(1, 0, 1),
        clamp(length(r.x), 0.0, 1.0)
    );

    color = (f * f * f + 0.5 * f * f + 0.2 * f) * color;

    vec3 shaderColor = clamp(color, vec3(0.0), vec3(1.0));

    vec3 base = removeHand > 0.5 ? beforeColor.rgb : afterColor.rgb;
    vec3 finalColor = mix(base, shaderColor, mask * (1.0 - shaderTransparency));

    fragColor = vec4(finalColor, 1.0);
}
