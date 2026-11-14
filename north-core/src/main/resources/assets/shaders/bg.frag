#version 330 core

layout (location = 0) out vec4 color;

//in vec2 texture_coordinates;
in vec3 inColor;

//uniform sampler2D u_tex;
uniform vec4 u_color;

void main() {
//    color = texture(u_tex, texture_coordinates);
    color = u_color;
//    color = (vec4(inColor, 0.1) + 0.2) * 5;
    color = vec4(0.42, 0.42, 0.42, 0.9);
}
