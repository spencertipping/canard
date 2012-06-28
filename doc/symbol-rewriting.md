# Symbol rewriting

Now that all symbols within a list share a calling convention, we can look again
at dynamic symbol-resolution rewriting. Specifically, can we reliably identify
monomorphic call sites and rewrite them?

A monomorphic call site is defined as one that, regardless of inputs, always
calls the same block of code (or, in this case, symbol). We don't want to
complexify the base layer too much, but anything goes if it makes the difference
between tail call optimization and stack overflows.

We can't just rewrite the location five bytes before the return address, since
the return address might be a tail call proxy. Instead, we need to explicitly
invoke a patchup function that fixes the invocation site.