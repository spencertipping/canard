# The parser's role in compilation

I've been overlooking an important opportunity here. The parser has the option
to generate list code that modifies itself -- for instance, looking up symbols.
Therefore, we can let the symbol table simply act as a partial function from
symbol to address (defaulting to a cons for the symbol table). The parser can
generate code that uses it. For example, here's the output of parsing '[foo bar]
bif':

    call invoke_symbol_foo_anon   <- anonymous function for 'bif'
    call invoke_list_anon         <- anonymous function to push [foo bar]
    ret                           <- nil
    invoke_symbol_foo_anon:
      pop %rax                    <- continuation
      push xx(%rip)               <- push 'bif' onto stack
      push %rax                   <- push return continuation
      jmp invoke_symbol           <- tail call: resolve symbol and rewrite caller
    invoke_list_anon:
      pop %rax                    <- continuation
      push yy(%rip)               <- push [foo bar] onto the stack
      jmp *%rax                   <- invoke continuation