package com.usac.contactoalien.contactoalienbackend.codegen.exec;

import java.io.IOException;

/**
A toolchain command (assembler, linker or simulator) could not be started. The message already says which
tool and how to get it, so callers can show it as is.
*/
public class ToolUnavailableException extends IOException {

    public ToolUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
