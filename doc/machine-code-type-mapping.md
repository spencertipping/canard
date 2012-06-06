# Machine code type mapping

Values are analogous to machine-code operations across the list-program homomorphism. For example:

    transparent pointer to <address> = jmp <address>
    packed pointer to <address>      = call <address>
    nil                              = ret
    nop                              = transparent metadata
    anything else                    = primitive

In particular, the transparent pointer is interesting because you could easily enough have more than one of them. For example:

    address1: e8 prog1 e9 address2
    address2: e9 address3
    address3: c3

Here we have just one cons cell: prog1 :: nil. Address2 is an intermediate pointer, but its type is encoded by the machine instruction that represents it, which importantly has tail
precedence: jmp a -> jmp b === jmp b. Any machine operation with this property can be erased when converting back from the machine-code representation to the logical one.

Quoted symbols are encoded as instructions that push the symbol's address onto the data stack.