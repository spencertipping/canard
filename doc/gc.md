# Garbage collection

Trivially, we can implement a conservative GC by going through the data stack
and marking all things which satisfy:

    1. The thing points to allocated memory.
    2. The thing is an x86 instruction whose relative offset refers to allocated
       memory.

Maybe it should be possible to add new GC traversal strategies. GC involves some
knowledge about the objects being traced, so maybe it's appropriate to use some
sort of stronger type information. (Need to think about this...)