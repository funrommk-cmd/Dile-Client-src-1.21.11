#version 150

in vec2 texCoord;
out vec4 fragColor;

layout(std140) uniform ClickGuiBgData {
    vec2  iResolution;
    float iTime;
    float iOpacity;
    vec2  iMouse;
    int   iClickCount;
    vec4  iClicks[16];
};

mat2 rotate2D(float r) {
    return mat2(cos(r), sin(r), -sin(r), cos(r));
}

void main() {
    vec2 uv = (gl_FragCoord.xy - 0.5 * iResolution.xy) / iResolution.y;
    vec3 col = vec3(0);
    float t = iTime;

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

    float alpha = iOpacity;

    for (int i = 0; i < 16; i++) {
        if (i >= iClickCount) break;
        vec4 click = iClicks[i];
        if (click.w <= 0.001) continue;
        float dist = distance(gl_FragCoord.xy, click.xy);
        float revealFactor = 1.0 - smoothstep(0.0, click.z, dist);
        alpha *= (1.0 - revealFactor * click.w);
    }

    fragColor = vec4(col, alpha);
}
