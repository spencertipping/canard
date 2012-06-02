# List/machine-language homomorphism

Canard lists can be translated into machine language via the compiler. In this sense, Canard is not an interpreter after all, but a compiler that does the translation up-front. This makes a
certain amount of sense.

The real question is whether all machine code has a list representation as well. If so, then primitives are just lists of numbers or some such, and this has some nice advantages from a
representational standpoint. How might this work? Here are some options:

    1. Basic blocks are encoded as simple lists of numbers (instruction prefixes, opcode, operands), and each list ends with a jump instruction. The jump instruction is a list pointer.
    2. Lists are always represented in some form that can be executed natively; so consing corresponds to jumping from one list into another.

There are probably other solutions, but I think (2) is interesting. Not sure what to do about primitive commands...

# Executable list representation

The x86-64 32-bit relative jump instruction uses a single-byte E9 opcode. This wouldn't add a lot of overhead to a cons cell and could easily enough fit into a 64-bit chunk. There are some
other considerations, however, among which are things like CPS conversion. Encoding the list's execution semantics into the list itself makes the compiler intrinsic, which may not be a great
idea. But let's follow this to see where it goes.

Cons cell (nop precedes each jump, and the first one is a call to push the continuation pointer):

    0f1f00 e8 hhhhhhhh 0f1f00 e9 tttttttt

Therefore, nil can be encoded as a ret instruction:

    0f1f80 00000000 c3

The trouble with this approach is that we have no way to encode primitive stack instructions. Technically, those should be directly encoded into the head of the list. So, for example, the +
instruction (add the top two stack values) would be something like this:

    4883ee08 488b4608 480106 e9 tttttttt
    subq $8, %rsi
    movq 8(%rsi), %rax
    addq %rax, (%rsi)
    jmp  tttttttt

Not sure whether this scales, however. Intel instructions have low enough information density that I can see this scheme breaking down for more complex instructions. Maybe it's possible to
overcome this by using very long cons cells...

Also, the existing scheme makes it very difficult to detect a cons cell. Testing a single continuation-jump byte seems really sketchy. The limit case is that we would need to decode the whole
thing as x86 instructions and figure out whether the e9 is a real opcode.

# Optimizing compilation

The list representation above is suboptimal in a few ways:

    1. Function calls are not inlined.
    2. There are nops all over the place.
    3. Adjacent instructions have not been fused. This can be resolved at a symbolic level; there is no particular reason to implement a dynamic register allocator, I think.
    4. The stack model is unerased (not sure whether it makes sense to tackle this).
    5. Any polymorphic primitives will have unerased decisionals (unfortunately, the VM does provide some of these).

So in order to optimize, we'll have to go back to the symbolic stage and start fusing instructions into a separate native instruction stream, doing some register-stack aliasing in the process.
This also means we'll need to know the shape of each primitive, which shouldn't be too difficult. (Now it's becoming clear why most stack VMs don't provide high-power stack manipulators, but
if we constant-fold first it shouldn't be an issue.)

Optimization should not be a big concern in the native representation of cons cells, since it can be handled from within the language. The real value of having native cons representation is
enabling a protocol by which low-level system programming can be achieved.