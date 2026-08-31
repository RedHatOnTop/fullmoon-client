#!/usr/bin/env bash
# Run FtCheck against the fonts this client ships, on the same FreeType the game uses.
# LWJGL and its natives come from the Gradle cache rather than a declared dependency: this
# is a design-time gate, not part of the mod, and the point is to use the exact build that
# net.minecraft.client.gui.font.providers.TrueTypeGlyphProviderDefinition will use.
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
jdk=/home/person/.gradle/jdks/eclipse_adoptium-25-amd64-linux.2/bin/java
cache=/home/person/.gradle/caches/modules-2/files-2.1/org.lwjgl

cp=$(find "$cache/lwjgl/3.4.1" "$cache/lwjgl-freetype/3.4.1" -name '*.jar' | paste -sd:)
if [ -z "$cp" ]; then
    echo "no lwjgl jars in the gradle cache; run the mod's build once first" >&2
    exit 1
fi

fonts=("$@")
if [ ${#fonts[@]} -eq 0 ]; then
    mapfile -t fonts < <(find "$here/../mod/src/main/resources/assets/fullmoon/font" -name '*.ttf' | sort)
fi

exec "$jdk" --enable-native-access=ALL-UNNAMED -cp "$cp" "$here/ftcheck/FtCheck.java" "${fonts[@]}"
