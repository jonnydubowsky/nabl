package org.metaborg.meta.nabl2.scopegraph.esop.persistent;

import static org.metaborg.meta.nabl2.util.tuples.HasLabel.labelEquals;
import static org.metaborg.meta.nabl2.util.tuples.ScopeLabelOccurrence.occurrenceEquals;
import static org.metaborg.meta.nabl2.util.tuples.ScopeLabelOccurrence.scopeEquals;
import static org.metaborg.meta.nabl2.util.tuples.ScopeLabelScope.sourceScopeEquals;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.metaborg.meta.nabl2.regexp.IRegExpMatcher;
import org.metaborg.meta.nabl2.regexp.RegExpMatcher;
import org.metaborg.meta.nabl2.relations.IRelation;
import org.metaborg.meta.nabl2.relations.RelationDescription;
import org.metaborg.meta.nabl2.relations.terms.Relation;
import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IResolutionParameters;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.OpenCounter;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopNameResolution;
import org.metaborg.meta.nabl2.scopegraph.path.IDeclPath;
import org.metaborg.meta.nabl2.scopegraph.path.IPath;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
import org.metaborg.meta.nabl2.scopegraph.path.IScopePath;
import org.metaborg.meta.nabl2.scopegraph.terms.path.Paths;
import org.metaborg.meta.nabl2.util.functions.Function0;
import org.metaborg.meta.nabl2.util.functions.PartialFunction0;
import org.metaborg.meta.nabl2.util.tuples.ImmutableTuple2;
import org.metaborg.meta.nabl2.util.tuples.Tuple2;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.Set.Immutable;
import io.usethesource.capsule.util.stream.CapsuleCollectors;

public class PersistentNameResolution<S extends IScope, L extends ILabel, O extends IOccurrence>
        implements IEsopNameResolution<S, L, O>, Serializable {

    private static final long serialVersionUID = 42L;

    private final PersistentScopeGraph<S, L, O> scopeGraph;

    private final Set.Immutable<L> labels;
    private final L labelD;
    private final IRegExpMatcher<L> wf;

    private final IRelation<L> ordered;
    private final IRelation<L> unordered;

    private final OpenCounter<S, L> scopeCounter;

    transient private Map<O, IPersistentEnvironment<S, L, O, IResolutionPath<S, L, O>>> resolveCache;

    transient private Map<S, IPersistentEnvironment<S, L, O, IDeclPath<S, L, O>>> visibleCache;
    transient private Map<S, IPersistentEnvironment<S, L, O, IDeclPath<S, L, O>>> reachableCache;

    transient private Map<IRelation<L>, EnvironmentL<S, L, O>> stagedEnv_L;

    public PersistentNameResolution(PersistentScopeGraph<S, L, O> scopeGraph, IResolutionParameters<L> params,
            OpenCounter<S, L> scopeCounter) {
        this.scopeGraph = scopeGraph;

        this.labels = Set.Immutable.<L>of().__insertAll(Sets.newHashSet(params.getLabels()));
        this.labelD = params.getLabelD();
        this.wf = RegExpMatcher.create(params.getPathWf());
        this.ordered = params.getSpecificityOrder();
        assert ordered.getDescription().equals(
                RelationDescription.STRICT_PARTIAL_ORDER) : "Label specificity order must be a strict partial order";
        this.unordered = new Relation<>(RelationDescription.STRICT_PARTIAL_ORDER);
        this.scopeCounter = scopeCounter;

        initTransients();
    }

    private void initTransients() {
        this.resolveCache = Maps.newHashMap();
        this.visibleCache = Maps.newHashMap();
        this.reachableCache = Maps.newHashMap();
        this.stagedEnv_L = Maps.newHashMap();
    }

    // NOTE: never used in project
    @Deprecated
    @Override
    public Set.Immutable<S> getAllScopes() {
        return scopeGraph.getAllScopes();
    }

    // NOTE: all references could be duplicated to get rid of scope graph reference
    @Override
    public Set.Immutable<O> getAllRefs() {
        return scopeGraph.getAllRefs();
    }

    @Override
    public Set.Immutable<IResolutionPath<S, L, O>> resolve(O ref) {
        return tryResolve(ref).map(Tuple2::_1).orElse(Set.Immutable.of());
    }

    @Override
    public Set.Immutable<IDeclPath<S, L, O>> visible(S scope) {
        return tryVisible(scope).map(Tuple2::_1).orElse(Set.Immutable.of());
    }

    @Override
    public Set.Immutable<IDeclPath<S, L, O>> reachable(S scope) {
        return tryReachable(scope).map(Tuple2::_1).orElse(Set.Immutable.of());
    }

    public Optional<Tuple2<Immutable<IResolutionPath<S, L, O>>, Immutable<String>>> tryResolve(O ref) {
        final IPersistentEnvironment<S, L, O, IResolutionPath<S, L, O>> env = resolveCache.computeIfAbsent(ref,
                r -> resolveEnv(Set.Immutable.of(), ref));
        return env.solution().map(ps -> ImmutableTuple2.of(ps, Set.Immutable.of()));
    }

    public Optional<Tuple2<Immutable<IDeclPath<S, L, O>>, Immutable<String>>> tryVisible(S scope) {
        final IPersistentEnvironment<S, L, O, IDeclPath<S, L, O>> env = visibleCache.computeIfAbsent(scope,
                s -> visibleEnv(scope));
        return env.solution().map(ps -> ImmutableTuple2.of(ps, Set.Immutable.of()));
    }

    public Optional<Tuple2<Immutable<IDeclPath<S, L, O>>, Immutable<String>>> tryReachable(S scope) {
        final IPersistentEnvironment<S, L, O, IDeclPath<S, L, O>> env = reachableCache.computeIfAbsent(scope,
                s -> reachableEnv(scope));
        return env.solution().map(ps -> ImmutableTuple2.of(ps, Set.Immutable.of()));
    }

    private IPersistentEnvironment<S, L, O, IDeclPath<S, L, O>> visibleEnv(S scope) {
        return env(Set.Immutable.of(), ordered, wf, Paths.empty(scope), Environments.envFilter());
    }

    private IPersistentEnvironment<S, L, O, IDeclPath<S, L, O>> reachableEnv(S scope) {
        return env(Set.Immutable.of(), unordered, wf, Paths.empty(scope), Environments.envFilter());
    }

    private IPersistentEnvironment<S, L, O, IResolutionPath<S, L, O>> resolveEnv(Set.Immutable<O> seenI, O reference) {
        // TODO: use hash lookup on occurrence instead of filter

        final Set.Immutable<O> nextSeenI = seenI.__insert(reference);
        final IPersistentEnvironment.Filter<S, L, O, IResolutionPath<S, L, O>> nextFilter = Environments.resolutionFilter(reference);

        // @formatter:off
        IPersistentEnvironment<S, L, O, IResolutionPath<S, L, O>> environment = scopeGraph.localReferencesStream()
            .filter(occurrenceEquals(reference))
            .findAny() // must be unique (TODO ensure this)
            .map(tuple -> tuple.scope())
            .map(scope -> env(nextSeenI, ordered, wf, Paths.empty(scope), nextFilter))
            .orElse(Environments.empty());
        // @formatter:on

        return environment;       
    }

    /**
     * Calculate new environment if path is well-formed, otherwise return an
     * empty environment.
     */
    private <P extends IPath<S, L, O>> IPersistentEnvironment<S, L, O, P> env(Set.Immutable<O> seenImports,
            IRelation<L> lt, IRegExpMatcher<L> re, IScopePath<S, L, O> path,
            IPersistentEnvironment.Filter<S, L, O, P> filter) {
        if (re.isEmpty()) {
            return Environments.empty();
        } else {
            return env_L(labels, seenImports, lt, re, path, filter);
        }
    }

    /**
     * Returns the set of declarations that are reachable from S with a
     * l-labeled step.
     */
    private <P extends IPath<S, L, O>> IPersistentEnvironment<S, L, O, P> env_l(Set.Immutable<O> seenImports,
            IRelation<L> lt, IRegExpMatcher<L> re, L l, IScopePath<S, L, O> path,
            IPersistentEnvironment.Filter<S, L, O, P> filter) {

        return Environments.guarded((PartialFunction0<IPersistentEnvironment<S, L, O, P>> & Serializable) () -> {
            // NOTE: capturing immutable state: scopeGraph, labelD
            // NOTE: capturing mutable state: scopeCounter

            if (scopeCounter.isOpen(path.getTarget(), l)) {
                return Optional.empty(); // no solution available currently
            }

            final IPersistentEnvironment<S, L, O, P> result;

            if (l.equals(labelD)) {
                // case: env_D

                if (!re.isAccepting()) {
                    result = Environments.empty();
                } else {
                    // @formatter:off
                    final Set.Immutable<P> paths = scopeGraph.localDeclarationsStream()
                        .filter(scopeEquals(path.getTarget()))
                        .map(tuple -> tuple.occurrence())
                        .flatMap(declaration -> OptionalStream.of(filter.test(Paths.decl(path, declaration))))
                        .collect(CapsuleCollectors.toSet());
                    // @formatter:on

                    result = Environments.of(paths);
                }

            } else {
                // case: env_nonD

                final IRegExpMatcher<L> nextRe = re.match(l);
                               
                if (nextRe.isEmpty()) {
                    // TODO check if importScopes calculation can be pruned as well                   
                    result = Environments.empty();
                } else {
                    final Function<IScopePath<S, L, O>, IPersistentEnvironment<S, L, O, P>> getter = p -> env(seenImports, lt, nextRe, p, filter);
                    
                    final Set.Immutable<IPersistentEnvironment<S, L, O, P>> directScopes = directScopes(seenImports, l, path, filter, getter);
                    final Set.Immutable<IPersistentEnvironment<S, L, O, P>> importScopes = importScopes(seenImports, l, path, filter, getter);

                    // TODO: currently union of two sequences of environments
                    // TODO: goal is to do better
                    result = Environments.union(Iterables.concat(directScopes, importScopes));
                }
            }

            return Optional.of(result);
        });
    }

    private <P extends IPath<S, L, O>> Set.Immutable<IPersistentEnvironment<S, L, O, P>> directScopes(
            Set.Immutable<O> seenImports, L l, IScopePath<S, L, O> path,
            IPersistentEnvironment.Filter<S, L, O, P> filter,
            Function<IScopePath<S, L, O>, IPersistentEnvironment<S, L, O, P>> getter) {

        final Function<S, Optional<IScopePath<S, L, O>>> extendPathToNextScopeAndValidate = nextScope -> Paths
                .append(path, Paths.direct(path.getTarget(), l, nextScope));              
        
        // @formatter:off
        final Set.Immutable<IPersistentEnvironment<S, L, O, P>> environments = scopeGraph.directEdgesStream()
            .filter(labelEquals(l))
            .filter(sourceScopeEquals(path.getTarget()))
            .map(tuple -> tuple.targetScope())
            .map(extendPathToNextScopeAndValidate)
            .flatMap(OptionalStream::of)
            .map(getter::apply)
            //.flatMap(nextScope -> OptionalStream.of(Paths.append(path, Paths.direct(path.getTarget(), l, nextScope)).map(getter::apply)))
            .collect(CapsuleCollectors.toSet());
        // @formatter:on

        return environments;
    }

    private <P extends IPath<S, L, O>> Set.Immutable<IPersistentEnvironment<S, L, O, P>> importScopes(
            Set.Immutable<O> seenImports, L l, IScopePath<S, L, O> path,
            IPersistentEnvironment.Filter<S, L, O, P> filter,
            Function<IScopePath<S, L, O>, IPersistentEnvironment<S, L, O, P>> getter) {
              
        final Function<IResolutionPath<S, L, O>, IPersistentEnvironment<S, L, O, P>> importPathToUnionEnvironment = importPath -> {   
            // @formatter:off        
            final Set.Immutable<IPersistentEnvironment<S, L, O, P>> importEnvironments = scopeGraph.exportDeclarationsStream()
                    .filter(labelEquals(l))
                    .filter(occurrenceEquals(importPath.getDeclaration()))
                    .map(tuple -> tuple.scope())
                    .flatMap(nextScope -> OptionalStream.of(Paths.append(path, Paths.named(path.getTarget(), l, importPath, nextScope)).map(getter::apply)))
                    .collect(CapsuleCollectors.toSet());
            // @formatter:on
            
            return Environments.union(importEnvironments);
        };

        final Function<IPersistentEnvironment<S, L, O, IResolutionPath<S, L, O>>, IPersistentEnvironment<S, L, O, P>> intermediateToFinal = environment -> {
            final Set.Immutable<IPersistentEnvironment<S, L, O, P>> importEnvironments = environment.solution()
                    .orElse(Set.Immutable.of()).stream().map(importPathToUnionEnvironment)
                    .collect(CapsuleCollectors.toSet());
            
            return Environments.union(importEnvironments);            
        };
        
        // @formatter:off        
        final Set.Immutable<IPersistentEnvironment<S, L, O, P>> environments = scopeGraph.importReferencesStream()
            .filter(scopeEquals(path.getTarget()))
            .filter(tuple -> !seenImports.contains(tuple.occurrence()))
            .map(tuple -> tuple.occurrence())
            .map(reference -> resolveEnv(seenImports, reference))
            .map(intermediateToFinal)            
            .collect(CapsuleCollectors.toSet());
        // @formatter:on     
        
        return environments;       
    }

    private <P extends IPath<S, L, O>> IPersistentEnvironment<S, L, O, P> env_L(Set.Immutable<L> L,
            Set.Immutable<O> seenImports, IRelation<L> lt, IRegExpMatcher<L> re, IScopePath<S, L, O> path,
            IPersistentEnvironment.Filter<S, L, O, P> filter) {
        return stagedEnv_L.computeIfAbsent(lt, lo -> stageEnv_L(L, lt)).apply(seenImports, re, path, filter,
                Maps.newHashMap());
    }

    private EnvironmentL<S, L, O> stageEnv_L(Set<L> L, IRelation<L> lt) {
        List<EnvironmentL<S, L, O>> stagedEnvs = Lists.newArrayList();
        for (L l : max(lt, L)) {
            EnvironmentL<S, L, O> smallerEnv = stageEnv_L(smaller(lt, L, l), lt);
            stagedEnvs.add(new EnvironmentL<S, L, O>() {

                @Override
                public <P extends IPath<S, L, O>> IPersistentEnvironment<S, L, O, P> apply(Set.Immutable<O> seenI,
                        IRegExpMatcher<L> re, IScopePath<S, L, O> path,
                        IPersistentEnvironment.Filter<S, L, O, P> filter,
                        Map<L, IPersistentEnvironment<S, L, O, P>> env_lCache) {
                    final IPersistentEnvironment<S, L, O, P> env_l = Environments
                            .lazy((Function0<IPersistentEnvironment<S, L, O, P>> & Serializable) () -> {
                                return env_lCache.computeIfAbsent(l, ll -> env_l(seenI, lt, re, l, path, filter));
                            });
                    return Environments.shadow(filter, smallerEnv.apply(seenI, re, path, filter, env_lCache), env_l);
                }

            });
        }
        return new EnvironmentL<S, L, O>() {

            @Override
            public <P extends IPath<S, L, O>> IPersistentEnvironment<S, L, O, P> apply(Set.Immutable<O> seenI,
                    IRegExpMatcher<L> re, IScopePath<S, L, O> path, IPersistentEnvironment.Filter<S, L, O, P> filter,
                    Map<L, IPersistentEnvironment<S, L, O, P>> env_lCache) {
                return Environments.union(stagedEnvs.stream().map(se -> se.apply(seenI, re, path, filter, env_lCache))
                        .collect(Collectors.toList()));

            }

        };
    }

    private Set.Immutable<L> max(IRelation<L> lt, Set<L> L) {
        Set.Transient<L> maxL = Set.Transient.of();
        tryNext: for (L l : L) {
            for (L larger : lt.larger(l)) {
                if (L.contains(larger)) {
                    continue tryNext;
                }
            }
            maxL.__insert(l);
        }
        return maxL.freeze();
    }

    private Set.Immutable<L> smaller(IRelation<L> lt, Set<L> L, L l) {
        Set.Transient<L> smallerL = Set.Transient.of();
        for (L smaller : lt.smaller(l)) {
            if (L.contains(smaller)) {
                smallerL.__insert(smaller);
            }
        }
        return smallerL.freeze();
    }

    // serialization

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        initTransients();
    }

}

class OptionalStream {

    public static final <T> Stream<T> of(Optional<T> optional) {
        if (optional.isPresent()) {
            return Stream.of(optional.get());
        } else {
            return Stream.empty();
        }
    }

}