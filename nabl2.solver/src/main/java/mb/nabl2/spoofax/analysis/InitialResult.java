package mb.nabl2.spoofax.analysis;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Optional;
import java.util.Set;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.constraints.Constraints;
import mb.nabl2.constraints.IConstraint;
import mb.nabl2.solver.SolverConfig;
import mb.nabl2.stratego.ConstraintTerms;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class InitialResult {

    @Value.Parameter public abstract Set<IConstraint> getConstraints();

    @Value.Parameter public abstract Args getArgs();

    @Value.Parameter public abstract SolverConfig getConfig();

    @Value.Auxiliary public abstract Optional<ITerm> getCustomResult();

    public abstract InitialResult withCustomResult(Optional<? extends ITerm> customResult);

    public static IMatcher<InitialResult> matcher() {
        return M.appl3("InitialResult", ConstraintTerms.specialize(Constraints.matchConstraintOrList()),
                ConstraintTerms.specialize(Args.matcher()), ConstraintTerms.specialize(SolverConfig.matcher()),
                (t, constraint, args, config) -> {
                    return ImmutableInitialResult.of(ImmutableSet.of(constraint), args, config);
                });
    }

}