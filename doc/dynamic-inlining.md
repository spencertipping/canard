# Dynamic inlining

It is simple to remove indirection lazily by modifying calling code. For
example:

    call symbol_f_definition
    ...
    symbol_f_definition:
      movq $symbol_f_bound_value, %rax
      movq (%rsp), %rbx
      subq %rbx, %rax
      movl %eax, -4(%rbx)
      jmp -5(%rbx)
    ...
    symbol_f_bound_value:
      ...

Or some such. The idea is to patch up the caller so that the next call goes
straight to the symbol's destination rather than through the symbol table. After
the call has been executed, the code should become:

    call symbol_f_bound_value
    ...
    symbol_f_definition:
      ...
    symbol_f_bound_value:
      ...

The result is that we've removed the unnecessary indirection through the symbol
table.

# Inlining and CPS

CPS is fundamentally at odds with symbol-inlining because we're relying on the
caller to specify its own continuation as the return address. If it doesn't do
this, then we'll modify someone else's code. For example:

    pushq %rax            <- continuation, but not our_continuation
    jmp symbol_f_definition
    our_continuation:
      ...

Here, the five bytes preceding (%rax) will be modified; this is undesirable
because it will probably not be the calling site. To avoid this, we need to
invoke the indirect symbol_f_definition instead:

    pushq %rax
    jmp symbol_f_definition_indirect

This one behaves just like symbol_f_definition but does not patch the caller.

# Philosophical implications

What we're doing here is just removing dynamicism from the program at runtime.
Symbols cannot be redefined, so there isn't much reason to preserve the
indirection through the symbol table once we know what the destination is.

We can't fix up tail calls (which are a special case of call/cc) because we
don't know where the continuation came from:

    call/cc [foo bar]

Here, [foo bar] will tail-call to the outer continuation; however, this is not
always the case:

    . [foo bar]