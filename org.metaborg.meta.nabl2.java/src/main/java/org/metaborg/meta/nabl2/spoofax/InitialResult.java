package org.metaborg.meta.nabl2.spoofax;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.scopegraph.terms.ResolutionParameters;
import org.metaborg.meta.nabl2.solver.Relations;

@Value.Immutable
@Serial.Version(value = 42L)
public interface InitialResult {

    @Value.Parameter Iterable<IConstraint> getConstraints();

    @Value.Parameter Args getArgs();

    @Value.Parameter ResolutionParameters getResolutionParams();

    @Value.Parameter Relations getRelations();

}