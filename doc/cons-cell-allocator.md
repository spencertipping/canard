# Cons cell allocator

Allocating cons cells efficiently should be straightforward. I think this
algorithm will work:

    pop %rax                      <- continuation
    pop %rbx                      <- tail element pointer
    pop %rcx                      <- head element pointer
    testq %rbx, %rsi              <- are we consing onto the last cell?
    je after_tail                 <- if so, don't cons the tail
      subq %rsi, %rbx             <- absolute to relative address
      movl %ebx, -4(%rsi)         <- copy 32-bit displacement into place
      lea -5(%rsi), %rsi          <- subtract to finish the tail allocation
      movb $0xe9, (%rsi)          <- encode JMP instruction
    after_tail:
      subq %rsi, %rcx             <- absolute to relative address
      movl %ecx, -4(%rsi)         <- copy 32-bit displacement into place
      lea -5(%rsi), %rsi          <- allocate head of cons cell
      movb $0xe8, (%rsi)          <- encode CALL instruction
    jmp *%rax                     <- invoke continuation

Notice that we use %rsi as the heap allocation pointer (and that it grows
downwards). This means that %rsi must be preserved across things like syscalls.

Here are the cases (... indicates the original position of %rsi):

    cons [] x             %rsi -> | e8 xx xx xx xx | c3 | ...
    cons y x              %rsi -> | e8 xx xx xx xx | e9 yy yy yy yy | ...
    cons cons y z x       %rsi -> | e8 xx xx xx xx | e8 yy yy yy yy |
                                                     e9 zz zz zz zz | ...

Cons cells are more structural than they are referential. This is mostly because
the single byte c3 is more efficient to encode than a jmp instruction that
points to c3.