package org.metaborg.meta.nabl2.solver;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.scopegraph.INameResolution;
import org.metaborg.meta.nabl2.scopegraph.IScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.scopegraph.terms.Scope;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.generic.TermIndex;
import org.metaborg.meta.nabl2.unification.IUnifier;

import com.google.common.collect.Multimap;

@Value.Immutable
@Serial.Version(value = 1L)
public abstract class Solution implements ISolution {

    @Value.Parameter @Override public abstract IProperties<TermIndex> getAstProperties();
    
    @Value.Parameter @Override public abstract IScopeGraph<Scope,Label,Occurrence> getScopeGraph();

    @Value.Parameter @Override public abstract INameResolution<Scope,Label,Occurrence> getNameResolution();

    @Value.Parameter @Override public abstract IProperties<Occurrence> getDeclProperties();
    
    @Value.Parameter @Override public abstract IRelations getRelations();

    @Value.Parameter @Override public abstract IUnifier getUnifier();
 
    
    @Value.Parameter public abstract Multimap<ITerm,String> getErrors();

    @Value.Parameter public abstract Multimap<ITerm,String> getWarnings();

    @Value.Parameter public abstract Multimap<ITerm,String> getNotes();

}