package com.usac.contactoalien.contactoalienbackend.api.run;

import com.usac.contactoalien.contactoalienbackend.codegen.exec.interactive.RunEvent;
import com.usac.contactoalien.contactoalienbackend.codegen.exec.interactive.RunSession;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
Follows a session's event log and writes each entry to an SSE connection as it appears. When
nothing happens for a few seconds it sends a comment line: that keeps proxies from closing the
connection and, because the write fails once the browser is gone, tells the session nobody is watching.
*/
@Component
public class RunStreamer {

    private static final long HEARTBEAT_MILLIS = 3000;
    private static final String HEARTBEAT_COMMENT = "keepalive";
    // No server-side timeout: the session's own limits end the stream.
    private static final long NO_TIMEOUT = 0L;

    public record TextPayload(String text) {
    }

    public record ExitPayload(int exitCode, long durationMs, long cpuMs, String note) {
    }

    // Starts delivering events from the given log index (0 for everything, or the one after Last-Event-ID).
    public SseEmitter open(RunSession session, int fromIndex) {
        SseEmitter emitter = new SseEmitter(NO_TIMEOUT);
        session.streamAttached();
        Thread pump = Thread.ofVirtual().start(() -> pump(session, emitter, fromIndex));
        emitter.onCompletion(pump::interrupt);
        emitter.onTimeout(pump::interrupt);
        emitter.onError(error -> pump.interrupt());
        return emitter;
    }

    private void pump(RunSession session, SseEmitter emitter, int fromIndex) {
        int index = fromIndex;
        try {
            while (true) {
                RunEvent event = session.awaitEvent(index, HEARTBEAT_MILLIS);
                if (event == null) {
                    emitter.send(SseEmitter.event().comment(HEARTBEAT_COMMENT));
                    continue;
                }
                emitter.send(SseEmitter.event().id(Integer.toString(event.id())).name(event.kind().eventName())
                        .data(payload(event), MediaType.APPLICATION_JSON));
                index++;
                if (event.isExit()) {
                    break;
                }
            }
            emitter.complete();
        } catch (IOException | IllegalStateException e) {
            // The browser closed the connection.
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            session.streamDetached();
        }
    }

    private Object payload(RunEvent event) {
        return event.isExit()
                ? new ExitPayload(event.exitCode(), event.durationMs(), event.cpuMs(), event.note())
                : new TextPayload(event.text());
    }
}
