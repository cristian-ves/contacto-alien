package com.usac.contactoalien.contactoalienbackend.codegen.exec.interactive;

import com.usac.contactoalien.contactoalienbackend.codegen.exec.ExecMessages;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
Copies one output stream of the program into the session log as text, as soon as bytes arrive.
Bytes are decoded incrementally so a character split across two reads is not damaged. Only the
first maxBytes are kept; the rest is read and discarded so the program never blocks on a full pipe.
*/
final class StreamPump implements Runnable {

    private static final int BUFFER_BYTES = 4096;
    // The longest UTF-8 sequence, kept between reads when a character is cut in half.
    private static final int MAX_CHARACTER_BYTES = 4;

    private final InputStream stream;
    private final RunEvent.Kind kind;
    private final EventLog log;
    private final int maxBytes;
    private final long settleMillis;
    private final Runnable onActivity;
    private final CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
    private final ByteBuffer pending = ByteBuffer.allocate(BUFFER_BYTES + MAX_CHARACTER_BYTES);

    /**
    settleMillis delays what is read from this stream before it is logged. Standard error uses a few
    milliseconds so text the program wrote to standard output just before it lands in the log first.
    */
    StreamPump(InputStream stream, RunEvent.Kind kind, EventLog log, int maxBytes, long settleMillis,
               Runnable onActivity) {
        this.stream = stream;
        this.kind = kind;
        this.log = log;
        this.maxBytes = maxBytes;
        this.settleMillis = settleMillis;
        this.onActivity = onActivity;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[BUFFER_BYTES];
        long total = 0;
        boolean noticeLogged = false;
        try {
            int read;
            while ((read = stream.read(buffer)) != -1) {
                int keep = (int) Math.min(read, Math.max(0, maxBytes - total));
                total += read;
                if (keep > 0) {
                    pause();
                    onActivity.run();
                    append(buffer, keep);
                }
                if (total > maxBytes && !noticeLogged) {
                    noticeLogged = true;
                    log.appendText(RunEvent.Kind.SYSTEM, ExecMessages.OUTPUT_LIMIT_REACHED);
                }
            }
        } catch (IOException e) {
            // The stream closes when the process is killed; whatever was read is already in the log.
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        flush();
    }

    private void pause() throws InterruptedException {
        if (settleMillis > 0) {
            Thread.sleep(settleMillis);
        }
    }

    private void append(byte[] buffer, int count) {
        pending.put(buffer, 0, count);
        pending.flip();
        CharBuffer text = CharBuffer.allocate(pending.remaining());
        decoder.decode(pending, text, false);
        pending.compact();
        emit(text);
    }

    // At the end of the stream, a character that never completed is reported as a replacement character.
    private void flush() {
        pending.flip();
        CharBuffer text = CharBuffer.allocate(pending.remaining() + 1);
        decoder.decode(pending, text, true);
        decoder.flush(text);
        pending.clear();
        emit(text);
    }

    private void emit(CharBuffer text) {
        text.flip();
        if (text.hasRemaining()) {
            log.appendText(kind, text.toString());
        }
    }
}
