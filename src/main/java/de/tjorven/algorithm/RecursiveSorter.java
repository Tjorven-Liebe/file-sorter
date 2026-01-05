package de.tjorven.algorithm;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Getter
@RequiredArgsConstructor
public class RecursiveSorter {

    private final String selectedFolderPath;
    private final Map<String, Map<String, String>> fileMetadataMap;

    private final List<MoveHistory> lastOperationHistory = new ArrayList<>();

    public void runFilter(Path rootPath, List<String> selectedAttributes, List<String> allowedExtensions) throws IOException {
        this.lastOperationHistory.clear();
        List<Path> filesInFolder;

        try (Stream<Path> stream = Files.list(rootPath)) {
            filesInFolder = stream.filter(Files::isRegularFile)
                    .filter(path -> {
                        if (allowedExtensions == null || allowedExtensions.isEmpty()) return true;
                        String name = path.getFileName().toString().toLowerCase();
                        return allowedExtensions.stream().anyMatch(ext -> name.endsWith(ext.toLowerCase()));
                    })
                    .toList();
        }

        for (Path filePath : filesInFolder) {
            Map<String, String> fileMeta = this.fileMetadataMap.get(filePath.getFileName().toString());
            if (fileMeta == null) continue;

            Path currentTargetDir = rootPath;
            for (String attr : selectedAttributes) {
                String rawValue = fileMeta.get(attr);
                String folderName = (rawValue == null || rawValue.isBlank()) ? "Unknown_" + attr : rawValue.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
                currentTargetDir = currentTargetDir.resolve(folderName);
            }

            if (!Files.exists(currentTargetDir)) Files.createDirectories(currentTargetDir);

            Path targetFile = currentTargetDir.resolve(filePath.getFileName());
            Files.move(filePath, targetFile, StandardCopyOption.REPLACE_EXISTING);
            this.lastOperationHistory.add(new MoveHistory(filePath, targetFile));
        }
    }

    public void revertSort() throws IOException {
        if (this.lastOperationHistory.isEmpty()) {
            return;
        }

        for (MoveHistory move : this.lastOperationHistory) {
            if (!Files.exists(move.target())) {
                continue;
            }

            Files.move(move.target(), move.source(), StandardCopyOption.REPLACE_EXISTING);
        }

        this.cleanUpEmptyFolders();
        this.lastOperationHistory.clear();
    }

    private void cleanUpEmptyFolders() {
        this.lastOperationHistory.stream()
                .map(move -> move.target().getParent())
                .distinct()
                .forEach(dir -> {
                    try {
                        while (dir != null && !dir.equals(Paths.get(this.selectedFolderPath))) {
                            try (Stream<Path> s = Files.list(dir)) {
                                if (s.findAny().isPresent()) {
                                    break;
                                }

                                Files.delete(dir);
                                dir = dir.getParent();
                            }
                        }
                    } catch (IOException ignored) {
                    }
                });
    }

    public record MoveHistory(Path source, Path target) {
    }
}
