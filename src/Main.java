import java.io.OutputStream;
import java.nio.charset.Charset;
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

public class Main {
    private static final Charset GB18030 = Charset.forName("GB18030");
    private static final HexFormat HF = HexFormat.of();
    private static final Path OUTFILE = Path.of("out/out");

    public static void main(String[] args) throws Exception {
        var map = new HashMap<>(loadMap("src/MappingTableBMP.txt"));
        map.putAll(loadMap("src/MappingTableSMP.txt"));

//        map.keySet().stream().sorted()
//                .forEach(c -> check(map, c));
        var output = IntStream.range(0, 0x10FFFF)
                .mapToObj(c -> check(map, c))
                .filter(Objects::nonNull)
                .toList();
        Files.deleteIfExists(OUTFILE);
        for (String str : output) {
//            System.out.println(str);
            Files.writeString(OUTFILE, str + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
    }

    static Map<Integer, Integer> loadMap(String mapfile) throws Exception {
        return Files.readAllLines(Path.of(mapfile)).stream()
                .map(line -> line.split("[\s\t]+"))
                .collect(Collectors.toMap(m -> Integer.valueOf(m[0], 16), m -> Integer.parseUnsignedInt(m[1], 16)));
    }

    static String check(Map<Integer, Integer> mapping, Integer c) {
        var bytes = HF.formatHex(Character.toString(c).getBytes(GB18030));
        var mapped = Integer.toUnsignedString(mapping.getOrDefault(c, 0x3f), 16);
        return bytes.equals(mapped) ?
                null :
                "cp: " + Integer.toHexString(c) + ", map: " + mapped + ", conv: " + bytes;
    }
}