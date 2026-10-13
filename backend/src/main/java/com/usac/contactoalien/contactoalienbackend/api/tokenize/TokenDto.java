package com.usac.contactoalien.contactoalienbackend.api.tokenize;

// endColumn assumes single-line tokens, true for all literals in the three grammars
public record TokenDto(String type, String text, int line, int startColumn, int endColumn,
                       boolean hidden) {
}