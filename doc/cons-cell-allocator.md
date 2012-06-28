# Cons cell allocator

Allocating cons cells efficiently should be straightforward. I think this
algorithm will work:

    pop %rax                      <- continuation
    pop %rbx                      <- tail element pointer
    pop %rcx                      <- head element pointer
    testq %rbx, %rbx              <- are we consing onto nil?
    jz encode_tail_call           <- if so, do a JMP instead of a CALL
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
    encode_tail_call:
      subq %rsi, %rcx             <- absolute to relative address
      movl %ecx, -4(%rsi)         <- copy 32-bit displacement
      lea -5(%rsi), %rsi          <- allocate head
      movb $0xe9, (%rsi)          <- encode JMP instruction (tail call)
    jmp *%rax                     <- invoke continuation

Notice that we use %rsi as the heap allocation pointer (and that it grows
downwards). This means that %rsi must be preserved across things like syscalls.

Here are the cases (... indicates the original position of %rsi):

    cons x []             %rsi -> | e9 xx xx xx xx | ...
    cons x y              %rsi -> | e8 xx xx xx xx | e9 yy yy yy yy | ...
    cons x cons y z       %rsi -> | e8 xx xx xx xx | e8 yy yy yy yy |
                                                     e9 zz zz zz zz | ...