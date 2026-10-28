package com.usac.contactoalien.contactoalienbackend.codegen.exec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

// Throw-away directory under the system temp folder where one program is assembled and run.
public final class ScratchDirectory {

    private static final String PREFIX = "contacto-alien-";

    private ScratchDirectory() {
    }

    public static Path create() throws IOException {
        return Files.createTempDirectory(PREFIX);
    }

    public static void delete(Path directory) {
        if (directory == null) {
            return;
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        } catch (IOException e) {
            // Nothing else to do: the directory lives under the system temp folder.
        }
    }
}
