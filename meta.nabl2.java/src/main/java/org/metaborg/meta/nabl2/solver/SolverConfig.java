package org.metaborg.meta.nabl2.solver;


import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.relations.IRelations;
import org.metaborg.meta.nabl2.relations.terms.FunctionTerms;
import org.metaborg.meta.nabl2.relations.terms.RelationTerms;
import org.metaborg.meta.nabl2.scopegraph.terms.ResolutionParameters;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.util.functions.PartialFunction1;

import io.usethesource.capsule.Map;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class SolverConfig {

    @Value.Parameter public abstract ResolutionParameters getResolutionParams();

    @Value.Parameter public abstract IRelations.Immutable<ITerm> getRelations();

    @Value.Parameter public abstract Map.Immutable<String, PartialFunction1<ITerm, ITerm>> getFunctions();

    public static IMatcher<SolverConfig> matcher() {
        return M.tuple3(ResolutionParameters.matcher(), RelationTerms.relations(), FunctionTerms.functions(),
                (t, resolutionParams, relations, functions) -> {
                    return ImmutableSolverConfig.of(resolutionParams, relations, functions);
                });
    }

}