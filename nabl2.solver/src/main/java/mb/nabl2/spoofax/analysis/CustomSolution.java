package mb.nabl2.spoofax.analysis;

import static mb.nabl2.terms.matching.TermMatch.M;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.nabl2.constraints.messages.MessageInfo;
import mb.nabl2.constraints.messages.MessageKind;
import mb.nabl2.solver.messages.IMessages;
import mb.nabl2.solver.messages.Messages;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class CustomSolution {

    @Value.Parameter public abstract ITerm getAnalysis();

    @Value.Parameter public abstract IMessages getMessages();

    public static IMatcher<CustomSolution> matcher() {
        return M.tuple4(M.listElems(MessageInfo.matcherEditorMessage(MessageKind.ERROR)),
                M.listElems(MessageInfo.matcherEditorMessage(MessageKind.WARNING)),
                M.listElems(MessageInfo.matcherEditorMessage(MessageKind.NOTE)), M.term(), (t, es, ws, ns, a) -> {
                    Messages.Transient messages = Messages.Transient.of();
                    messages.addAll(es);
                    messages.addAll(ws);
                    messages.addAll(ns);
                    return ImmutableCustomSolution.of(a, messages.freeze());
                });
    }

}