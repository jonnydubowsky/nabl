package mb.nabl2.terms;

import com.google.common.collect.ImmutableClassToInstanceMap;

public interface ITermVar extends ITerm, IListTerm {

    String getResource();

    String getName();

    ITermVar withAttachments(ImmutableClassToInstanceMap<Object> value);

}