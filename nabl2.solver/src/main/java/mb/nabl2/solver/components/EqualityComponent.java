package mb.nabl2.solver.components;

import java.util.Optional;
import java.util.Set;

import org.metaborg.util.Ref;

import com.google.common.collect.Sets;

import mb.nabl2.constraints.IConstraint;
import mb.nabl2.constraints.equality.CEqual;
import mb.nabl2.constraints.equality.CInequal;
import mb.nabl2.constraints.equality.IEqualityConstraint;
import mb.nabl2.constraints.messages.IMessageInfo;
import mb.nabl2.constraints.messages.MessageContent;
import mb.nabl2.solver.ASolver;
import mb.nabl2.solver.ISolver.SeedResult;
import mb.nabl2.solver.ISolver.SolveResult;
import mb.nabl2.solver.ImmutableSeedResult;
import mb.nabl2.solver.ImmutableSolveResult;
import mb.nabl2.solver.SolverCore;
import mb.nabl2.solver.messages.IMessages;
import mb.nabl2.solver.messages.Messages;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.UnificationException;
import mb.nabl2.unification.UnificationMessages;

public class EqualityComponent extends ASolver {

    private final Ref<IUnifier.Immutable> unifier;

    public EqualityComponent(SolverCore core, Ref<IUnifier.Immutable> unifier) {
        super(core);
        this.unifier = unifier;
    }

    public SeedResult seed(IUnifier.Immutable solution, IMessageInfo message) throws InterruptedException {
        final Set<IConstraint> constraints = Sets.newHashSet();
        final IMessages.Transient messages = Messages.Transient.of();
        try {
            final IUnifier.Transient unifier = this.unifier.get().melt();
            unifier.unify(solution);
            this.unifier.set(unifier.freeze());
        } catch(UnificationException e) {
            messages.add(message.withContent(UnificationMessages.getError(e.getLeft(), e.getRight())));
        }
        return ImmutableSeedResult.builder().constraints(constraints).messages(messages.freeze()).build();
    }

    public Optional<SolveResult> solve(IEqualityConstraint constraint) {
        return constraint.match(IEqualityConstraint.Cases.of(this::solve, this::solve));
    }

    public IUnifier.Immutable finish() {
        return unifier.get();
    }

    // ------------------------------------------------------------------------------------------------------//

    private Optional<SolveResult> solve(CEqual constraint) {
        final ITerm left = constraint.getLeft();
        final ITerm right = constraint.getRight();
        try {
            final IUnifier.Transient unifier = this.unifier.get().melt();
            final IUnifier.Immutable unifyResult = unifier.unify(left, right);
            final SolveResult solveResult = ImmutableSolveResult.builder().unifierDiff(unifyResult).build();
            this.unifier.set(unifier.freeze());
            return Optional.of(solveResult);
        } catch(UnificationException ex) {
            final MessageContent content = MessageContent.builder().append("Cannot unify ").append(left)
                    .append(" with ").append(right).build();
            final IMessageInfo message = (constraint.getMessageInfo().withDefaultContent(content));
            final SolveResult solveResult = SolveResult.messages(message);
            return Optional.of(solveResult);
        }
    }

    private Optional<SolveResult> solve(CInequal constraint) {
        ITerm left = constraint.getLeft();
        ITerm right = constraint.getRight();
        if(unifier().areEqual(left, right)) {
            MessageContent content = MessageContent.builder().append(constraint.getLeft().toString()).append(" and ")
                    .append(constraint.getRight().toString()).append(" must be inequal, but are not.").build();
            IMessageInfo message = constraint.getMessageInfo().withDefaultContent(content);
            return Optional.of(SolveResult.messages(message));
        } else {
            return unifier().areUnequal(left, right) ? Optional.of(SolveResult.empty()) : Optional.empty();
        }
    }

    // ------------------------------------------------------------------------------------------------------//

}