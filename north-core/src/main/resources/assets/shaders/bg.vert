#version 330 core

layout (location = 0) in vec3 a_position;
//layout (location = 1) in vec2 a_uv;

out vec2 texture_coordinates;
out vec3 inColor;

uniform mat4 u_model;
uniform mat4 u_view;
uniform mat4 u_projection;

uniform mat4 u_transform;

void main() {
    vec4 vertPosition = u_projection * u_view * u_model * vec4(a_position, 1);
    gl_Position = vertPosition;
//    gl_Position = u_projection * vec4(a_position, 1);
//    texture_coordinates = a_uv;
    inColor = vertPosition.xyz;
}
