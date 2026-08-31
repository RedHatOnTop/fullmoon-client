package dev.fullmoon.client.keybinds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class KeybindTest {

    private static final KeybindEntry FORWARD = new KeybindEntry(
        "key.forward", "Movement", "Walk Forward", "W", 87, true, false);
    private static final KeybindEntry BACK = new KeybindEntry(
        "key.back", "Movement", "Walk Backward", "S", 83, true, false);
    private static final KeybindEntry JUMP = new KeybindEntry(
        "key.jump", "Movement", "Jump", "SPACE", 32, true, false);
    private static final KeybindEntry SETTINGS = new KeybindEntry(
        "key.fullmoon.settings", "Fullmoon", "Settings", "F9", 298, true, false);
    private static final KeybindEntry CONFLICTING_FORWARD = new KeybindEntry(
        "key.custom.action", "Gameplay", "Special Move", "W", 87, false, false);
    private static final KeybindEntry UNBOUND_1 = new KeybindEntry(
        "key.unbound1", "Misc", "Unbound 1", "NONE", -1, false, true);
    private static final KeybindEntry UNBOUND_2 = new KeybindEntry(
        "key.unbound2", "Misc", "Unbound 2", "NONE", -1, false, true);

    @Test
    void noConflictsWhenKeysAreDistinct() {
        List<KeybindEntry> entries = List.of(FORWARD, BACK, JUMP, SETTINGS);
        Map<String, List<KeybindEntry>> conflicts = KeybindConflict.findConflicts(entries);
        assertTrue(conflicts.isEmpty(), "distinct keys should have zero conflicts");
    }

    @Test
    void detectsDuplicateBoundKeys() {
        List<KeybindEntry> entries = List.of(FORWARD, BACK, CONFLICTING_FORWARD);
        Map<String, List<KeybindEntry>> conflicts = KeybindConflict.findConflicts(entries);

        assertEquals(2, conflicts.size());
        assertTrue(conflicts.containsKey("key.forward"));
        assertTrue(conflicts.containsKey("key.custom.action"));

        List<KeybindEntry> forwardConflicts = conflicts.get("key.forward");
        assertEquals(1, forwardConflicts.size());
        assertEquals("key.custom.action", forwardConflicts.getFirst().id());
    }

    @Test
    void unboundKeysDoNotConflictWithEachOther() {
        List<KeybindEntry> entries = List.of(FORWARD, UNBOUND_1, UNBOUND_2);
        Map<String, List<KeybindEntry>> conflicts = KeybindConflict.findConflicts(entries);
        assertTrue(conflicts.isEmpty(), "unbound keys must never generate false conflicts");
    }

    @Test
    void searchFiltersByLabelCategoryAndKey() {
        List<KeybindEntry> entries = List.of(FORWARD, BACK, JUMP, SETTINGS);

        assertEquals(List.of(FORWARD), KeybindSearch.filter(entries, "forward"));
        assertEquals(List.of(JUMP), KeybindSearch.filter(entries, "space"));
        assertEquals(List.of(FORWARD, BACK, JUMP), KeybindSearch.filter(entries, "movement"));
        assertEquals(List.of(SETTINGS), KeybindSearch.filter(entries, "fullmoon settings"));
        assertEquals(List.of(SETTINGS), KeybindSearch.filter(entries, "f9"));
        assertEquals(entries, KeybindSearch.filter(entries, ""));
    }
}
