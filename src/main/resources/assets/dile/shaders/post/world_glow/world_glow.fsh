#version 150

uniform sampler2D SceneSampler;
uniform sampler2D DepthSampler;

layout(std140) uniform WorldGlowData {
    vec4 Data;       // x = strength, y = speed, z = time (seconds), w = unused
    vec4 Color;      // rgb = glow color, a = glow alpha
    vec4 CameraPos;  // xyz = camera world position
    mat4 InverseProjection;
    mat4 InverseView;
};

in vec2 texCoord;
out vec4 fragColor;

vec3 reconstructWorld(vec2 uv, float depth) {
    vec4 clip = vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 view = InverseProjection * clip;
    view /= view.w;
    vec4 world = InverseView * view;
    world /= world.w;
    return world.xyz + CameraPos.xyz;
}

void main() {
    vec3 scene = texture(SceneSampler, texCoord).rgb;
    float depth = texture(DepthSampler, texCoord).r;

    if (depth >= 1.0) {
        fragColor = vec4(scene, 1.0);
        return;
    }

    vec3 worldPos = reconstructWorld(texCoord, depth);
    if (isnan(worldPos.x) || isnan(worldPos.y) || isnan(worldPos.z)
            || abs(worldPos.x) > 3.0e7 || abs(worldPos.y) > 3.0e7 || abs(worldPos.z) > 3.0e7) {
        fragColor = vec4(scene, 1.0);
        return;
    }

    float strength = clamp(Data.x, 0.0, 1.0);
    float wave = 0.5 + 0.5 * sin(worldPos.x * 1.2 + worldPos.z * 1.2 - Data.z * Data.y);
    float wave2 = 0.5 + 0.5 * sin(worldPos.x * 0.7 - worldPos.z * 0.7 - Data.z * Data.y * 0.5);
    float waveMix = wave * 0.7 + wave2 * 0.3;

    float fade = clamp(0.15 + strength * 0.7, 0.0, 0.9);
    vec3 baseFade = mix(scene, vec3(0.0), fade);
    vec3 glow = Color.rgb * (0.3 + waveMix * 0.7) * Color.a * strength;

    fragColor = vec4(baseFade + glow, 1.0);
}
