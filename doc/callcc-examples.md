# Call/cc examples

It makes sense to implement continuations in terms of a function analogous to call/cc. For example:

    call/cc [. %s 5]              <- the same thing as 5
    call/cc [.]                   <- the same thing as []
    call/cc []                    <- the same thing as r<

Some questions:

    1. How is the data stack tied to the return stack, if at all? Is the data stack part of a continuation?
    2. How is the current continuation replaced by a different one?
    3. Does the FP-style continuation model make sense in a concatenative context?
    4. Is continuation modeling even necessary?