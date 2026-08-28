#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

// One pipeline draws every solid in this UI: rect, rounded rect, hairline, ring, circle.
// The quad carries the shape's own geometry instead of a texture, so the fragment stage can
// evaluate a signed distance and antialias against the physical pixel derivative.
//
//   UV0  local offset from the shape centre, in GUI px
//   UV1  half extent, 1/16 px fixed point
//   UV2  x = corner radius, y = ring thickness (<= 0 means filled), 1/16 px fixed point

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;

out vec4 vertexColor;
out vec2 shapeLocal;
out vec2 shapeHalf;
out float shapeRadius;
out float shapeThickness;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color;
    shapeLocal = UV0;
    shapeHalf = vec2(UV1) / 16.0;
    shapeRadius = float(UV2.x) / 16.0;
    shapeThickness = float(UV2.y) / 16.0;
}
