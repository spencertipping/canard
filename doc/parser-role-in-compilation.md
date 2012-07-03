# The parser's role in compilation

I've been overlooking an important opportunity here. The parser has the option
to generate list code that modifies itself -- for instance, looking up symbols.
Therefore, we can let the symbol table simply act as a partial function from
symbol to address (defaulting to a cons for the symbol table). The parser can
generate code that uses it. For example, here's the output of parsing '[foo bar]
bif':

    call invoke_symbol_bif_anon   <- anonymous function for 'bif'
    call invoke_list_anon         <- anonymous function to push [foo bar]
    ret                           <- nil
    invoke_symbol_foo_anon:
      dpush xx(%rip)              <- push 'bif' onto stack
      jmp invoke_symbol           <- invoke and rewrite calling address
    invoke_list_anon:
      dpush yy(%rip)              <- push [foo bar] onto the stack
      ret                         <- invoke continuation

Here's the interpreter core support code that does the rewriting:

    invoke_symbol:
      call resolve_symbol         <- convert symbol to address
      dpop %rax                   <- symbol definition address
      movq %rax, %rcx             <- save into another register... [1]
      subq (%rsp), %rax           <- relative to absolute
      movq (%rsp), %rbx           <- continuation address of caller
      movl %eax, -4(%rbx)         <- overwrite calling address
    jmp *%rcx                     <- [1] ...tail-call symbol definition