# Garbage collection

Trivially, we can implement a conservative GC by going through the data stack
and marking all things which satisfy:

    1. The thing points to allocated memory.
    2. The thing is an x86 instruction whose relative offset refers to allocated
       memory.

Maybe it should be possible to add new GC traversal strategies. GC involves some
knowledge about the objects being traced, so maybe it's appropriate to use some
sort of stronger type information.

The simplest way to do this is to use the symbol table as the root (along with
any stack entries that point to valid ranges of memory), and decode the x86
instructions to determine the things they reference. Most references will be of
these forms:

    1. Relative jump (possibly conditional)
    2. Memory access
    3. Closure value (48b8 ... 48ab)

Of the three, closure values are probably the hardest to deal with because it is
very unclear what the value means. In the worst case, the value could be a
series of bytes that will be executed as code; determining what it refers to in
this case is impossible because we don't know where it will be placed.

Because of all this, we may need to figure out a simpler way to mark objects as
being in use. A great way is just to use the heuristic that objects referred to
by the data stack or return stack, or bound in the global symbol table, are
marked. This places a burden on the programmer: any value you care about needs
to have a solid reference.