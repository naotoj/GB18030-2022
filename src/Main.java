import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.nio.file.Files.newBufferedReader;
import static java.nio.file.Files.newBufferedWriter;

public class Main {
    private static final Charset GB18030 = Charset.forName("GB18030");
    private static final CharsetEncoder GB18030_ENC = GB18030.newEncoder();
    private static final HexFormat HF = HexFormat.of();
    private static final Path ENC_OUT = Path.of("out/enc_out");
    private static final Path DEC_OUT = Path.of("out/dec_out");

    public static void main(String[] args) throws Exception {
        var encMap = new HashMap<>(loadMap("src/MappingTableBMP.txt", true));
        encMap.putAll(loadMap("src/MappingTableSMP.txt", true));
        var decMap = new HashMap<>(loadMap("src/MappingTableBMP.txt", false));
        decMap.putAll(loadMap("src/MappingTableSMP.txt", false));

        // Encoding (Unicode -> GB18030) check
        var output = IntStream.range(0, 0x10FFFF)
                .mapToObj(c -> checkEnc(encMap, c))
                .filter(Objects::nonNull)
                .toList();
        Files.deleteIfExists(ENC_OUT);
        var bw = Files.newBufferedWriter(ENC_OUT, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        for (String str : output) {
            bw.append(str);
            bw.newLine();
        }
        bw.close();

        // Decoding (GB18030 -> Unicode) check
        output = decMap.keySet()
                .stream()
                .sorted()
                .map(b -> checkDec(decMap, b))
                .filter(Objects::nonNull)
                .toList();
        Files.deleteIfExists(DEC_OUT);
        bw = Files.newBufferedWriter(DEC_OUT, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        for (String str : output) {
            bw.append(str);
            bw.newLine();
        }
        bw.close();
    }

    static Map<Integer, Integer> loadMap(String mapfile, boolean isEncoding) throws Exception {
        return Files.readAllLines(Path.of(mapfile)).stream()
                .map(line -> line.split("[\s\t]+"))
                .collect(Collectors.toMap(m -> Integer.parseUnsignedInt(m[isEncoding ? 0 : 1], 16),
                        m -> Integer.parseUnsignedInt(m[isEncoding ? 1 : 0], 16)));
    }

    static String checkEnc(Map<Integer, Integer> mapping, Integer c) {
        var bytes = HF.formatHex(Character.toString(c).getBytes(GB18030)).replaceFirst("^0+", "");
        var mapped = Integer.toUnsignedString(mapping.getOrDefault(c, 0x3f), 16);
        return  mapped.equals("0") || mapped.equals("3f") || bytes.equals(mapped) ?
                null :
                "cp: " + Integer.toHexString(c) + ", map: " + mapped + ", conv: " + bytes;
    }

    static String checkDec(Map<Integer, Integer> mapping, int gbcode) {
        var len = (gbcode & 0xffff0000) != 0 ? 4 : ((gbcode & 0xffffff00) != 0 ? 2 : 1);
        var bb = ByteBuffer.allocate(4);
        switch (len) {
            case 4 -> bb.putInt(gbcode);
            case 2 -> bb.putShort((short)gbcode );
            default -> bb.put((byte)gbcode);
        }
        var cp = Integer.toUnsignedString(new String(bb.array(), GB18030).codePointAt(0), 16);
        var mapped = Integer.toUnsignedString(mapping.getOrDefault(gbcode, 0x3f), 16);
        return  mapped.equals("0") || mapped.equals("3f") || cp.equals(mapped) ?
                null :
                "gbcode: " + Integer.toHexString(gbcode) + ", map: " + mapped + ", conv: " + cp;
    }
}
