#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D CaptureSampler;
uniform sampler2D SceneSampler;
uniform sampler2D BlurSampler;
uniform sampler2D DepthSampler;

layout(std140) uniform GlassHandsData {
    vec4 params0;
    vec4 params1;
    vec4 params2;
    vec4 params3;
};

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
    float blurRadius = params0.x;
    float blurIterations = params0.y;
    float saturation = params0.z;
    float reflectEnabled = params0.w;

    vec3 tintColor = params1.rgb;
    float tintIntensity = params1.a;

    float edgeGlowIntensity = params2.x;
    vec2 screenParams = params2.zw;

    float removeHand = params3.y;
    vec2 px = vec2(1.0 / max(screenParams.x, 1.0), 1.0 / max(screenParams.y, 1.0));

    vec4 beforeColor = texture(CaptureSampler, texCoord);
    vec4 afterColor = texture(SceneSampler, texCoord);

    float mask;
    if (removeHand > 0.5) {
        mask = step(0.15, dilatedMask(texCoord, px));
    } else {
        mask = diffMask(texCoord);
    }
    mask *= handDepthMask(texCoord);

    if (mask < 0.01) {
        fragColor = vec4(afterColor.rgb, 1.0);
        return;
    }

    vec3 blurred = texture(BlurSampler, texCoord).rgb;

    vec3 glassColor = mix(afterColor.rgb, blurred, clamp(blurRadius / 5.0, 0.0, 1.0));

    float grey = dot(glassColor, vec3(0.299, 0.587, 0.114));
    glassColor = mix(vec3(grey), glassColor, 1.0 + saturation * 0.5);

    glassColor = mix(glassColor, tintColor, tintIntensity * mask);

    float edgeDist = 0.0;
    for (int i = 0; i < 8; i++) {
        vec2 dirs[8] = vec2[](
            vec2( 1.0,  0.0), vec2( 0.707,  0.707),
            vec2( 0.0,  1.0), vec2(-0.707,  0.707),
            vec2(-1.0,  0.0), vec2(-0.707, -0.707),
            vec2( 0.0, -1.0), vec2( 0.707, -0.707)
        );
        vec2 probeUv = clamp(texCoord + dirs[i] * px * 2.0, vec2(0.0), vec2(1.0));
        float neighborMask = diffMask(probeUv);
        edgeDist += abs(mask - neighborMask);
    }
    edgeDist /= 8.0;

    float edgeGlow = smoothstep(0.0, 0.15, edgeDist) * edgeGlowIntensity;
    vec3 glowColor = glassColor * 1.4 + vec3(0.1);
    glassColor = mix(glassColor, glowColor, edgeGlow);

    float reflectAmount = reflectEnabled * 0.15 * mask;
    vec2 reflectUv = clamp(vec2(texCoord.x, 1.0 - texCoord.y) + vec2(0.0, reflectAmount), vec2(0.0), vec2(1.0));
    vec3 reflected = texture(SceneSampler, reflectUv).rgb;
    glassColor = mix(glassColor, reflected, reflectAmount);

    glassColor = clamp(glassColor, vec3(0.0), vec3(1.0));

    vec3 baseColor = removeHand > 0.5 ? beforeColor.rgb : afterColor.rgb;
    vec3 finalColor = mix(baseColor, glassColor, mask);

    fragColor = vec4(finalColor, 1.0);
}
