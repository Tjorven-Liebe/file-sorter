package de.tjorven.algorithm;

import de.tjorven.MetadataListUI;
import lombok.Getter;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public class FolderAnalyser {

    private final Map<String, Map<String, String>> metadata = new ConcurrentHashMap<>();

    public FolderAnalyser(Path rootPath) throws IOException {
        this.readToMap(rootPath);
    }

    private void readToMap(Path path) throws IOException {
        try (Stream<Path> stream = Files.list(path)) {
            stream.parallel().filter(Files::isRegularFile).forEach(file -> {
                try (InputStream is = Files.newInputStream(file)) {
                    TikaConfig config = new TikaConfig(this.getClass().getClassLoader());
                    AutoDetectParser parser = new AutoDetectParser(config);

                    Metadata tikaMetadata = new Metadata();
                    parser.parse(is, new BodyContentHandler(-1), tikaMetadata);

                    Map<String, String> fileMeta = new HashMap<>();
                    for (String name : tikaMetadata.names()) {
                        fileMeta.put(name, tikaMetadata.get(name));
                    }

                    this.metadata.put(file.getFileName().toString(), fileMeta);
                } catch (Exception e) {
                    MetadataListUI.getLogger().error("Could not parse: {}", file, e);
                }
            });
        }
    }

    public List<Map.Entry<String, Integer>> findSimilarities() throws IOException {
        Map<String, Integer> matches = new HashMap<>();
        this.metadata.values().forEach(map ->
                map.keySet().forEach(key -> matches.merge(key, 1, Integer::sum))
        );

        Set<String> blacklist = this.loadBlacklist();

        return matches.entrySet().stream()
                .filter(entry -> blacklist.stream().noneMatch(b -> entry.getKey().startsWith(b)))
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .toList();
    }

    public Set<String> getFileNames() {
        return Collections.unmodifiableSet(this.metadata.keySet());
    }

    private Set<String> loadBlacklist() {
        Path path = Paths.get("blacklist.txt");
        if (!Files.exists(path)) return Collections.emptySet();
        try (Stream<String> lines = Files.lines(path)) {
            return lines.filter(line -> !line.isBlank()).collect(Collectors.toSet());
        } catch (IOException e) {
            return Collections.emptySet();
        }
    }
}