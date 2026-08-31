package dev.fullmoon.client.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HudConfigTest {

    @Test
    void jsonRoundTrip() {
        HudConfig config = new HudConfig();
        config.elements.put("coords", new HudConfig.ElementState(true, "TOP_LEFT", 12, 16, 1.0f));
        config.elements.put("fps", new HudConfig.ElementState(false, "BOTTOM_RIGHT", 20, 24, 1.2f));

        String json = config.toJson();
        HudConfig restored = HudConfig.fromJson(json);

        assertEquals(2, restored.elements.size());
        assertTrue(restored.elements.get("coords").enabled);
        assertEquals("TOP_LEFT", restored.elements.get("coords").anchor);
        assertEquals(12, restored.elements.get("coords").offsetX);
        assertEquals(16, restored.elements.get("coords").offsetY);

        assertEquals(false, restored.elements.get("fps").enabled);
        assertEquals("BOTTOM_RIGHT", restored.elements.get("fps").anchor);
        assertEquals(20, restored.elements.get("fps").offsetX);
        assertEquals(24, restored.elements.get("fps").offsetY);
    }
}
