package dev.fullmoon.client.render;

/** Packed-colour arithmetic. Hue and lightness always come from a token; only alpha varies. */
public final class Rgb {
    private Rgb() {}

    /**
     * Reopens a token's alpha. The only legitimate use is a scrim: a full-screen surface has
     * to let the blurred world read through it, and an opaque scrim would hide the blur the
     * game just rendered.
     */
    public static int alpha(int argb, float alpha) {
        int a = Math.round(Math.clamp(alpha, 0.0f, 1.0f) * 255.0f);
        return (a << 24) | (argb & 0xFFFFFF);
    }
}
