package org.metaborg.meta.nabl2.unification;

import org.metaborg.meta.nabl2.terms.ITerm;

public class UnificationException extends Exception {

    private static final long serialVersionUID = 1L;

    public UnificationException(ITerm t1, ITerm t2) {
        super("Cannot unify " + t1 + " with " + t2);
    }

}
