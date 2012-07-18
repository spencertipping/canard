# CPS and the return stack

The reader should leverage implicit CPS if possible; this would allow it to be
resumable across system I/O, for example, allowing for inversion of control.
However, there are a couple of good reasons this doesn't necessarily make sense:

    1. What does it mean to reuse or alias a previous parse state? If this is
       allowed, then it imposes considerable GC overhead.
    2. The return stack is linear, not in a tree form. This means that proper
       continuations can't be stored without copying the return stack, which at
       this point is not possible.

Given this, it may be simpler to have the reader hard-wired to call a
buffer-fill function and use the jonesforth strategy.