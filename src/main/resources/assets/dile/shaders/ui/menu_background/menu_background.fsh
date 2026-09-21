#version 150

layout(std140) uniform MenuBackgroundParams {
    vec4 Params0;
    vec4 Params1;
};

in vec2 FragCoord;
out vec4 OutColor;

float ran(float a) {
    return fract(sin(a * 2250.0) * 2750.0);
}

void main() {
    float time = Params0.x;
    vec2 resolution = vec2(Params1.x, Params1.y);
    vec2 p = gl_FragCoord.xy / resolution;
    float r = ran(p.x * p.x + p.y * p.y * time);
    OutColor = vec4(r, r, r, 0.22);
}
