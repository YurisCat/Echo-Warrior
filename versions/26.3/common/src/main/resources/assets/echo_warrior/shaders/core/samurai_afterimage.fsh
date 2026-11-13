#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:fog.glsl>
#include <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;
uniform sampler2D DissolveMaskSampler;

layout(location = 0) in float sphericalVertexDistance;
layout(location = 1) in float cylindricalVertexDistance;
layout(location = 2) in vec4 vertexPerFaceColorBack;
layout(location = 3) in vec4 vertexPerFaceColorFront;
layout(location = 4) in vec4 lightMapColor;
layout(location = 5) in vec4 overlayColor;
layout(location = 6) in vec2 texCoord0;
layout(location = 7) in float dissolveThreshold;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0);
#ifdef ALPHA_CUTOUT
    if (color.a < ALPHA_CUTOUT) {
        discard;
    }
#endif

    if (dissolveThreshold < texture(DissolveMaskSampler, texCoord0).a) {
        discard;
    }

    vec4 faceVertexColor = gl_FrontFacing ? vertexPerFaceColorFront : vertexPerFaceColorBack;
    color.rgb *= vec3(AFTERIMAGE_TINT_R, AFTERIMAGE_TINT_G, AFTERIMAGE_TINT_B);
    color *= faceVertexColor * ColorModulator;
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
    color *= lightMapColor;

    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance,
            FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart,
            FogRenderDistanceEnd, FogColor);
}
