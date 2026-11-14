#version 330 core

out vec4 color;

in vec2 texture_coordinates;

uniform sampler2D u_texture;
uniform vec4      u_color;
uniform int       u_sprite_index;
uniform int       u_sprite_count;

void main() {
    float spriteCount = max(float(u_sprite_count), 1.0);
    float spriteWidth = 1.0 / spriteCount;
    float spriteOffset = float(u_sprite_index) * spriteWidth;

    vec2 updated_coordinates = vec2(
            spriteOffset + texture_coordinates.x * spriteWidth,
            texture_coordinates.y);

    color = texture(u_texture, updated_coordinates) * u_color;
}
