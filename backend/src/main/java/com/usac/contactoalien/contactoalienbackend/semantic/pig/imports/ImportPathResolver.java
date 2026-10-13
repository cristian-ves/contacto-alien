package com.usac.contactoalien.contactoalienbackend.semantic.pig.imports;

import com.usac.contactoalien.contactoalienbackend.grammar.pig.PigParser;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;

// Resolves a Pig Latin importDecl (IDENTIFIER (DOT IDENTIFIER)*, last segment 'y' or 'z') into
// the virtual file path, source language and expected module name that the loader needs, without
// touching the filesystem itself.
public final class ImportPathResolver {

    private ImportPathResolver() {}

    public enum Language { Y, Z }

    public static final class ResolvedImport {
        private final String virtualPath;
        private final Language language;
        private final String expectedModuleName;
        private final String importText;

        ResolvedImport(String virtualPath, Language language, String expectedModuleName, String importText) {
            this.virtualPath = virtualPath;
            this.language = language;
            this.expectedModuleName = expectedModuleName;
            this.importText = importText;
        }

        public String getVirtualPath() { return virtualPath; }
        public Language getLanguage() { return language; }
        public String getExpectedModuleName() { return expectedModuleName; }
        public String getImportText() { return importText; }
    }

    public static ResolvedImport resolve(PigParser.ImportDeclContext ctx) {
        List<String> segments = new ArrayList<>();
        for (TerminalNode t : ctx.IDENTIFIER()) {
            segments.add(t.getText());
        }

        String extText = segments.get(segments.size() - 1);
        Language language;
        if (extText.equals("y")) {
            language = Language.Y;
        } else if (extText.equals("z")) {
            language = Language.Z;
        } else {
            throw new IllegalArgumentException("Unsupported import extension '" + extText + "' in '" + ctx.getText() + "'");
        }

        String baseName = segments.get(segments.size() - 2);
        List<String> folderSegments = segments.subList(0, segments.size() - 2);
        String virtualPath = folderSegments.isEmpty()
                ? baseName + "." + extText
                : String.join("/", folderSegments) + "/" + baseName + "." + extText;

        String importText = String.join(".", segments);
        return new ResolvedImport(virtualPath, language, baseName, importText);
    }
}