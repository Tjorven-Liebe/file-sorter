package de.tjorven.folder;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class FolderAnalyser {

    Map<String, Map<String, String>> metadatas = new HashMap<>();

    public FolderAnalyser(Path rootPath) throws IOException {
        this.readToMap(rootPath);
    }

    private void readToMap(Path path) throws IOException {
        Stream<Path> list = Files.list(path);
        list.forEach(file -> {
            try {
                if (file.toFile().isDirectory()) {
                    return;
                }

                FileInputStream stream = new FileInputStream(file.toFile());

                Metadata metadata = new Metadata();
                AutoDetectParser parser = new AutoDetectParser();

                // The handler is for text content; metadata is filled during parsing
                parser.parse(stream, new BodyContentHandler(), metadata);

                Map<String, String> metadatas = new HashMap<>();

                for (String name : metadata.names()) {
                    metadatas.put(name, metadata.get(name));
                }

                this.metadatas.put(file.toFile().getName(), metadatas);
            } catch (IOException | SAXException | TikaException e) {
                Logger.getLogger("default").log(Level.SEVERE, "Error reading files", e);
            }
        });
    }

    public String[] getFileNames() {
        return this.metadatas.keySet().toArray(String[]::new);
    }

    public Map<String, String> getMetadata(String file) {
        return metadatas.get(file);
    }

    public List<Map.Entry<String, Integer>> findSimilarities() throws FileNotFoundException {
        Map<String, Integer> matches = new HashMap<>();
        this.metadatas.forEach((file, metadata) -> {
            for (String metaKey : metadata.keySet()) {
                matches.put(metaKey, matches.getOrDefault(metaKey, 0) + 1);
            }
        });

        File file = new File("blacklist.txt");
        FileReader fileReader = new FileReader(file);
        BufferedReader reader = new BufferedReader(fileReader);
        List<String> lines = reader.lines().toList();

        return matches.entrySet().stream()
                .filter(stringIntegerEntry -> {
                    for (String line : lines) {
                        if (stringIntegerEntry.getKey().startsWith(line)) {
                            return false;
                        }
                    }
                    return true;
                })
                .sorted((stringIntegerEntry, t1) -> t1.getValue())
                .toList();
    }
}
