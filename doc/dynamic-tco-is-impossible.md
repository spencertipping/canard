# Dynamic TCO is impossible

This is the case because call sites are aliased. This happens because of the
identity that '. [x]' and 'x' are functionally equivalent (though they are not
the same lists).

The problem is that the cons cell that points to the . is shared across all
values of the list it is invoking. Therefore, we can't modify it unless we can
prove that it is monomorphic (but if it were, the odds are good that it would
have been written as just 'x', not '. [x]').

An alternative is to eagerly clone the code over the domain of monomorphism ...
but this is a serious undertaking and is probably impossible.

# Update

This is not at all the case. See note::dynamic-tail-call-optimization.