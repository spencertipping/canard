# Dynamic tail call optimization

Now that tail calls are not automatically generated, we have the flexibility to
rewrite all calling addresses (except those used with call/cc, but that is a
different matter). Here's the code:

    popq %rax                     <- continuation
    testb (%rax), $0xc3           <- is the continuation opcode RET?
    jne preserve_caller
    testb -5(%rax), $0xe8         <- is the call site using a CALL?
    jne preserve_caller
      movb $0xe9, -5(%rax)        <- rewrite CALL to JMP
    preserve_caller:
    ...