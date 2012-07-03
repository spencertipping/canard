# Memory allocation

Sometimes it's useful to allocate some space for a native subroutine. This is
not the same thing as consing one thing onto another, since subroutine calls
modify the return stack. This memory also has some other characteristics:

    1. It is never going to be changed after it's written.
    2. It will probably never need to be GC'd (though cons cells might).
    3. It is not part of a GC root set; therefore, structural information can be
       lost.
    4. Chunks are allocated in strange sizes and require no alignment.

Allocation is simple; we just maintain a register that points to the next free
byte in the heap. The allocator looks like this:

    dpop  %rax            <- get amount of memory to allocate
    dpush %rbp            <- return pointer to memory
    addq  %rax, %rbp      <- skip past newly-allocated memory
    ret

The reason we don't use cons cells for these things is that conses are not
immediate; we want to be able to relocate and/or delete a cons cell without
breaking references. (In other words, cons cells should have by-value semantics,
not by-reference; we don't want them to have identity beyond being the tail of
some list.)