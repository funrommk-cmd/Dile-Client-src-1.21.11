#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D InputSampler;

layout(std140) uniform GlassHandsData {
    vec4 params0;
    vec4 params1;
    vec4 params2;
};

void main() {
    vec2 px = params2.zw;
    float radius = params0.x;

    vec4 result = vec4(0.0);
    float totalWeight = 0.0;

    float weights[5] = float[](0.227027, 0.1945946, 0.1216216, 0.054054, 0.016216);

    for (int i = 0; i < 5; i++) {
        float weight = weights[i];
        vec2 offset = px * float(i + 1) * radius * 0.5;

        vec4 s1 = texture(InputSampler, clamp(texCoord + vec2(offset.x, 0.0), vec2(0.0), vec2(1.0)));
        vec4 s2 = texture(InputSampler, clamp(texCoord - vec2(offset.x, 0.0), vec2(0.0), vec2(1.0)));
        vec4 s3 = texture(InputSampler, clamp(texCoord + vec2(0.0, offset.y), vec2(0.0), vec2(1.0)));
        vec4 s4 = texture(InputSampler, clamp(texCoord - vec2(0.0, offset.y), vec2(0.0), vec2(1.0)));

        result += (s1 + s2 + s3 + s4) * weight;
        totalWeight += weight * 4.0;
    }

    vec4 center = texture(InputSampler, texCoord);
    fragColor = (result + center * 0.227027) / max(totalWeight + 0.227027, 0.01);
}
