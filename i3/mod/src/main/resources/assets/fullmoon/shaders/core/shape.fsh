#version 330

#moj_import <minecraft:dynamictransforms.glsl>

in vec4 vertexColor;
in vec2 shapeLocal;
in vec2 shapeHalf;
in float shapeRadius;
in float shapeThickness;

out vec4 fragColor;

float roundBoxDistance(vec2 p, vec2 halfExtent, float radius) {
    vec2 q = abs(p) - halfExtent + radius;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - radius;
}

void main() {
    float d = roundBoxDistance(shapeLocal, shapeHalf, shapeRadius);
    if (shapeThickness > 0.0) {
        // Inset band: the stroke grows inwards from the edge, so a 1px rule on an
        // integer-aligned rect lands on exactly one row of pixels.
        d = abs(d + shapeThickness * 0.5) - shapeThickness * 0.5;
    }
    // shapeLocal is in GUI px, so fwidth(d) is one physical pixel whatever the GUI scale.
    // That keeps a hairline one pixel wide instead of scaling up into a slab.
    float coverage = clamp(0.5 - d / max(fwidth(d), 1e-5), 0.0, 1.0);
    vec4 color = vertexColor * ColorModulator;
    color.a *= coverage;
    if (color.a < 0.002) {
        discard;
    }
    fragColor = color;
}
