package com.usac.contactoalien.contactoalienbackend.semantic.analysis;

import com.usac.contactoalien.contactoalienbackend.semantic.symbols.CompilationUnitRegistry;
import com.usac.contactoalien.contactoalienbackend.semantic.types.TypeCompatibility;
import com.usac.contactoalien.contactoalienbackend.semantic.types.ZType;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
Chooses the overload a call refers to from the static types of its arguments: a candidate whose
parameter types are exactly the argument types wins, otherwise the first candidate to which every
argument can be assigned (int to double, null to a reference). Callers pass only candidates with
the right number of parameters.
*/
public final class OverloadResolver {

    private OverloadResolver() {
    }

    public static <T> T pick(List<T> candidates, Function<T, List<ZType>> parameters, List<ZType> arguments,
                      CompilationUnitRegistry registry) {
        T assignable = null;
        for (T candidate : candidates) {
            List<ZType> types = parameters.apply(candidate);
            if (types.equals(arguments)) {
                return candidate;
            }
            if (assignable == null && accepts(types, arguments, registry)) {
                assignable = candidate;
            }
        }
        return assignable;
    }

    // "(int, string)" for messages.
    public static String describe(List<ZType> arguments) {
        return arguments.stream().map(ZType::toString).collect(Collectors.joining(", ", "(", ")"));
    }

    private static boolean accepts(List<ZType> parameters, List<ZType> arguments, CompilationUnitRegistry registry) {
        for (int i = 0; i < parameters.size(); i++) {
            if (!TypeCompatibility.isAssignable(parameters.get(i), arguments.get(i), registry)) {
                return false;
            }
        }
        return true;
    }
}
