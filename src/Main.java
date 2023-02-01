import java.io.OutputStream;
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
    private static final Path OUTFILE = Path.of("out/out");

    public static void main(String[] args) throws Exception {
        var encMap = new HashMap<>(loadMap("src/MappingTableBMP.txt", true));
        encMap.putAll(loadMap("src/MappingTableSMP.txt", true));

        var output = IntStream.range(0, 0x10FFFF)
                .mapToObj(c -> check(encMap, c))
                .filter(Objects::nonNull)
                .toList();
        Files.deleteIfExists(OUTFILE);
        var bw = Files.newBufferedWriter(OUTFILE, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
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

    static String check(Map<Integer, Integer> mapping, Integer c) {
        var bytes = HF.formatHex(Character.toString(c).getBytes(GB18030)).replaceFirst("^0+", "");
        var mapped = Integer.toUnsignedString(mapping.getOrDefault(c, 0x3f), 16);
        return  mapped.equals("0") || mapped.equals("3f") || bytes.equals(mapped) ?
                null :
                "cp: " + Integer.toHexString(c) + ", map: " + mapped + ", conv: " + bytes;
    }
}