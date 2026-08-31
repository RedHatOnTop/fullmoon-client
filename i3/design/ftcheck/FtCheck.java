// Answers the one question a font file has to pass before the game will load it: does
// FreeType call it "TrueType"? net.minecraft.client.gui.font.providers
// .TrueTypeGlyphProviderDefinition#load throws IOException("Font is not in TTF format,
// was ...") on anything else, and the same FreeType build (LWJGL 3.4.1) answers here.
//
//   design/ftcheck.sh <font> [<font> ...]

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FreeType;

public final class FtCheck {
    private static final int[] PROBE = {'A', '0', 0xAC00, 0xB9CC, 0x2014};

    public static void main(String[] args) throws Exception {
        long library;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer out = stack.mallocPointer(1);
            if (FreeType.FT_Init_FreeType(out) != 0) {
                throw new IllegalStateException("FT_Init_FreeType failed");
            }
            library = out.get(0);
        }

        int failed = 0;
        for (String arg : args) {
            failed += check(library, Path.of(arg)) ? 0 : 1;
        }
        FreeType.FT_Done_FreeType(library);
        System.exit(failed == 0 ? 0 : 1);
    }

    private static boolean check(long library, Path path) throws Exception {
        byte[] bytes = Files.readAllBytes(path);
        ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length).put(bytes).flip();
        FT_Face face;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer out = stack.mallocPointer(1);
            int error = FreeType.FT_New_Memory_Face(library, buffer, 0, out);
            if (error != 0) {
                report(path, "FT_New_Memory_Face rejected the file, error " + error, false);
                MemoryUtil.memFree(buffer);
                return false;
            }
            face = FT_Face.create(out.get(0));
        }

        String format = FreeType.FT_Get_Font_Format(face);
        boolean ok = "TrueType".equals(format);
        StringBuilder note = new StringBuilder("format=" + format);
        if (ok) {
            ok = FreeType.FT_Select_Charmap(face, FreeType.FT_ENCODING_UNICODE) == 0;
            note.append(ok ? " unicode charmap" : " NO unicode charmap");
        }
        if (ok) {
            FreeType.FT_Set_Pixel_Sizes(face, 22, 22);
            StringBuilder drawn = new StringBuilder();
            for (int cp : PROBE) {
                int gid = FreeType.FT_Get_Char_Index(face, cp);
                boolean loaded = gid != 0
                    && FreeType.FT_Load_Glyph(face, gid, FreeType.FT_LOAD_RENDER) == 0
                    && face.glyph() != null
                    && face.glyph().bitmap().width() > 0;
                drawn.append(' ').append(new String(Character.toChars(cp))).append(loaded ? "+" : "-");
                ok &= loaded;
            }
            note.append(" glyphs").append(drawn);
        }
        report(path, note.toString(), ok);
        FreeType.FT_Done_Face(face);
        MemoryUtil.memFree(buffer);
        return ok;
    }

    private static void report(Path path, String note, boolean ok) {
        System.out.printf("%-4s %-22s %s%n", ok ? "PASS" : "FAIL", path.getFileName(), note);
    }
}
