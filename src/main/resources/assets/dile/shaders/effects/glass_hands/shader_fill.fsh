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

vec4 textureRND2D(vec2 uv) {
    uv = floor(fract(uv) * 1e3);
    float v = uv.x + uv.y * 1e3;
    return fract(1e5 * sin(vec4(v * 1e-2, (v + 1.0) * 1e-2, (v + 1e3) * 1e-2, (v + 1e3 + 1.0) * 1e-2)));
}

float noise2D(vec2 p) {
    vec2 f = fract(p * 1e3);
    vec4 r = textureRND2D(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(r.x, r.y, f.x), mix(r.z, r.w, f.x), f.y);
}

float cloud(vec2 p) {
    float v = 0.0;
    v += noise2D(p * 1.0) * 0.5;
    v += noise2D(p * 2.0) * 0.2;
    v += noise2D(p * 4.0) * 0.125;
    v += noise2D(p * 8.0) * 0.0625;
    v += noise2D(p * 16.0) * 0.03125;
    return v * v * v;
}

vec3 shaderCloud(vec2 uv, float time) {
    vec2 p = uv * 0.05 + 0.5;
    vec3 c = vec3(0.0, 0.0, 0.2);
    c.rgb += vec3(0.6, 0.6, 0.8) * cloud(p * 0.3 + time * 0.0002) * 0.6;
    c.gbr += vec3(0.8, 0.8, 1.0) * cloud(p * 0.2 + time * 0.0002) * 0.8;
    c.grb += vec3(1.0, 1.0, 1.0) * cloud(p * 0.1 + time * 0.0002) * 1.0;
    return c;
}

vec3 shaderDistort(vec2 uv, float time) {
    vec2 res = params2.zw;
    vec2 pos = uv + vec2(0.0);
    pos *= 3.0;

    float halfDistort = 0.8 / 0.5;
    float distortsc2 = 0.8 / 0.8 + halfDistort;

    for (float i = 1.0; i < 10.0; i++) {
        pos.x += 0.45 / i * sin(i * 0.8 * pos.y - time * 1.5);
        pos.y += 0.45 / i * sin(i * distortsc2 * pos.x + time * 1.5);
    }

    vec3 col = vec3(0.5 / sin(time * 11.0 - length(pos.yx) - pos.y));
    vec3 finalCol = col * col;
    vec3 color = vec3(1.0) * 0.3;
    color = color * color * 0.5 + 0.5 * cos(time + pos.xyx + vec3(0.0, 2.0, 4.0)) * 0.7;
    return finalCol.rgb * color;
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
    float viewMode = params3.x;
    float removeHand = params3.y;
    float time = params3.z;
    float shaderTransparency = params2.y;
    vec2 px = vec2(1.0 / max(params2.z, 1.0), 1.0 / max(params2.w, 1.0));

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

    vec3 shaderColor = viewMode < 2.5 ? shaderCloud(texCoord, time) : shaderDistort(texCoord, time);

    vec3 base = removeHand > 0.5 ? beforeColor.rgb : afterColor.rgb;
    vec3 finalColor = mix(base, shaderColor, mask * (1.0 - shaderTransparency));

    fragColor = vec4(finalColor, 1.0);
}
