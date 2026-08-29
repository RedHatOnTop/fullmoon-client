package dev.fullmoon.client.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * As a player, I want to search settings with the words on screen, so I can reach an option
 * without remembering which vanilla menu owns it.
 */
class SettingSearchTest {
    private static final List<SettingSearch.Entry> KOREAN = List.of(
        entry("auto_jump", "게임 플레이", "자동 점프", "블록 모서리에서 자동으로 점프한다", "이동"),
        entry("view_bobbing", "게임 플레이", "화면 흔들림", "걸을 때 시점이 움직인다", "카메라"),
        entry("subtitles", "접근성", "자막 표시", "주변 소리를 방향과 함께 글로 표시한다", "소리"),
        entry("high_contrast", "접근성", "고대비", "메뉴와 글자의 구분을 더 선명하게 한다", "대비"));

    @Test
    void blankQueryKeepsTheCatalogOrder() {
        assertEquals(KOREAN, SettingSearch.filter(KOREAN, "  \t "));
    }

    @Test
    void aVisibleLabelFindsItsSetting() {
        assertEquals(List.of(KOREAN.get(2)), SettingSearch.filter(KOREAN, "자막"));
    }

    @Test
    void descriptionsAndAliasesAreSearchable() {
        assertEquals(List.of(KOREAN.get(1)), SettingSearch.filter(KOREAN, "시점"));
        assertEquals(List.of(KOREAN.get(0)), SettingSearch.filter(KOREAN, "이동"));
    }

    @Test
    void queryWordsMayLandInDifferentFields() {
        assertEquals(List.of(KOREAN.get(2)), SettingSearch.filter(KOREAN, "접근성 소리"));
    }

    @Test
    void everyQueryWordMustMatch() {
        assertEquals(List.of(), SettingSearch.filter(KOREAN, "접근성 점프"));
    }

    @Test
    void matchingIsCaseInsensitiveAndUnicodeNormalized() {
        SettingSearch.Entry entry = entry(
            "save_chat_drafts", "Interface", "Save chat drafts", "Keep unfinished chat", "CHAT");
        assertEquals(List.of(entry), SettingSearch.filter(List.of(entry), "chat drafts"));
        assertEquals(List.of(entry), SettingSearch.filter(List.of(entry), "ＣＨＡＴ"));
    }

    @Test
    void resultAndAliasesAreImmutableCopies() {
        SettingSearch.Entry entry = entry("subtitles", "Accessibility", "Subtitles", "Sound", "audio");
        List<SettingSearch.Entry> result = SettingSearch.filter(List.of(entry), "sound");

        assertThrows(UnsupportedOperationException.class, () -> result.add(entry));
        assertThrows(UnsupportedOperationException.class, () -> entry.aliases().add("captions"));
    }

    @Test
    void malformedEntriesAndQueriesAreRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> entry("", "Accessibility", "Subtitles", "Sound", "audio"));
        assertThrows(IllegalArgumentException.class,
            () -> entry("subtitles", "", "Subtitles", "Sound", "audio"));
        assertThrows(IllegalArgumentException.class,
            () -> entry("subtitles", "Accessibility", "", "Sound", "audio"));
        assertThrows(IllegalArgumentException.class,
            () -> entry("subtitles", "Accessibility", "Subtitles", "Sound", " "));
        assertThrows(NullPointerException.class, () -> SettingSearch.filter(KOREAN, null));
    }

    private static SettingSearch.Entry entry(String id, String section, String label,
            String description, String... aliases) {
        return new SettingSearch.Entry(id, section, label, description, List.of(aliases));
    }
}
