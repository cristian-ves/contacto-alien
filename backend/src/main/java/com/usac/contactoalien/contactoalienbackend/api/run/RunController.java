package com.usac.contactoalien.contactoalienbackend.api.run;

import com.usac.contactoalien.contactoalienbackend.api.compile.CompileRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
Interactive execution: POST /api/run starts a session, GET .../stream follows its output
(server-sent events), POST .../input feeds its standard input and POST .../stop ends it.
*/
@RestController
@RequestMapping("/api/run")
public class RunController {

    private final RunService runService;
    private final RunStreamer streamer;

    public RunController(RunService runService, RunStreamer streamer) {
        this.runService = runService;
        this.streamer = streamer;
    }

    @PostMapping
    public RunResponse start(@RequestBody CompileRequest request) {
        return runService.start(request);
    }

    @GetMapping("/{id}/stream")
    public SseEmitter stream(@PathVariable String id,
                             @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {
        return streamer.open(runService.require(id), firstIndexAfter(lastEventId));
    }

    @PostMapping("/{id}/input")
    public ResponseEntity<Void> input(@PathVariable String id, @RequestBody RunInputRequest request) {
        runService.input(id, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/stop")
    public ResponseEntity<Void> stop(@PathVariable String id) {
        runService.stop(id);
        return ResponseEntity.noContent().build();
    }

    // A reconnecting client sends the id of the last entry it received; delivery resumes right after it.
    private int firstIndexAfter(String lastEventId) {
        try {
            return lastEventId == null ? 0 : Integer.parseInt(lastEventId.trim()) + 1;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
