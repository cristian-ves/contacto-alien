package com.usac.contactoalien.contactoalienbackend.codegen.gen;

import com.usac.contactoalien.contactoalienbackend.codegen.gen.index.ProgramIndex;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.pig.PigProgramGenerator;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.y.YUnitGenerator;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.z.ZClassGenerator;
import com.usac.contactoalien.contactoalienbackend.codegen.quad.Quad;
import com.usac.contactoalien.contactoalienbackend.codegen.quad.QuadBuilder;
import com.usac.contactoalien.contactoalienbackend.errors.CompilerError;
import com.usac.contactoalien.contactoalienbackend.grammar.ParseOutcome;
import com.usac.contactoalien.contactoalienbackend.grammar.SourceParser;
import com.usac.contactoalien.contactoalienbackend.grammar.pig.PigParser;
import com.usac.contactoalien.contactoalienbackend.grammar.y.YParser;
import com.usac.contactoalien.contactoalienbackend.grammar.zetariano.ZetarianoParser;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.imports.ImportPathResolver;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
Entry point of quadruple generation for a program that already passed validation: parses the
Pig Latin file and every file it imports, builds the index of callable functions and classes,
and translates the main program, the Y? functions and the Zetariano classes in that order.
Zetariano files that sit in the same folder as an imported class are included too, because
classes refer to their siblings without importing them.
*/
public final class ProgramGenerator {

    public List<Quad> generate(String pigCode, Map<String, String> files) {
        PigParser.ProgramContext program = parse(SourceParser.parsePig(pigCode));
        Map<String, YParser.ProgramContext> yTrees = new LinkedHashMap<>();
        Map<String, ZetarianoParser.ClassDeclarationContext> zTrees = new LinkedHashMap<>();
        loadImports(program, files, yTrees, zTrees);

        ProgramIndex index = new ProgramIndex();
        registerClasses(zTrees, index);
        for (Map.Entry<String, YParser.ProgramContext> y : yTrees.entrySet()) {
            inFile(y.getKey(), () -> YUnitGenerator.register(y.getValue(), index));
        }

        QuadBuilder quads = new QuadBuilder();
        ZClassGenerator.declareMethodTables(index, quads);
        new PigProgramGenerator(program, quads, index).generate();
        for (Map.Entry<String, YParser.ProgramContext> y : yTrees.entrySet()) {
            inFile(y.getKey(), () -> new YUnitGenerator(y.getValue(), quads, index).generate());
        }
        for (Map.Entry<String, ZetarianoParser.ClassDeclarationContext> z : zTrees.entrySet()) {
            inFile(z.getKey(), () -> new ZClassGenerator(z.getValue(), quads, index).generate());
        }
        return quads.quads();
    }

    /**
    Registers the Zetariano classes with a superclass after it, since a class starts from the layout and
    the method table of its parent. Ids follow the order of the files.
    */
    private void registerClasses(Map<String, ZetarianoParser.ClassDeclarationContext> zTrees, ProgramIndex index) {
        Map<String, Integer> ids = new LinkedHashMap<>();
        int nextId = 1;
        for (String path : zTrees.keySet()) {
            ids.put(path, nextId++);
        }
        Map<String, ZetarianoParser.ClassDeclarationContext> pending = new LinkedHashMap<>(zTrees);
        while (!pending.isEmpty()) {
            boolean progress = false;
            for (Map.Entry<String, ZetarianoParser.ClassDeclarationContext> z : List.copyOf(pending.entrySet())) {
                String superName = ZClassGenerator.superNameOf(z.getValue());
                boolean parentReady = superName == null || index.classInfo(superName) != null
                        || pending.values().stream().noneMatch(other -> other.IDENTIFIER(0).getText().equals(superName));
                if (parentReady) {
                    inFile(z.getKey(), () -> ZClassGenerator.register(z.getValue(), index, ids.get(z.getKey())));
                    pending.remove(z.getKey());
                    progress = true;
                }
            }
            if (!progress) {
                throw new GenerationException(GenerationMessages.INTERNAL, 1, 1);
            }
        }
    }

    // A problem found while translating an imported file names that file, like the semantic errors do.
    private void inFile(String path, Runnable translation) {
        try {
            translation.run();
        } catch (GenerationException e) {
            throw new GenerationException("[" + path + "] " + e.getMessage(), e.getLine(), e.getColumn());
        }
    }

    private void loadImports(PigParser.ProgramContext program, Map<String, String> files,
                             Map<String, YParser.ProgramContext> yTrees,
                             Map<String, ZetarianoParser.ClassDeclarationContext> zTrees) {
        Map<String, String> zSources = new LinkedHashMap<>();
        for (PigParser.ImportDeclContext declaration : program.importDecl()) {
            ImportPathResolver.ResolvedImport resolved = ImportPathResolver.resolve(declaration);
            String path = resolved.getVirtualPath();
            String source = files == null ? null : files.get(path);
            if (source == null) {
                throw new GenerationException(GenerationMessages.MISSING_IMPORT + path, declaration);
            }
            if (resolved.getLanguage() == ImportPathResolver.Language.Y) {
                yTrees.put(path, parse(SourceParser.parseY(source)));
            } else {
                zSources.put(path, source);
                addSiblings(path, files, zSources);
            }
        }
        for (Map.Entry<String, String> source : zSources.entrySet()) {
            zTrees.put(source.getKey(), parse(SourceParser.parseZetariano(source.getValue())));
        }
    }

    private void addSiblings(String path, Map<String, String> files, Map<String, String> zSources) {
        String folder = path.substring(0, path.lastIndexOf('/') + 1);
        for (Map.Entry<String, String> file : files.entrySet()) {
            String other = file.getKey();
            boolean sameFolder = other.startsWith(folder) && other.indexOf('/', folder.length()) < 0;
            if (other.endsWith(".z") && sameFolder) {
                zSources.putIfAbsent(other, file.getValue());
            }
        }
    }

    // The program was validated before, so a parse error here means the inputs changed in between.
    private <T> T parse(ParseOutcome<T> outcome) {
        if (outcome.hasErrors()) {
            CompilerError first = outcome.errors().get(0);
            throw new GenerationException(first.message(), first.line(), first.column());
        }
        return outcome.tree();
    }
}
