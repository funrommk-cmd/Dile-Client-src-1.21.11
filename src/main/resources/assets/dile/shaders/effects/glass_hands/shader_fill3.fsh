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

mat2 rotate2D(float r) {
    return mat2(cos(r), sin(r), -sin(r), cos(r));
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
    vec2 px = vec2(1.0 / max(params2.z, 1.0), 1.0 / max(params2.w, 1.0));
    vec2 resolution = params2.zw;

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

    vec2 uv = (gl_FragCoord.xy - 0.5 * resolution) / resolution.y;
    vec3 col = vec3(0);
    float t = time;

    vec2 n = vec2(0);
    vec2 q = vec2(0);
    vec2 p = uv;
    float d = dot(p, p);
    float S = 16.0;
    float a = 0.0;
    mat2 m = rotate2D(p.x * 0.1 + length(p) * 0.2 + 0.5);

    for (float j = 0.0; j < 6.0; j++) {
        p *= m;
        n *= m;
        q = p * S + t * 0.6 + sin(t * 0.25 - d * 4.0) * 4.0 + j + a - n;
        a += dot(cos(q) / S, vec2(0.4));
        n -= sin(q);
        S *= 1.4;
        m = m * 1.05;
    }

    col = vec3(1.6, 2.6, 3.4) * ((a * 3.0) + 0.2) + a + a - d;

    vec3 shaderColor = clamp(col, vec3(0.0), vec3(1.0));

    vec3 base = removeHand > 0.5 ? beforeColor.rgb : afterColor.rgb;
    vec3 finalColor = mix(base, shaderColor, mask * (1.0 - shaderTransparency));

    fragColor = vec4(finalColor, 1.0);
}
