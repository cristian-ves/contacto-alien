package com.usac.contactoalien.contactoalienbackend;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.usac.contactoalien.contactoalienbackend.grammar.pig.PigLexer;
import com.usac.contactoalien.contactoalienbackend.grammar.pig.PigParser;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.analysis.PigSemanticAnalyzer;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.imports.PigImportLoader;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.imports.PigImportResult;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.errors.PigSemanticError;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class ContactoAlienBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ContactoAlienBackendApplication.class, args);

		// TEMPORARY: full Pig Latin pipeline test (imports + variabilesSection + maiorSection)
		runPigPipeline();
	}

	private static void runPigPipeline() {
		String nodoSource =
				"public class Nodo {\n" +
						"    int dato;\n" +
						"    Nodo siguiente;\n" +
						"    public Nodo(int dato1) {\n" +
						"        dato = dato1;\n" +
						"        siguiente = null;\n" +
						"    }\n" +
						"    public int getDato() {\n" +
						"        return dato;\n" +
						"    }\n" +
						"    public void setDato(int dato1) {\n" +
						"        dato = dato1;\n" +
						"    }\n" +
						"    public Nodo getSiguiente() {\n" +
						"        return siguiente;\n" +
						"    }\n" +
						"    public void setSiguiente(Nodo siguiente1) {\n" +
						"        siguiente = siguiente1;\n" +
						"    }\n" +
						"}\n";

		String pilaSource =
				"public class Pila {\n" +
						"    private Nodo cima;\n" +
						"    private int tamanio;\n" +
						"    public Pila() {\n" +
						"        this.cima = null;\n" +
						"        this.tamanio = 0;\n" +
						"    }\n" +
						"    public void apilar(int dato) {\n" +
						"        Nodo nuevo = new Nodo(dato);\n" +
						"        nuevo.setSiguiente(cima);\n" +
						"        cima = nuevo;\n" +
						"        tamanio++;\n" +
						"    }\n" +
						"    public int desapilar() {\n" +
						"        int dato = cima.getDato();\n" +
						"        cima = cima.getSiguiente();\n" +
						"        tamanio--;\n" +
						"        return dato;\n" +
						"    }\n" +
						"    public int obtenerCima() {\n" +
						"        if (estaVacia()) {\n" +
						"            return null;\n" +
						"        }\n" +
						"        return cima.getDato();\n" +
						"    }\n" +
						"    public boolean estaVacia() {\n" +
						"        return cima == null;\n" +
						"    }\n" +
						"    public int obtenerTamanio() {\n" +
						"        return tamanio;\n" +
						"    }\n" +
						"    public String toString() {\n" +
						"        String resultado = \"Pila [Cima -> Base]: \";\n" +
						"        Nodo actual = cima;\n" +
						"        while (actual != null) {\n" +
						"            resultado = resultado + actual.getDato();\n" +
						"            if (actual.getSiguiente() != null) {\n" +
						"                resultado = resultado + \", \";\n" +
						"            }\n" +
						"            actual = actual.getSiguiente();\n" +
						"        }\n" +
						"        return resultado;\n" +
						"    }\n" +
						"}\n";

		String utilsYSource =
				"%funciones\n" +
						"definir imprimirBienvenida():\n" +
						"   imprimir(\"------------------------------------------\")\n" +
						"   imprimir(\"Este es mi primer programa a bajo nivel :D\")\n" +
						"   imprimit(\"With <3 by IGriega\")\n" +
						"   imprimir(\"------------------------------------------\")\n";

		String mainPigSource =
				"import Nodo.z\n" +
						"import Pila.z\n" +
						"import utils.utils.y\n" +
						"\n" +
						"VARIABILES>\n" +
						"esto pila : novus Pila();\n" +
						"esto opcion : numerus -1;\n" +
						"esto lectura : numerus 0;\n" +
						"\n" +
						"MAIOR>\n" +
						"imprimirBienvenida();\n" +
						"\n" +
						"dum (opcion != 4) {\n" +
						"    >> \"-----------------------------------------------\"\n" +
						"    >> \"Ingresa la accion: \\n\";\n" +
						"    >> \"1. Ingresar en pila \\n\";\n" +
						"    >> \"2. Sacar de pila \\n\";\n" +
						"    >> \"3. Imprimir pila \\n\";\n" +
						"    >> \"4. Salir \\n\";\n" +
						"    >> \"-----------------------------------------------\"\n" +
						"    opcion \n" +
						"    si (opcion == 1) {\n" +
						"        >> \"Ingresa el numero: \\n\";\n" +
						"        lectura \n" +
						"        pila.apilar(lectura);\n" +
						"    } aliter (opcion == 2 ) { \n" +
						"        lectura = pila.desapilar();\n" +
						"        >> \"Elemento desapilado: \";\n" +
						"        >> lectura;\n" +
						"    } aliter (opcion == 3 ) { \n" +
						"        >> pila.toString();\n" +
						"    } aliter (opcion == 4 ) { \n" +
						"        >> \"Fin del programa\"\n" +
						"    } finis; \n" +
						"\n" +
						"    >> \"Ingresa cualquier tecla para continuar \"\n" +
						"    lectura \n" +
						"\n" +
						"} finis; \n" +
						"\n" +
						"FINIS;\n";

		Map<String, String> virtualFiles = new LinkedHashMap<>();
		virtualFiles.put("Nodo.z", nodoSource);
		virtualFiles.put("Pila.z", pilaSource);
		virtualFiles.put("utils/utils.y", utilsYSource);

		PigLexer lexer = new PigLexer(CharStreams.fromString(mainPigSource));
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		PigParser parser = new PigParser(tokens);
		PigParser.ProgramContext programTree = parser.program();

		System.out.println("=== Pig Latin pipeline: main.pig ===");
		if (parser.getNumberOfSyntaxErrors() > 0) {
			System.out.println("Syntax errors found while parsing main.pig: " + parser.getNumberOfSyntaxErrors());
			return;
		}

		PigImportResult importResult = new PigImportLoader().load(programTree.importDecl(), virtualFiles);
		List<PigSemanticError> allErrors = new PigSemanticAnalyzer().analyze(programTree, importResult);

		if (allErrors.isEmpty()) {
			System.out.println("(no errors)");
		} else {
			for (PigSemanticError error : allErrors) {
				System.out.println(error);
			}
		}
		System.out.println("Total: " + allErrors.size());
	}
}