package mb.nabl2.terms.unification;

import static mb.nabl2.terms.build.TermBuild.B;

import java.io.Serializable;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.metaborg.util.Ref;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.usethesource.capsule.Map;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.Terms;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Set2;
import mb.nabl2.util.Tuple2;

public abstract class PersistentUnifier implements IUnifier, Serializable {

    private static final long serialVersionUID = 42L;

    protected abstract Map<ITermVar, ITermVar> reps();

    protected abstract Map<ITermVar, ITerm> terms();

    ///////////////////////////////////////////
    // unifier functions
    ///////////////////////////////////////////

    @Override public boolean isEmpty() {
        return reps().isEmpty() && terms().isEmpty();
    }

    @Override public int size() {
        return reps().size() + terms().size();
    }

    @Override public boolean contains(ITermVar var) {
        return reps().containsKey(var) || terms().containsKey(var);
    }

    @Override public Set<ITermVar> varSet() {
        return Sets.union(reps().keySet(), terms().keySet());
    }

    @Override public Set<ITermVar> freeVarSet() {
        final Set<ITermVar> freeVars = Sets.newHashSet();
        reps().values().stream().filter(var -> !contains(var)).forEach(freeVars::add);
        terms().values().stream().flatMap(term -> term.getVars().elementSet().stream()).filter(var -> !contains(var))
                .forEach(freeVars::add);
        return freeVars;
    }

    @Override public boolean isCyclic() {
        return isCyclic(varSet());
    }

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for(ITermVar var : terms().keySet()) {
            sb.append(first ? " " : ", ");
            first = false;
            sb.append(var);
            sb.append(" |-> ");
            sb.append(terms().get(var));
        }
        for(ITermVar var : reps().keySet()) {
            sb.append(first ? " " : ", ");
            first = false;
            sb.append(var);
            sb.append(" |-> ");
            sb.append(findRep(var));
        }
        sb.append(first ? "}" : " }");
        return sb.toString();
    }

    ///////////////////////////////////////////
    // findTerm(ITerm) / findRep(ITerm)
    ///////////////////////////////////////////

    @Override public ITerm findTerm(ITerm term) {
        return term.match(Terms.<ITerm>cases().var(var -> {
            final ITermVar rep = findRep(var);
            return terms().getOrDefault(rep, rep);
        }).otherwise(t -> t));
    }

    protected static ITermVar findRep(ITermVar var, Map.Transient<ITermVar, ITermVar> reps) {
        ITermVar rep = reps.get(var);
        if(rep == null) {
            return var;
        } else {
            rep = findRep(rep, reps);
            reps.put(var, rep);
            return rep;
        }
    }

    ///////////////////////////////////////////
    // findRecursive(ITerm)
    ///////////////////////////////////////////

    @Override public ITerm findRecursive(final ITerm term) {
        return findTermRecursive(term, Sets.newHashSet(), Maps.newHashMap());
    }

    private ITerm findTermRecursive(final ITerm term, final Set<ITermVar> stack,
            final java.util.Map<ITermVar, ITerm> visited) {
        return term.match(Terms.cases(
        // @formatter:off
            appl -> B.newAppl(appl.getOp(), findRecursiveTerms(appl.getArgs(), stack, visited), appl.getAttachments()),
            list -> findListTermRecursive(list, stack, visited),
            string -> string,
            integer -> integer,
            blob -> blob,
            var -> findVarRecursive(var, stack, visited)
            // @formatter:on
        ));
    }

    private IListTerm findListTermRecursive(IListTerm list, final Set<ITermVar> stack,
            final java.util.Map<ITermVar, ITerm> visited) {
        Deque<IListTerm> elements = Lists.newLinkedList();
        while(list != null) {
            list = list.match(ListTerms.cases(
            // @formatter:off
                cons -> {
                    elements.push(cons);
                    return cons.getTail();
                },
                nil -> {
                    elements.push(nil);
                    return null;
                },
                var -> {
                    elements.push(var);
                    return null;
                }
                // @formatter:on
            ));
        }
        Ref<IListTerm> instance = new Ref<>();
        while(!elements.isEmpty()) {
            instance.set(elements.pop().match(ListTerms.<IListTerm>cases(
            // @formatter:off
                cons -> B.newCons(findTermRecursive(cons.getHead(), stack, visited), instance.get(), cons.getAttachments()),
                nil -> nil,
                var -> (IListTerm) findVarRecursive(var, stack, visited)
                // @formatter:on
            )));
        }
        return instance.get();
    }

    private ITerm findVarRecursive(final ITermVar var, final Set<ITermVar> stack,
            final java.util.Map<ITermVar, ITerm> visited) {
        final ITermVar rep = findRep(var);
        final ITerm instance;
        if(!visited.containsKey(rep)) {
            stack.add(rep);
            visited.put(rep, rep);
            final ITerm term = terms().get(rep);
            instance = term != null ? findTermRecursive(term, stack, visited) : rep;
            visited.put(rep, instance);
            stack.remove(rep);
            return instance;
        } else if(stack.contains(rep)) {
            throw new IllegalArgumentException("Recursive terms cannot be instantiated.");
        } else {
            instance = visited.get(rep);
        }
        return instance;
    }

    private Iterable<ITerm> findRecursiveTerms(final Iterable<ITerm> terms, final Set<ITermVar> stack,
            final java.util.Map<ITermVar, ITerm> visited) {
        List<ITerm> instances = Lists.newArrayList();
        for(ITerm term : terms) {
            instances.add(findTermRecursive(term, stack, visited));
        }
        return instances;
    }

    ///////////////////////////////////////////
    // areEqual(ITerm, ITerm)
    ///////////////////////////////////////////

    @Override public boolean areEqual(final ITerm left, final ITerm right) {
        return equalTerms(left, right, Sets.newHashSet(), Maps.newHashMap());
    }

    private boolean equalTerms(final ITerm left, final ITerm right, final Set<Set2<ITermVar>> stack,
            final java.util.Map<Set2<ITermVar>, Boolean> visited) {
        // @formatter:off
        return left.match(Terms.cases(
            applLeft -> right.match(Terms.<Boolean>cases()
                .appl(applRight -> applLeft.getOp().equals(applRight.getOp()) &&
                                   applLeft.getArity() == applRight.getArity() &&
                                   equals(applLeft.getArgs(), applRight.getArgs(), stack, visited))
                .var(varRight -> equalVarTerm(varRight, applLeft, stack, visited))
                .otherwise(t -> false)
            ),
            listLeft -> right.match(Terms.<Boolean>cases()
                .list(listRight -> listLeft.match(ListTerms.cases(
                    consLeft -> listRight.match(ListTerms.<Boolean>cases()
                        .cons(consRight -> {
                            return equalTerms(consLeft.getHead(), consRight.getHead(), stack, visited) && 
                            equalTerms(consLeft.getTail(), consRight.getTail(), stack, visited);
                        })
                        .var(varRight -> equalVarTerm(varRight, consLeft, stack, visited))
                        .otherwise(l -> false)
                    ),
                    nilLeft -> listRight.match(ListTerms.<Boolean>cases()
                        .nil(nilRight -> true)
                        .var(varRight -> equalVarTerm(varRight, nilLeft, stack, visited))
                        .otherwise(l -> false)
                    ),
                    varLeft -> listRight.match(ListTerms.<Boolean>cases()
                        .var(varRight -> equalVars(varLeft, varRight, stack, visited))
                        .otherwise(termRight -> equalVarTerm(varLeft, termRight, stack, visited))
                    )
                )))
                .var(varRight -> equalVarTerm(varRight, listLeft, stack, visited))
                .otherwise(t -> false)
            ),
            stringLeft -> right.match(Terms.<Boolean>cases()
                .string(stringRight -> stringLeft.getValue().equals(stringRight.getValue()))
                .var(varRight -> equalVarTerm(varRight, stringLeft, stack, visited))
                .otherwise(t -> false)
            ),
            integerLeft -> right.match(Terms.<Boolean>cases()
                .integer(integerRight -> integerLeft.getValue() == integerRight.getValue())
                .var(varRight -> equalVarTerm(varRight, integerLeft, stack, visited))
                .otherwise(t -> false)
            ),
            blobLeft -> right.match(Terms.<Boolean>cases()
                .blob(blobRight -> blobLeft.getValue().equals(blobRight.getValue()))
                .var(varRight -> equalVarTerm(varRight, blobLeft, stack, visited))
                .otherwise(t -> false)
            ),
            varLeft -> right.match(Terms.<Boolean>cases()
                // match var before term, or term will always match
                .var(varRight -> equalVars(varLeft, varRight, stack, visited))
                .otherwise(termRight -> equalVarTerm(varLeft, termRight, stack, visited))
            )
        ));
        // @formatter:on
    }

    private boolean equalVarTerm(final ITermVar var, final ITerm term, final Set<Set2<ITermVar>> stack,
            final java.util.Map<Set2<ITermVar>, Boolean> visited) {
        final ITermVar rep = findRep(var);
        if(terms().containsKey(rep)) {
            return equalTerms(terms().get(rep), term, stack, visited);
        }
        return false;
    }

    private boolean equalVars(final ITermVar left, final ITermVar right, final Set<Set2<ITermVar>> stack,
            final java.util.Map<Set2<ITermVar>, Boolean> visited) {
        final ITermVar leftRep = findRep(left);
        final ITermVar rightRep = findRep(right);
        if(leftRep.equals(rightRep)) {
            return true;
        }
        final Set2<ITermVar> pair = Set2.of(leftRep, rightRep);
        final boolean equal;
        if(!visited.containsKey(pair)) {
            stack.add(pair);
            visited.put(pair, false);
            final ITerm leftTerm = terms().get(leftRep);
            final ITerm rightTerm = terms().get(rightRep);
            equal = (leftTerm != null && rightTerm != null) ? equalTerms(leftTerm, rightTerm, stack, visited) : false;
            visited.put(pair, equal);
            stack.remove(pair);
        } else if(stack.contains(pair)) {
            equal = false;
        } else {
            equal = visited.get(pair);
        }
        return equal;
    }

    private boolean equals(final Iterable<ITerm> lefts, final Iterable<ITerm> rights, final Set<Set2<ITermVar>> stack,
            final java.util.Map<Set2<ITermVar>, Boolean> visited) {
        Iterator<ITerm> itLeft = lefts.iterator();
        Iterator<ITerm> itRight = rights.iterator();
        while(itLeft.hasNext()) {
            if(!itRight.hasNext()) {
                return false;
            }
            if(!equalTerms(itLeft.next(), itRight.next(), stack, visited)) {
                return false;
            }
        }
        if(itRight.hasNext()) {
            return false;
        }
        return true;
    }

    ///////////////////////////////////////////
    // areUnequal(ITerm, ITerm)
    ///////////////////////////////////////////

    @Override public boolean areUnequal(final ITerm left, final ITerm right) {
        return unequalTerms(left, right, Sets.newHashSet(), Maps.newHashMap());
    }

    private boolean unequalTerms(final ITerm left, final ITerm right, Set<Set2<ITermVar>> stack,
            final java.util.Map<Set2<ITermVar>, Boolean> visited) {
        // @formatter:off
        return left.match(Terms.cases(
            applLeft -> right.match(Terms.<Boolean>cases()
                .appl(applRight -> !applLeft.getOp().equals(applRight.getOp()) ||
                                    applLeft.getArity() != applRight.getArity() ||
                                    unequals(applLeft.getArgs(), applRight.getArgs(), stack, visited))
                .var(varRight -> unequalVarTerm(varRight, applLeft, stack, visited))
                .otherwise(t -> true)
            ),
            listLeft -> right.match(Terms.<Boolean>cases()
                .list(listRight -> listLeft.match(ListTerms.cases(
                    consLeft -> listRight.match(ListTerms.<Boolean>cases()
                        .cons(consRight -> {
                            return unequalTerms(consLeft.getHead(), consRight.getHead(), stack, visited) || 
                                   unequalTerms(consLeft.getTail(), consRight.getTail(), stack, visited);
                        })
                        .var(varRight -> unequalVarTerm(varRight, consLeft, stack, visited))
                        .otherwise(t -> true)
                    ),
                    nilLeft -> listRight.match(ListTerms.<Boolean>cases()
                        .nil(nilRight -> false)
                        .var(varRight -> unequalVarTerm(varRight, nilLeft, stack, visited))
                        .otherwise(t -> true)
                    ),
                    varLeft -> listRight.match(ListTerms.<Boolean>cases()
                        .var(varRight -> unequalVars(varLeft, varRight, stack, visited))
                        .otherwise(termRight -> unequalVarTerm(varLeft, termRight, stack, visited))
                    )
                )))
                .var(varRight -> unequalVarTerm(varRight, listLeft, stack, visited))
                .otherwise(t -> true)
            ),
            stringLeft -> right.match(Terms.<Boolean>cases()
                .string(stringRight -> !stringLeft.getValue().equals(stringRight.getValue()))
                .var(varRight -> unequalVarTerm(varRight, stringLeft, stack, visited))
                .otherwise(t -> true)
            ),
            integerLeft -> right.match(Terms.<Boolean>cases()
                .integer(integerRight -> integerLeft.getValue() != integerRight.getValue())
                .var(varRight -> unequalVarTerm(varRight, integerLeft, stack, visited))
                .otherwise(t -> true)
            ),
            blobLeft -> right.match(Terms.<Boolean>cases()
                .blob(blobRight -> !blobLeft.getValue().equals(blobRight.getValue()))
                .var(varRight -> unequalVarTerm(varRight, blobLeft, stack, visited))
                .otherwise(t -> true)
            ),
            varLeft -> right.match(Terms.<Boolean>cases()
                // match var before term, or term will always match
                .var(varRight -> unequalVars(varLeft, varRight, stack, visited))
                .otherwise(termRight -> unequalVarTerm(varLeft, termRight, stack, visited))
            )
        ));
        // @formatter:on
    }

    private boolean unequalVarTerm(final ITermVar var, final ITerm term, Set<Set2<ITermVar>> stack,
            final java.util.Map<Set2<ITermVar>, Boolean> visited) {
        final ITermVar rep = findRep(var);
        if(terms().containsKey(rep)) {
            return unequalTerms(terms().get(rep), term, stack, visited);
        }
        return false;
    }

    private boolean unequalVars(final ITermVar left, final ITermVar right, Set<Set2<ITermVar>> stack,
            final java.util.Map<Set2<ITermVar>, Boolean> visited) {
        final ITermVar leftRep = findRep(left);
        final ITermVar rightRep = findRep(right);
        if(leftRep.equals(rightRep)) {
            return false;
        }
        final Set2<ITermVar> pair = Set2.of(leftRep, rightRep);
        final boolean unequal;
        if(!visited.containsKey(pair)) {
            stack.add(pair);
            visited.put(pair, false);
            final ITerm leftTerm = terms().get(leftRep);
            final ITerm rightTerm = terms().get(rightRep);
            unequal =
                    (leftTerm != null && rightTerm != null) ? unequalTerms(leftTerm, rightTerm, stack, visited) : false;
            visited.put(pair, unequal);
            stack.remove(pair);
        } else if(stack.contains(pair)) {
            unequal = false;
        } else {
            unequal = visited.get(pair);
        }
        return unequal;
    }

    private boolean unequals(final Iterable<ITerm> lefts, final Iterable<ITerm> rights, Set<Set2<ITermVar>> stack,
            final java.util.Map<Set2<ITermVar>, Boolean> visited) {
        Iterator<ITerm> itLeft = lefts.iterator();
        Iterator<ITerm> itRight = rights.iterator();
        while(itLeft.hasNext()) {
            if(!itRight.hasNext()) {
                return true;
            }
            if(unequalTerms(itLeft.next(), itRight.next(), stack, visited)) {
                return true;
            }
        }
        if(itRight.hasNext()) {
            return true;
        }
        return false;
    }

    ///////////////////////////////////////////
    // isCyclic(ITerm)
    ///////////////////////////////////////////

    @Override public boolean isCyclic(final ITerm term) {
        return isCyclic(term.getVars().elementSet(), Sets.newHashSet(), Maps.newHashMap());
    }

    protected boolean isCyclic(final Set<ITermVar> vars) {
        return isCyclic(vars, Sets.newHashSet(), Maps.newHashMap());
    }

    private boolean isCyclic(final Set<ITermVar> vars, final Set<ITermVar> stack,
            final java.util.Map<ITermVar, Boolean> visited) {
        return vars.stream().anyMatch(var -> isCyclic(var, stack, visited));
    }

    private boolean isCyclic(final ITermVar var, final Set<ITermVar> stack,
            final java.util.Map<ITermVar, Boolean> visited) {
        final boolean cyclic;
        final ITermVar rep = findRep(var);
        if(!visited.containsKey(rep)) {
            stack.add(rep);
            visited.put(rep, false);
            final ITerm term = terms().get(rep);
            cyclic = term != null ? isCyclic(term.getVars().elementSet(), stack, visited) : false;
            visited.put(rep, cyclic);
            stack.remove(rep);
        } else if(stack.contains(rep)) {
            cyclic = true;
        } else {
            cyclic = visited.get(rep);
        }
        return cyclic;
    }

    ///////////////////////////////////////////
    // isGround(ITerm)
    ///////////////////////////////////////////

    @Override public boolean isGround(final ITerm term) {
        return isGround(term.getVars().elementSet(), Sets.newHashSet(), Maps.newHashMap());
    }

    private boolean isGround(final Set<ITermVar> vars, final Set<ITermVar> stack,
            final java.util.Map<ITermVar, Boolean> visited) {
        return vars.stream().anyMatch(var -> isGround(var, stack, visited));
    }

    private boolean isGround(final ITermVar var, final Set<ITermVar> stack,
            final java.util.Map<ITermVar, Boolean> visited) {
        final boolean ground;
        final ITermVar rep = findRep(var);
        if(!visited.containsKey(rep)) {
            stack.add(rep);
            visited.put(rep, false);
            final ITerm term = terms().get(rep);
            ground = term != null ? isGround(term.getVars().elementSet(), stack, visited) : false;
            visited.put(rep, ground);
            stack.remove(rep);
        } else if(stack.contains(rep)) {
            ground = false;
        } else {
            ground = visited.get(rep);
        }
        return ground;
    }

    ///////////////////////////////////////////
    // getVars(ITerm)
    ///////////////////////////////////////////

    @Override public Set<ITermVar> getVars(final ITerm term) {
        final Set<ITermVar> vars = Sets.newHashSet();
        getVars(term.getVars().elementSet(), Lists.newLinkedList(), Sets.newHashSet(), vars);
        return vars;
    }

    private void getVars(final Set<ITermVar> tryVars, final LinkedList<ITermVar> stack, final Set<ITermVar> visited,
            Set<ITermVar> vars) {
        tryVars.stream().forEach(var -> getVars(var, stack, visited, vars));
    }

    private void getVars(final ITermVar var, final LinkedList<ITermVar> stack, final Set<ITermVar> visited,
            Set<ITermVar> vars) {
        final ITermVar rep = findRep(var);
        if(!visited.contains(rep)) {
            visited.add(rep);
            stack.push(rep);
            final ITerm term = terms().get(rep);
            if(term != null) {
                getVars(term.getVars().elementSet(), stack, visited, vars);
            } else {
                vars.add(rep);
            }
            stack.pop();
        } else {
            final int index = stack.indexOf(rep); // linear
            if(index >= 0) {
                vars.addAll(stack.subList(0, index + 1));
            }
        }
    }

    ///////////////////////////////////////////
    // size(ITerm)
    ///////////////////////////////////////////

    @Override public TermSize size(final ITerm term) {
        return size(term, Sets.newHashSet(), Maps.newHashMap());
    }

    private TermSize size(final ITerm term, final Set<ITermVar> stack,
            final java.util.Map<ITermVar, TermSize> visited) {
        return term.match(Terms.cases(
        // @formatter:off
            appl -> TermSize.ONE.add(sizes(appl.getArgs(), stack, visited)),
            list -> size(list, stack, visited),
            string -> TermSize.ONE,
            integer -> TermSize.ONE,
            blob -> TermSize.ONE,
            var -> size(var, stack, visited)
            // @formatter:on
        ));
    }

    private TermSize size(IListTerm list, final Set<ITermVar> stack, final java.util.Map<ITermVar, TermSize> visited) {
        final Ref<TermSize> size = new Ref<>(TermSize.ZERO);
        while(list != null) {
            list = list.match(ListTerms.cases(
            // @formatter:off
                cons -> {
                    size.set(size.get().add(TermSize.ONE).add(size(cons.getHead(), stack, visited)));
                    return cons.getTail();
                },
                nil -> {
                    size.set(size.get().add(TermSize.ONE));
                    return null;
                },
                var -> {
                    size.set(size.get().add(size(var, stack, visited)));
                    return null;
                }
                // @formatter:on
            ));
        }
        return size.get();
    }

    private TermSize size(final ITermVar var, final Set<ITermVar> stack,
            final java.util.Map<ITermVar, TermSize> visited) {
        final ITermVar rep = findRep(var);
        final TermSize size;
        if(!visited.containsKey(rep)) {
            stack.add(rep);
            visited.put(rep, TermSize.ZERO);
            final ITerm term = terms().get(rep);
            size = term != null ? size(term, stack, visited) : TermSize.ZERO;
            visited.put(rep, size);
            stack.remove(rep);
            return size;
        } else if(stack.contains(rep)) {
            size = TermSize.INF;
        } else {
            size = visited.get(rep);
        }
        return size;
    }

    private TermSize sizes(final Iterable<ITerm> terms, final Set<ITermVar> stack,
            final java.util.Map<ITermVar, TermSize> visited) {
        TermSize size = TermSize.ZERO;
        for(ITerm term : terms) {
            size = size.add(size(term, stack, visited));
        }
        return size;
    }

    ///////////////////////////////////////////
    // class Immutable
    ///////////////////////////////////////////

    public static class Immutable extends PersistentUnifier implements IUnifier.Immutable, Serializable {

        private static final long serialVersionUID = 42L;

        private final Ref<Map.Immutable<ITermVar, ITermVar>> reps;
        private final Map.Immutable<ITermVar, Integer> ranks;
        private final Map.Immutable<ITermVar, ITerm> terms;

        private Immutable(final Map.Immutable<ITermVar, ITermVar> reps, final Map.Immutable<ITermVar, Integer> ranks,
                final Map.Immutable<ITermVar, ITerm> terms) {
            this.reps = new Ref<>(reps);
            this.ranks = ranks;
            this.terms = terms;
        }

        @Override protected Map<ITermVar, ITermVar> reps() {
            return reps.get();
        }

        @Override protected Map<ITermVar, ITerm> terms() {
            return terms;
        }

        @Override public IUnifier.Immutable.Result<IUnifier.Immutable> unify(ITerm left, ITerm right)
                throws UnificationException {
            final IUnifier.Transient unifier = melt();
            final IUnifier.Immutable diff = unifier.unify(left, right);
            return new PersistentUnifier.Result<>(diff, unifier.freeze());
        }

        @Override public IUnifier.Immutable.Result<IUnifier.Immutable> unify(IUnifier other)
                throws UnificationException {
            final IUnifier.Transient unifier = melt();
            final IUnifier.Immutable diff = unifier.unify(other);
            return new PersistentUnifier.Result<>(diff, unifier.freeze());
        }

        @Override public IUnifier.Immutable compose(IUnifier other) {
            final IUnifier.Transient unifier = melt();
            unifier.compose(other);
            return unifier.freeze();
        }

        @Override public IUnifier.Immutable.Result<IUnifier.Immutable> match(ITerm pattern, ITerm term)
                throws MatchException {
            final IUnifier.Transient unifier = melt();
            final IUnifier.Immutable diff = unifier.match(pattern, term);
            return new PersistentUnifier.Result<>(diff, unifier.freeze());
        }

        @Override public ITermVar findRep(ITermVar var) {
            final Map.Transient<ITermVar, ITermVar> reps = this.reps.get().asTransient();
            final ITermVar rep = findRep(var, reps);
            this.reps.set(reps.freeze());
            return rep;
        }

        @Override public IUnifier.Immutable.Result<IUnifier.Immutable> retain(ITermVar var) {
            final IUnifier.Transient unifier = melt();
            IUnifier.Immutable result = unifier.retain(var);
            return new PersistentUnifier.Result<>(result, unifier.freeze());
        }

        @Override public IUnifier.Immutable.Result<IUnifier.Immutable> retainAll(Iterable<ITermVar> vars) {
            final IUnifier.Transient unifier = melt();
            IUnifier.Immutable result = unifier.retainAll(vars);
            return new PersistentUnifier.Result<>(result, unifier.freeze());
        }

        @Override public IUnifier.Immutable.Result<IUnifier.Immutable> remove(ITermVar var) {
            final IUnifier.Transient unifier = melt();
            IUnifier.Immutable result = unifier.remove(var);
            return new PersistentUnifier.Result<>(result, unifier.freeze());
        }

        @Override public IUnifier.Immutable.Result<IUnifier.Immutable> removeAll(Iterable<ITermVar> vars) {
            final IUnifier.Transient unifier = melt();
            IUnifier.Immutable result = unifier.removeAll(vars);
            return new PersistentUnifier.Result<>(result, unifier.freeze());
        }

        @Override public IUnifier.Transient melt() {
            return new PersistentUnifier.Transient(reps.get().asTransient(), ranks.asTransient(), terms.asTransient());
        }

        public static IUnifier.Immutable of() {
            return new PersistentUnifier.Immutable(Map.Immutable.of(), Map.Immutable.of(), Map.Immutable.of());
        }

    }

    ///////////////////////////////////////////
    // class Transient
    ///////////////////////////////////////////

    public static class Transient extends PersistentUnifier implements IUnifier.Transient, Serializable {

        private static final long serialVersionUID = 42L;

        private final Map.Transient<ITermVar, ITermVar> reps;
        private final Map.Transient<ITermVar, Integer> ranks;
        private final Map.Transient<ITermVar, ITerm> terms;

        private Transient(final Map.Transient<ITermVar, ITermVar> reps, final Map.Transient<ITermVar, Integer> ranks,
                final Map.Transient<ITermVar, ITerm> terms) {
            this.reps = reps;
            this.ranks = ranks;
            this.terms = terms;
        }

        @Override protected Map<ITermVar, ITermVar> reps() {
            return reps;
        }

        @Override protected Map<ITermVar, ITerm> terms() {
            return terms;
        }

        @Override public ITermVar findRep(ITermVar var) {
            return findRep(var, reps);
        }

        @Override public PersistentUnifier.Immutable freeze() {
            return new PersistentUnifier.Immutable(reps.freeze(), ranks.freeze(), terms.freeze());
        }

        public static IUnifier.Transient of() {
            return new PersistentUnifier.Transient(Map.Transient.of(), Map.Transient.of(), Map.Transient.of());
        }

        ///////////////////////////////////////////
        // unify(ITerm, ITerm)
        ///////////////////////////////////////////

        @Override public IUnifier.Immutable unify(final ITerm left, final ITerm right) throws UnificationException {
            final Set<ITermVar> result = Sets.newHashSet();
            final Deque<Tuple2<ITerm, ITerm>> worklist = Lists.newLinkedList();
            worklist.push(ImmutableTuple2.of(left, right));
            while(!worklist.isEmpty()) {
                final Tuple2<ITerm, ITerm> work = worklist.pop();
                if(!unifyTerms(work._1(), work._2(), worklist, result)) {
                    throw new UnificationException(left, right);
                }
            }
            if(isCyclic(result)) {
                throw new UnificationException(left, right);
            }
            return diffUnifier(result);
        }

        @Override public IUnifier.Immutable unify(IUnifier other) throws UnificationException {
            final Set<ITermVar> result = Sets.newHashSet();
            final Deque<Tuple2<ITerm, ITerm>> worklist = Lists.newLinkedList();
            for(ITermVar var : other.varSet()) {
                final ITermVar rep = other.findRep(var);
                if(!var.equals(rep)) {
                    worklist.push(ImmutableTuple2.of(var, rep));
                } else {
                    final ITerm term = other.findTerm(var);
                    if(!var.equals(term)) {
                        worklist.push(ImmutableTuple2.of(var, term));
                    }
                }
            }
            while(!worklist.isEmpty()) {
                final Tuple2<ITerm, ITerm> work = worklist.pop();
                if(!unifyTerms(work._1(), work._2(), worklist, result)) {
                    throw new UnificationException(work._1(), work._2());
                }
            }
            if(isCyclic(result)) {
                throw new UnificationException(B.newTuple(), B.newTuple()); // FIXME
            }
            return diffUnifier(result);
        }

        private boolean unifyTerms(final ITerm left, final ITerm right, final Deque<Tuple2<ITerm, ITerm>> worklist,
                Set<ITermVar> result) {
            // @formatter:off
            return left.match(Terms.cases(
                applLeft -> right.match(Terms.<Boolean>cases()
                    .appl(applRight -> applLeft.getOp().equals(applRight.getOp()) &&
                                        applLeft.getArity() == applRight.getArity() &&
                                        unifys(applLeft.getArgs(), applRight.getArgs(), worklist))
                    .var(varRight -> unifyTerms(varRight, applLeft, worklist, result))
                    .otherwise(t -> false)
                ),
                listLeft -> right.match(Terms.<Boolean>cases()
                    .list(listRight -> unifyLists(listLeft, listRight, worklist, result))
                    .var(varRight -> unifyTerms(varRight, listLeft, worklist, result))
                    .otherwise(t -> false)
                ),
                stringLeft -> right.match(Terms.<Boolean>cases()
                    .string(stringRight -> stringLeft.getValue().equals(stringRight.getValue()))
                    .var(varRight -> unifyTerms(varRight, stringLeft, worklist, result))
                    .otherwise(t -> false)
                ),
                integerLeft -> right.match(Terms.<Boolean>cases()
                    .integer(integerRight -> integerLeft.getValue() == integerRight.getValue())
                    .var(varRight -> unifyTerms(varRight, integerLeft, worklist, result))
                    .otherwise(t -> false)
                ),
                blobLeft -> right.match(Terms.<Boolean>cases()
                    .blob(blobRight -> blobLeft.getValue().equals(blobRight.getValue()))
                    .var(varRight -> unifyTerms(varRight, blobLeft, worklist, result))
                    .otherwise(t -> false)
                ),
                varLeft -> right.match(Terms.<Boolean>cases()
                    // match var before term, or term will always match
                    .var(varRight -> unifyVars(varLeft, varRight, worklist, result))
                    .otherwise(termRight -> unifyVarTerm(varLeft, termRight, worklist, result))
                )
            ));
            // @formatter:on
        }

        private boolean unifyLists(final IListTerm left, final IListTerm right,
                final Deque<Tuple2<ITerm, ITerm>> worklist, Set<ITermVar> result) {
            return left.match(ListTerms.cases(
            // @formatter:off
                consLeft -> right.match(ListTerms.<Boolean>cases()
                    .cons(consRight -> {
                        worklist.push(ImmutableTuple2.of(consLeft.getHead(), consRight.getHead()));
                        worklist.push(ImmutableTuple2.of(consLeft.getTail(), consRight.getTail()));
                        return true;
                    })
                    .var(varRight -> unifyLists(varRight, consLeft, worklist, result))
                    .otherwise(l -> false)
                ),
                nilLeft -> right.match(ListTerms.<Boolean>cases()
                    .nil(nilRight -> true)
                    .var(varRight -> unifyVarTerm(varRight, nilLeft, worklist, result))
                    .otherwise(l -> false)
                ),
                varLeft -> right.match(ListTerms.<Boolean>cases()
                    .var(varRight -> unifyVars(varLeft, varRight, worklist, result))
                    .otherwise(termRight -> unifyVarTerm(varLeft, termRight, worklist, result))
                )
                // @formatter:on
            ));
        }

        private boolean unifyVarTerm(final ITermVar var, final ITerm term, final Deque<Tuple2<ITerm, ITerm>> worklist,
                Set<ITermVar> result) {
            final ITermVar rep = findRep(var);
            if(terms.containsKey(rep)) {
                worklist.push(ImmutableTuple2.of(terms.get(rep), term));
            } else {
                terms.put(rep, term);
                result.add(rep);
            }
            return true;
        }

        private boolean unifyVars(final ITermVar left, final ITermVar right, final Deque<Tuple2<ITerm, ITerm>> worklist,
                Set<ITermVar> result) {
            final ITermVar leftRep = findRep(left);
            final ITermVar rightRep = findRep(right);
            if(leftRep.equals(rightRep)) {
                return true;
            }
            final int leftRank = ranks.getOrDefault(leftRep, 1);
            final int rightRank = ranks.getOrDefault(rightRep, 1);
            final boolean swap = leftRank > rightRank;
            final ITermVar var = swap ? rightRep : leftRep; // the eliminated variable
            final ITermVar with = swap ? leftRep : rightRep; // the new representative
            ranks.put(with, leftRank + rightRank);
            reps.put(var, with);
            result.add(var);
            final ITerm term = terms.__remove(var); // term for the eliminated var
            if(term != null) {
                worklist.push(ImmutableTuple2.of(term, terms.getOrDefault(with, with)));
            }
            return true;
        }

        private boolean unifys(final Iterable<ITerm> lefts, final Iterable<ITerm> rights,
                final Deque<Tuple2<ITerm, ITerm>> worklist) {
            Iterator<ITerm> itLeft = lefts.iterator();
            Iterator<ITerm> itRight = rights.iterator();
            while(itLeft.hasNext()) {
                if(!itRight.hasNext()) {
                    return false;
                }
                worklist.push(ImmutableTuple2.of(itLeft.next(), itRight.next()));
            }
            if(itRight.hasNext()) {
                return false;
            }
            return true;
        }

        ///////////////////////////////////////////
        // match(ITerm, ITerm)
        ///////////////////////////////////////////

        @Override public IUnifier.Immutable match(final ITerm pattern, final ITerm term) throws MatchException {
            final Set<ITermVar> result = Sets.newHashSet();
            final Deque<Tuple2<ITerm, ITerm>> worklist = Lists.newLinkedList();
            worklist.push(ImmutableTuple2.of(pattern, term));
            while(!worklist.isEmpty()) {
                final Tuple2<ITerm, ITerm> work = worklist.pop();
                if(!matchTerms(work._1(), work._2(), worklist, result)) {
                    throw new MatchException(pattern, term);
                }
            }
            if(isCyclic(result)) {
                throw new MatchException(pattern, term);
            }
            return diffUnifier(result);
        }

        private boolean matchTerms(final ITerm pattern, final ITerm term, final Deque<Tuple2<ITerm, ITerm>> worklist,
                Set<ITermVar> result) {
            // @formatter:off
            return pattern.match(Terms.cases(
                applPattern -> term.match(Terms.<Boolean>cases()
                    .appl(applTerm -> applPattern.getOp().equals(applTerm.getOp()) &&
                                      applPattern.getArity() == applTerm.getArity() &&
                                      matchs(applPattern.getArgs(), applTerm.getArgs(), worklist))
                    .otherwise(t -> false)
                ),
                listPattern -> term.match(Terms.<Boolean>cases()
                    .list(listTerm -> matchLists(listPattern, listTerm, worklist, result))
                    .otherwise(t -> false)
                ),
                stringPattern -> term.match(Terms.<Boolean>cases()
                    .string(stringTerm -> stringPattern.getValue().equals(stringTerm.getValue()))
                    .otherwise(t -> false)
                ),
                integerPattern -> term.match(Terms.<Boolean>cases()
                    .integer(integerTerm -> integerPattern.getValue() == integerTerm.getValue())
                    .otherwise(t -> false)
                ),
                blobPattern -> term.match(Terms.<Boolean>cases()
                    .blob(blobTerm -> blobPattern.getValue().equals(blobTerm.getValue()))
                    .otherwise(t -> false)
                ),
                varPattern -> matchVar(varPattern, term, worklist, result)
            ));
            // @formatter:on
        }

        private boolean matchLists(final IListTerm pattern, final IListTerm term,
                final Deque<Tuple2<ITerm, ITerm>> worklist, Set<ITermVar> result) {
            return pattern.match(ListTerms.cases(
            // @formatter:off
                consPattern -> term.match(ListTerms.<Boolean>cases()
                    .cons(consTerm -> {
                        worklist.push(ImmutableTuple2.of(consPattern.getHead(), consTerm.getHead()));
                        worklist.push(ImmutableTuple2.of(consPattern.getTail(), consTerm.getTail()));
                        return true;
                    })
                    .otherwise(l -> false)
                ),
                nilPattern -> term.match(ListTerms.<Boolean>cases()
                    .nil(nilTerm -> true)
                    .otherwise(l -> false)
                ),
                varPattern -> matchVar(varPattern, term, worklist, result)
                // @formatter:on
            ));
        }

        private boolean matchVar(final ITermVar var, final ITerm term, final Deque<Tuple2<ITerm, ITerm>> worklist,
                Set<ITermVar> result) {
            final ITermVar rep = findRep(var);
            final ITerm pattern = terms.get(rep);
            if(pattern != null) {
                worklist.push(ImmutableTuple2.of(pattern, term));
            } else {
                terms.put(rep, term);
                result.add(rep);
            }
            return true;
        }

        private boolean matchs(final Iterable<ITerm> lefts, final Iterable<ITerm> rights,
                final Deque<Tuple2<ITerm, ITerm>> worklist) {
            Iterator<ITerm> itLeft = lefts.iterator();
            Iterator<ITerm> itRight = rights.iterator();
            while(itLeft.hasNext()) {
                if(!itRight.hasNext()) {
                    return false;
                }
                worklist.push(ImmutableTuple2.of(itLeft.next(), itRight.next()));
            }
            if(itRight.hasNext()) {
                return false;
            }
            return true;
        }

        ///////////////////////////////////////////
        // compose(IUnifier)
        ///////////////////////////////////////////

        @Override public void compose(IUnifier other) {
            for(ITermVar var : other.varSet()) {
                remove(var);
                final ITermVar rep = other.findRep(var);
                if(!rep.equals(var)) {
                    reps.put(var, rep);
                }
                final ITerm term = other.findTerm(var);
                if(!(term.equals(var) || term.equals(rep))) {
                    terms.put(var, term);
                }
            }
        }

        ///////////////////////////////////////////
        // retain(ITermVar)
        ///////////////////////////////////////////

        public IUnifier.Immutable retain(ITermVar var) {
            return retainAll(Collections.singleton(var));
        }

        @Override public IUnifier.Immutable retainAll(Iterable<ITermVar> vars) {
            return removeAll(Sets.difference(varSet(), ImmutableSet.copyOf(vars)));
        }

        ///////////////////////////////////////////
        // remove(ITermVar)
        ///////////////////////////////////////////

        @Override public IUnifier.Immutable removeAll(Iterable<ITermVar> vars) {
            final IUnifier.Transient diff = PersistentUnifier.Transient.of();
            for(ITermVar var : vars) {
                diff.compose(remove(var));
            }
            return diff.freeze();
        }

        @Override public IUnifier.Immutable remove(ITermVar var) {
            final IUnifier.Immutable diff;
            if(reps.containsKey(var)) {
                final ITermVar rep = reps.remove(var);
                diff = new PersistentUnifier.Immutable(Map.Immutable.of(var, rep), Map.Immutable.of(),
                        Map.Immutable.of());
                reps.replaceAll((v, r) -> v.equals(var) ? rep : r);
                terms.replaceAll((v, t) -> diff.findRecursive(t));
            } else if(terms.containsKey(var)) {
                final ITerm term = terms.remove(var);
                diff = new PersistentUnifier.Immutable(Map.Immutable.of(), Map.Immutable.of(),
                        Map.Immutable.of(var, term));
                diff.varSet().forEach(v -> {
                    reps.remove(v);
                    terms.put(v, term);
                });
                terms.replaceAll((v, t) -> diff.findRecursive(t));
            } else {
                diff = PersistentUnifier.Immutable.of();
            }
            return diff;
        }

        ///////////////////////////////////////////
        // diffUnifier(Set<ITermVar>)
        ///////////////////////////////////////////

        private IUnifier.Immutable diffUnifier(Set<ITermVar> vars) {
            final Map.Transient<ITermVar, ITermVar> diffReps = Map.Transient.of();
            final Map.Transient<ITermVar, ITerm> diffTerms = Map.Transient.of();
            for(ITermVar var : vars) {
                if(reps.containsKey(var)) {
                    diffReps.put(var, reps.get(var));
                } else if(terms.containsKey(var)) {
                    diffTerms.put(var, terms.get(var));
                }
            }
            return new PersistentUnifier.Immutable(diffReps.freeze(), Map.Immutable.of(), diffTerms.freeze());
        }

    }

    ///////////////////////////////////////////
    // class Result
    ///////////////////////////////////////////

    private static class Result<T> implements IUnifier.Immutable.Result<T> {

        private final T result;
        private final IUnifier.Immutable unifier;

        private Result(T result, IUnifier.Immutable unifier) {
            this.result = result;
            this.unifier = unifier;
        }

        @Override public T result() {
            return result;
        }

        public IUnifier.Immutable unifier() {
            return unifier;
        }

    }

}
