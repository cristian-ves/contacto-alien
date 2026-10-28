package com.usac.contactoalien.contactoalienbackend.codegen.exec;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
Reads a process stream on its own thread so the child never blocks on a full pipe. Only the
first maxBytes are kept; the rest is read and discarded.
*/
final class OutputCollector implements Runnable {

    private final InputStream stream;
    private final int maxBytes;
    private final ByteArrayOutputStream kept = new ByteArrayOutputStream();
    private volatile boolean truncated;

    OutputCollector(InputStream stream, int maxBytes) {
        this.stream = stream;
        this.maxBytes = maxBytes;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[4096];
        try {
            int read;
            while ((read = stream.read(buffer)) != -1) {
                int room = maxBytes - kept.size();
                if (room > 0) {
                    kept.write(buffer, 0, Math.min(read, room));
                }
                if (read > room) {
                    truncated = true;
                }
            }
        } catch (IOException e) {
            // The stream closes when the process is killed; whatever was read is still reported.
        }
    }

    String text() {
        String text = kept.toString(StandardCharsets.UTF_8);
        return truncated ? text + ExecMessages.OUTPUT_TRUNCATED : text;
    }
}
