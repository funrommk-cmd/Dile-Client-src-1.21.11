#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D SceneTex;

layout(std140) uniform ClickGuiCloudsData {
    vec4 iResolution;
    vec4 iTimeClick;
    vec4 iClicks[16];
};

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float vnoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(hash(i), hash(i + vec2(1, 0)), f.x),
        mix(hash(i + vec2(0, 1)), hash(i + vec2(1, 1)), f.x),
        f.y
    );
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 5; i++) {
        v += a * vnoise(p);
        p = p * 2.0;
        a *= 0.5;
    }
    return v;
}

void main() {
    vec2 uv = texCoord;
    float time = iTimeClick.x;
    int cc = int(iTimeClick.y);
    vec2 res = iResolution.xy;

    vec2 disp = vec2(0.0);
    for (int i = 0; i < 16; i++) {
        if (i >= cc) break;
        vec4 clk = iClicks[i];
        if (abs(clk.w) <= 0.001) continue;
        vec2 cp = clk.xy / res;
        vec2 dir = uv - cp;
        float dist = length(dir);
        float falloff = smoothstep(clk.z / max(res.x, res.y) * 1.5, 0.0, dist);
        float strength = clk.w * falloff;

        if (dist > 0.001) {
            vec2 ndir = normalize(dir);
            vec2 swirl = vec2(-ndir.y, ndir.x);
            disp += (ndir * 0.5 + swirl * 0.4) * strength * 0.35;
        }
    }

    vec2 finalUV = uv + disp;
    vec3 scene = texture(SceneTex, finalUV).rgb;

    float heightFactor = 1.0 - finalUV.y;

    vec2 cloudUV = finalUV * 2.5 + time * 0.008;
    float c1 = fbm(cloudUV);
    float c2 = fbm(cloudUV * 1.7 + vec2(5.0, 11.0));
    float c3 = fbm(cloudUV * 2.3 + vec2(3.0, 7.0));
    float cloudAlpha = mix(c1, mix(c2, c3, 0.4), 0.6);
    cloudAlpha = smoothstep(0.15, 0.5, cloudAlpha);
    cloudAlpha *= 0.5 + 0.5 * smoothstep(0.0, 0.5, heightFactor);

    float wisp = fbm(finalUV * 5.0 + time * 0.006);
    wisp = smoothstep(0.3, 0.55, wisp);
    wisp *= 0.25 * heightFactor;

    float totalCloud = max(cloudAlpha, wisp);
    totalCloud = clamp(totalCloud, 0.0, 0.85);

    vec3 white = vec3(1.0, 0.98, 0.95);
    vec3 grey = vec3(0.65, 0.65, 0.68);
    vec3 cloudColor = mix(grey, white, smoothstep(0.0, 0.4, totalCloud));

    vec3 finalColor = mix(scene, cloudColor, totalCloud);

    fragColor = vec4(finalColor, 1.0);
}
