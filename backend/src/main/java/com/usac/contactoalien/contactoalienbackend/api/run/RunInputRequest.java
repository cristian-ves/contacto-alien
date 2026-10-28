package com.usac.contactoalien.contactoalienbackend.api.run;

// Body of POST /api/run/{id}/input: text to send as it is (the console adds the line break) and/or end of input (eof may be omitted).
public record RunInputRequest(String text, Boolean eof) {
}
