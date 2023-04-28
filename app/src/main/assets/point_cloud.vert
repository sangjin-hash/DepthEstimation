uniform mat4 u_ModelViewProjection;
uniform float u_PointSize;

attribute vec4 a_Position;
attribute vec3 a_Color;

varying vec4 v_Color;

void main() {
    v_Color = vec4(a_Color, 1.0);
    gl_Position = u_ModelViewProjection * vec4(a_Position.xyz, 1.0);

    gl_Position.w *= a_Position.w;

    gl_PointSize = u_PointSize;
}