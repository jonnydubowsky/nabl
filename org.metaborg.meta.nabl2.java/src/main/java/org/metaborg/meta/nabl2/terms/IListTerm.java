package org.metaborg.meta.nabl2.terms;

import java.util.function.Function;

import org.metaborg.meta.nabl2.functions.CheckedFunction1;

public interface IListTerm extends ITerm, Iterable<ITerm> {

    int getLength();

    <T> T match(Cases<T> cases);

    interface Cases<T> extends Function<IListTerm,T> {

        T caseCons(IConsTerm cons);

        T caseNil(INilTerm nil);

    }

    <T, E extends Throwable> T matchThrows(CheckedCases<T,E> cases) throws E;

    interface CheckedCases<T, E extends Throwable> extends CheckedFunction1<IListTerm,T,E> {

        T caseCons(IConsTerm cons) throws E;

        T caseNil(INilTerm nil) throws E;

    }

}