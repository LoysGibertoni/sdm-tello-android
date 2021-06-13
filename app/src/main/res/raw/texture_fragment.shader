precision mediump float;

uniform sampler2D u_Texture;    // Texture
varying vec2 v_TexCoordinate;   // Coordinate (from Vertex Shader)

void main() {
    gl_FragColor = texture2D(u_Texture, v_TexCoordinate);
}
