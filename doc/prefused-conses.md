# Prefused conses

Cons cells are immutable, so there isn't a real reason to fragment them prematurely. Instead, we can prefuse them; runs of sequential atoms can be represented in a linear string. This
completes the CPS-conversion and inlines things in the process. For example:

    = 'foo [bar bif baz]

Here, the definitions of bar, bif, and baz can be inlined into a single mega-cons:

    [baz definition] [bif definition] [bar definition] 0f1f80 00000000 c3

The only difficulty is that we need to know where each sub-definition begins and ends. It should be possible to do this by inserting three-byte nop instructions and using the ModR/M byte to
encode the length of the current 'instruction'. For example:

    0f1f 09 [baz definition: 9 bytes] 0f1f 0b [bif definition: 11 bytes] 0f1f 08 [bar definition: 8 bytes] 0f1f 80 00000000 c3
    ^                                 ^                                  ^                                 ^

The jump targets are marked with carets. It's important to refer to the nop instructions because these encode the length of the next list entry.

Update: function definitions can't be inlined due to the structural inspection problem. Instead, each defined symbol needs to have an identity (probably a memory address; these identities can
be opaque), and this identity needs to be the referent of the head of a cons. Inlining should happen inside the compiler, which is defined in user-space.

# Allocating prefused data

In a case like the map function, we can allocate a prefused cons easily enough by having the allocator hand us memory towards the end, not the beginning, of the heap. We can then request more
chunks and see whether they are contiguous. This means that the allocator needs to know how we're representing cons cells, and must be aware of the implicit sequencing going on. The GC must be
able to do the same thing in order to collect prefused segments that are no longer in use:

    0f1f 05 ... 0f1f 08 ... (live set ->) 0f1f 03 ... 0f1f 80 00000000 c3
    |---------------------|
     this needs to be GC'd
                                  <---- |
         future conses onto the same tail