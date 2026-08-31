package dev.fullmoon.client.ui;

/**
 * What a state looks like: the ground under a control, the ink on it, and the edge around it.
 *
 * <p>The focus ring is not in here. It is drawn outside the control's bounds, so it belongs to
 * the {@link Voice} rather than to any one state — see {@link Voice#ring()}.
 */
public record Chrome(int fill, int ink, int line) {}
