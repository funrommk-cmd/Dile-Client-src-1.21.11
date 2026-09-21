#version 150

in vec2 texCoord;
out vec4 fragColor;

layout(std140) uniform SkyData {
    vec4 iColor;
    vec4 iParams;
    vec4 iCamera;
    vec2 iResolution;
};

#define MAX_ITER 4
#define STARS_OCT 5

mat3 rotX(float a) {
    float c = cos(a), s = sin(a);
    return mat3(1.0, 0.0, 0.0,
                0.0,   c,   s,
                0.0,  -s,   c);
}
mat3 rotY(float a) {
    float c = cos(a), s = sin(a);
    return mat3(  c, 0.0,   s,
                0.0, 1.0, 0.0,
                 -s, 0.0,   c);
}

float hash11(float p) {
    p = fract(p * 0.1031);
    p *= p + 33.33;
    p *= p + p;
    return fract(p);
}

float hash21(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

vec3 hash33(vec3 p3) {
    p3 = fract(p3 * vec3(0.1031, 0.1030, 0.0973));
    p3 += dot(p3, p3.yxz + 33.33);
    return fract((p3.xxy + p3.yxx) * p3.zyx);
}

float noise2d(vec2 st) {
    vec2 i = floor(st);
    vec2 f = fract(st);
    float a = hash21(i);
    float b = hash21(i + vec2(1.0, 0.0));
    float c = hash21(i + vec2(0.0, 1.0));
    float d = hash21(i + vec2(1.0, 1.0));
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

float fbm(vec2 st) {
    float val = 0.0;
    float amp = 0.5;
    for (int i = 0; i < STARS_OCT; i++) {
        val += amp * noise2d(st);
        amp *= 0.5;
        st *= 2.0;
    }
    return val;
}

mat2 rotate2d(float theta) {
    return mat2(cos(theta), -sin(theta), sin(theta), cos(theta));
}

void main() {
    float uTime = iCamera.w;
    vec2 uResolution = iResolution;
    vec3 uColor = iColor.rgb;
    float uAlpha = iColor.a;
    float uSpeed = iParams.x;
    float uScale = iParams.y;
    float uIntensity = iParams.z;
    float mode = iParams.w;
    vec2 uCameraDir = iCamera.xy;
    float uFov = iCamera.z;

    vec2 uv = texCoord;
    vec2 sp = uv * 2.0 - 1.0;
    float aspect = uResolution.x / uResolution.y;
    float tanV = tan(radians(uFov) * 0.5);
    vec3 rayV = normalize(vec3(sp.x * tanV * aspect, sp.y * tanV, 1.0));
    vec3 rayW = rotY(uCameraDir.x) * rotX(uCameraDir.y) * rayV;

    if (mode < 0.5) {
        // Water
        vec3 p = rayW * uScale;
        vec3 it = p;
        float c = 1.0;
        float inten = uIntensity;
        for (int n = 0; n < MAX_ITER; n++) {
            float t = uTime * uSpeed * (11.0 - (3.0 / float(n + 1)));
            it = p + vec3(
                cos(t - it.x) + sin(t + it.y),
                sin(t - it.y) + cos(t + it.z),
                cos(t - it.z) + sin(t + it.x)
            );
            c += 1.0 / length(vec3(
                p.x / (sin(it.x + t) / inten),
                p.y / (cos(it.y + t) / inten),
                p.z / (sin(it.z + t) / inten)
            ));
        }
        c /= float(MAX_ITER);
        c = 1.5 - sqrt(c);
        float brightness = c * c * c * c;
        vec3 color = uColor * brightness + uColor * 0.15;
        fragColor = vec4(color, uAlpha);

    } else if (mode < 1.5) {
        // Caustic
        vec3 p = rayW * uScale;
        vec3 it = p;
        float c = 1.0;
        float inten = uIntensity;
        for (int n = 0; n < MAX_ITER; n++) {
            float t = uTime * uSpeed * (1.0 - (3.0 / float(n + 1)));
            it = p + vec3(
                cos(t - it.x) + sin(t + it.y),
                sin(t - it.y) + cos(t + it.z),
                cos(t - it.z) + sin(t + it.x)
            );
            c += 1.0 / length(vec3(
                p.x / (sin(it.x + t) / inten),
                p.y / (cos(it.y + t) / inten),
                p.z / (sin(it.z + t) / inten)
            ));
        }
        c /= float(MAX_ITER);
        c = 1.5 - sqrt(c);
        float brightness = c * c * c * c;
        vec3 color = uColor * brightness * 1.5 + uColor * 0.2;
        fragColor = vec4(color, uAlpha);

    } else {
        // Stars
        vec2 dir = rayW.xy / rayW.z;
        vec2 suv = dir * 0.5 + 0.5;

        float t = 0.06 * uTime * 2.0 * (hash11(42.0) - 0.5);
        float t2 = 0.06 * uTime * 2.0 * (hash11(17.0) - 0.5);
        vec2 anch = vec2(0.5, -4.0);
        suv = (suv - anch) * rotate2d(t * uSpeed / length(anch)) + anch + t2;

        float res = uResolution.x / 2.0;
        vec2 starres = floor(res * suv) / res;

        suv *= uScale;
        float perlin = fbm(suv + vec2(fbm(suv), fbm(suv + 32.0)));

        vec3 darkTint = uColor * 0.25;
        vec3 brightTint = uColor * 1.2 + vec3(0.1, 0.3, 0.2);
        vec3 color = mix(darkTint, brightTint, perlin);
        color *= color;
        color *= color;
        color *= uIntensity * 80.0;

        float seed = hash21(starres);
        if (seed >= 0.998) {
            seed = hash21(starres + seed);
            color += vec3(0.2 + seed + 0.5 * seed * sin(uTime * uSpeed + 10.0 * seed)) * uIntensity * 80.0;
        }

        fragColor = vec4(color, uAlpha);
    }
}
