package com.usac.contactoalien.contactoalienbackend.codegen.exec.interactive;

import com.usac.contactoalien.contactoalienbackend.codegen.TargetConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
Reads the CPU time a process has used (user plus system) from /proc/pid/stat. A program blocked
waiting for input uses none, which is why the run limit is measured here and not on the wall clock.
*/
final class ProcessCpuClock {

    // Fields 14 (utime) and 15 (stime) of the file; counting from field 3, the one right after the command name.
    private static final int UTIME_INDEX = 11;
    private static final int STIME_INDEX = 12;
    private static final long MILLIS_PER_SECOND = 1000L;

    private ProcessCpuClock() {
    }

    // Returns the milliseconds of CPU used so far, or -1 when the process is gone or the file is unreadable.
    static long cpuMillis(long pid) {
        try {
            String stat = Files.readString(Path.of("/proc", Long.toString(pid), "stat"));
            // The command name sits between parentheses and may contain spaces, so the numbers start after the last ')'.
            String[] fields = stat.substring(stat.lastIndexOf(')') + 2).trim().split(" ");
            long ticks = Long.parseLong(fields[UTIME_INDEX]) + Long.parseLong(fields[STIME_INDEX]);
            return ticks * MILLIS_PER_SECOND / TargetConfig.CLOCK_TICKS_PER_SECOND;
        } catch (IOException | RuntimeException e) {
            return -1;
        }
    }
}
