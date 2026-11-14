#version 330 core

out vec4 color;

in vec2 texture_coordinates;

uniform sampler2D u_texture;
//uniform sampler2D u_texture2;
uniform vec4      u_color;
//uniform float     u_ratio;

vec4 lerp(vec4 colorA, vec4 colorB, float ratio) {
    return ratio * colorA + (1.0 - ratio) * colorB;
}

void main() {
//    color = lerp(texture(u_texture, texture_coordinates), texture(u_texture2, texture_coordinates), u_ratio) * u_color;
    color = texture(u_texture, texture_coordinates) * u_color;
}
