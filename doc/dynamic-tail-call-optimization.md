# Dynamic tail call optimization

Now that tail calls are not automatically generated, we have the flexibility to
rewrite all calling addresses (except those used with call/cc, but that is a
different matter). Here's the code:

    popq %rax                     <- continuation
    testb (%rax), $0xc3           <- is the continuation opcode RET?
    je rewrite_caller
    ...
    rewrite_caller:
      movb $0xe9, -5(%rax)        <- rewrite CALL to JMP
    ...

Here's the transformation in action:

    e8 xx | AA: c3                %rsp -> | ...
    (execute e8)                  %rsp -> | AA | ...
    popq %rax                     %rsp -> | ...           %rax = AA
    testb (%rax), $0x93           %rsp -> | ...           %rax = AA
    je rewrite_caller             (branch taken)
    movb $0xe9, -5(%rax)          -> e9 xx | AA: c3

Next time this code executes:

    e9 xx | AA: c3                %rsp -> | cc1 | ...
    (execute e9)                  %rsp -> | cc1 | ...
    popq %rax                     %rsp -> | ...           %rax = cc1
    testb (%rax), $0x93           %rsp -> | ...
    je rewrite_caller             (branch maybe taken)

Oops. This is a problem. We can't always control the two-level calling context
of a function; it's possible that we don't want to transitively tail-call
optimize. (Actually, it's fine to do this, but only for static call sites.)

Therefore, we need to know whether a call site is static or dynamic.