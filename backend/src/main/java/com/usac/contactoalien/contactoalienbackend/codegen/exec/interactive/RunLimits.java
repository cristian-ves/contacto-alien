package com.usac.contactoalien.contactoalienbackend.codegen.exec.interactive;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
Limits of interactive runs (compiler.run.* in application.properties). The CPU budget is not here: it
reuses compiler.target.timeout-seconds, so a program is allowed the same CPU time as in a batch run.
Time spent waiting for the user to type does not use CPU, so it never counts against that budget;
wallSeconds and idleSeconds bound the rest.
*/
@ConfigurationProperties(prefix = "compiler.run")
public record RunLimits(@DefaultValue("5") int maxSessions,
                        @DefaultValue("600") int wallSeconds,
                        @DefaultValue("300") int idleSeconds,
                        @DefaultValue("5") int detachGraceSeconds,
                        @DefaultValue("30") int finishedRetentionSeconds,
                        @DefaultValue("16384") int maxPendingInputBytes) {
}
