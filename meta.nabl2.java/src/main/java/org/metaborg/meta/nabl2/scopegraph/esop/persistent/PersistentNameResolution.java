package org.metaborg.meta.nabl2.scopegraph.esop.persistent;

import static org.metaborg.meta.nabl2.util.tuples.HasOccurrence.occurrenceEquals;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
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
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopNameResolution;
import org.metaborg.meta.nabl2.scopegraph.path.IDeclPath;
import org.metaborg.meta.nabl2.scopegraph.path.IPath;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
import org.metaborg.meta.nabl2.scopegraph.path.IScopePath;
import org.metaborg.meta.nabl2.scopegraph.terms.path.Paths;
import org.metaborg.meta.nabl2.util.tuples.ImmutableTuple2;
import org.metaborg.meta.nabl2.util.tuples.Tuple2;

import com.google.common.annotations.Beta;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.Set.Immutable;
import io.usethesource.capsule.util.stream.CapsuleCollectors;

public class PersistentNameResolution<S extends IScope, L extends ILabel, O extends IOccurrence, V>
        implements IEsopNameResolution<S, L, O>, Serializable {

    private static final long serialVersionUID = 42L;

    private final PersistentScopeGraph<S, L, O, V> scopeGraph;

    private final Set.Immutable<L> labels;
    private final L labelD;
    private final IRegExpMatcher<L> wf;

    private final IRelation.Immutable<L> ordered;
    private final IRelation.Immutable<L> unordered;

    transient private Map<O, IPersistentEnvironment<S, L, O, IResolutionPath<S, L, O>>> resolutionCache;

    transient private Map<S, IPersistentEnvironment<S, L, O, IDeclPath<S, L, O>>> visibilityCache;
    transient private Map<S, IPersistentEnvironment<S, L, O, IDeclPath<S, L, O>>> reachabilityCache;

    transient private Map<IRelation<L>, EnvironmentBuilder<S, L, O>> environmentBuilderCache;

    public PersistentNameResolution(PersistentScopeGraph<S, L, O, V> scopeGraph, IResolutionParameters<L> params) {
        this.scopeGraph = scopeGraph;

        this.labels = Set.Immutable.<L>of().__insertAll(Sets.newHashSet(params.getLabels()));
        this.labelD = params.getLabelD();
        this.wf = RegExpMatcher.create(params.getPathWf());
        this.ordered = params.getSpecificityOrder();
        assert ordered.getDescription().equals(
                RelationDescription.STRICT_PARTIAL_ORDER) : "Label specificity order must be a strict partial order";
        this.unordered = Relation.Immutable.of(RelationDescription.STRICT_PARTIAL_ORDER);

        initTransients();
        
        // stage and cache environment builders
        getEnvironmentBuilder(ordered);
        getEnvironmentBuilder(unordered);
    }

    private void initTransients() {
        this.resolutionCache = Maps.newHashMap();
        this.visibilityCache = Maps.newHashMap();
        this.reachabilityCache = Maps.newHashMap();                
        this.environmentBuilderCache = Maps.newHashMap();
    }

    @Beta
    public final Set.Immutable<L> getLabels() {
        return labels;
    }    
    
    @Beta
    public final PersistentScopeGraph<S, L, O, V> getScopeGraph() {
        return scopeGraph;
    }

    @Beta
    public final L getLabelD() {
        return labelD;
    }

    @Beta
    public final IRelation<L> getOrdered() {
        return ordered;
    }

    @Beta
    public final IRelation<L> getUnordered() {
        return unordered;
    }

    @Beta
    public final IRegExpMatcher<L> getWf() {
        return wf;
    }

    // NOTE: never used in project
    @Deprecated
    public Set.Immutable<S> getAllScopes() {
        return scopeGraph.getAllScopes();
    }

    // NOTE: all references could be duplicated to get rid of scope graph
    // reference
    public Set.Immutable<O> getAllRefs() {
        return scopeGraph.getAllRefs();
    }

    @Override
    public Optional<Set.Immutable<IResolutionPath<S, L, O>>> resolve(O ref) {
        // return tryResolve(ref).map(Tuple2::_1).orElse(Set.Immutable.of());
        return tryResolve(ref).map(Tuple2::_1);
    }

    @Override
    public Optional<Set.Immutable<O>> visible(S scope) {
        final Optional<Set.Immutable<IDeclPath<S, L, O>>> result = tryVisible(scope).map(Tuple2::_1);
        
        if (result.isPresent()) {
            return Optional.of(result.get().stream().map(path -> path.getDeclaration()).collect(CapsuleCollectors.toSet()));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Set.Immutable<O>> reachable(S scope) {
        final Optional<Set.Immutable<IDeclPath<S, L, O>>> result = tryReachable(scope).map(Tuple2::_1);
        
        if (result.isPresent()) {
            return Optional.of(result.get().stream().map(path -> path.getDeclaration()).collect(CapsuleCollectors.toSet()));
        } else {
            return Optional.empty();
        }        
    }
    
    public Optional<Tuple2<Set.Immutable<IResolutionPath<S, L, O>>, Set.Immutable<String>>> tryResolve(O reference) {
//        final IPersistentEnvironment<S, L, O, IResolutionPath<S, L, O>> environment = resolutionCache
//                .computeIfAbsent(reference, r -> resolveEnvironment(Set.Immutable.of(), r, this));
        
        // without cache ...
        final IPersistentEnvironment<S, L, O, IResolutionPath<S, L, O>> environment = resolveEnvironment(Set.Immutable.of(), reference, this);

        return environment.solution().map(paths -> ImmutableTuple2.of(paths, Set.Immutable.of()));
    }

    public Optional<Tuple2<Set.Immutable<IDeclPath<S, L, O>>, Set.Immutable<String>>> tryVisible(S scope) {
//        final IPersistentEnvironment<S, L, O, IDeclPath<S, L, O>> environment = visibilityCache.computeIfAbsent(scope,
//                s -> visibleEnvironment(s, this));
        
        final IPersistentEnvironment<S, L, O, IDeclPath<S, L, O>> environment = visibleEnvironment(scope, this);        

        return environment.solution().map(paths -> ImmutableTuple2.of(paths, Set.Immutable.of()));
    }

    public Optional<Tuple2<Set.Immutable<IDeclPath<S, L, O>>, Set.Immutable<String>>> tryReachable(S scope) {
//        final IPersistentEnvironment<S, L, O, IDeclPath<S, L, O>> environment = reachabilityCache.computeIfAbsent(scope,
//                s -> reachableEnvironment(s, this));

        final IPersistentEnvironment<S, L, O, IDeclPath<S, L, O>> environment = reachableEnvironment(scope, this);
        
        return environment.solution().map(paths -> ImmutableTuple2.of(paths, Set.Immutable.of()));
    }

    private static final <S extends IScope, L extends ILabel, O extends IOccurrence, V> IPersistentEnvironment<S, L, O, IDeclPath<S, L, O>> visibleEnvironment(final S scope, final PersistentNameResolution<S, L, O, V> nameResolution) {
        return buildEnvironment(
                Set.Immutable.of(), 
                nameResolution.getOrdered(), 
                nameResolution.getWf(), 
                Paths.empty(scope),
                Environments.identityFilter(), 
                Optional.empty(), nameResolution, false);
    }

    private static final <S extends IScope, L extends ILabel, O extends IOccurrence, V> IPersistentEnvironment<S, L, O, IDeclPath<S, L, O>> reachableEnvironment(final S scope, final PersistentNameResolution<S, L, O, V> nameResolution) {   
        return buildEnvironment(
                Set.Immutable.of(), 
                nameResolution.getUnordered(), 
                nameResolution.getWf(), 
                Paths.empty(scope),
                Environments.identityFilter(), 
                Optional.empty(), nameResolution, false);
    }
    
    static final <S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>, V> IPersistentEnvironment<S, L, O, IResolutionPath<S, L, O>> resolveEnvironment(
            final Set.Immutable<O> seenImports, final O reference,
            /***/
            PersistentNameResolution<S, L, O, V> nameResolution) {
        
        final PersistentScopeGraph<S, L, O, V> scopeGraph = nameResolution.getScopeGraph();
        
        // TODO: use hash lookup on occurrence instead of filter

        final Set.Immutable<O> nextSeenImports = seenImports.__insert(reference);
        final IPersistentEnvironment.Filter<S, L, O, IResolutionPath<S, L, O>> nextFilter = Environments
                .resolutionFilter(reference);
        final Optional<O> nextReference = Optional.of(reference);
        
        // EXPERIMENTAL
        boolean eagerEvaluation = true;
        
        // @formatter:off
        IPersistentEnvironment<S, L, O, IResolutionPath<S, L, O>> environment = scopeGraph.referenceEdgeStream()
            .filter(occurrenceEquals(reference))
            .findAny() // must be unique (TODO ensure this)
            .map(tuple -> tuple.scope())
            .map(scope -> buildEnvironment(nextSeenImports, nameResolution.getOrdered(), nameResolution.getWf(), Paths.empty(scope), nextFilter, nextReference, nameResolution, eagerEvaluation))
            .orElse(Environments.empty());
        // @formatter:on 
        
        return environment;
    }

    /**
     * Calculate new environment if path is well-formed, otherwise return an
     * empty environment.
     */
    static final <S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>, V> IPersistentEnvironment<S, L, O, P> buildEnvironment(
            Set.Immutable<O> seenImports,
            IRelation<L> lt, IRegExpMatcher<L> re, IScopePath<S, L, O> path,
            IPersistentEnvironment.Filter<S, L, O, P> filter,
            Optional<O> resolutionReference,
            PersistentNameResolution<S, L, O, V> nameResolution,
            boolean eagerEvaluation) {
        if (re.isEmpty()) {
            return Environments.empty();
        } else {
            final EnvironmentBuilder<S, L, O> builder = nameResolution.getEnvironmentBuilder(lt);

            final IPersistentEnvironment<S, L, O, P> environment = builder.build(builder, seenImports, re, path, filter,
                    Maps.newHashMap(), resolutionReference, nameResolution, eagerEvaluation);
            
            return environment;
        }
    }
        
    /**
     * Retrieves an environment builder for for a relation of labels. 
     */
    public EnvironmentBuilder<S, L, O> getEnvironmentBuilder(final IRelation<L> lt) {
        return environmentBuilderCache.computeIfAbsent(lt, key -> EnvironmentBuilders.stage(key, labels));
    }
    
    // serialization

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        initTransients();
    }

    @Override
    public java.util.Set<O> getResolvedRefs() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public java.util.Set<Map.Entry<O, Set.Immutable<IResolutionPath<S, L, O>>>> resolutionEntries() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Not yet implemented.");
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
