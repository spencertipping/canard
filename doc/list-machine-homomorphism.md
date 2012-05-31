# List/machine-language homomorphism

Canard lists can be translated into machine language via the compiler. In this sense, Canard is not an interpreter after all, but a compiler that does the translation up-front. This makes a
certain amount of sense.

The real question is whether all machine code has a list representation as well. If so, then primitives are just lists of numbers or some such, and this has some nice advantages from a
representational standpoint. How might this work? Here are some options:

    1. Basic blocks are encoded as simple lists of numbers (instruction prefixes, opcode, operands), and each list ends with a jump instruction. The jump instruction is a list pointer.
    2. Lists are always represented in some form that can be executed natively; so consing corresponds to jumping from one list into another.

There are probably other solutions, but I think (2) is interesting. Not sure what to do about primitive commands...