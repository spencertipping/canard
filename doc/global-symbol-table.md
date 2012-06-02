# Global symbol table

This is an interesting problem. Lists fundamentally contain symbols, not functions, since we don't know whether they will be executed. So we can't fully erase the identity of the symbols that
make up these lists, at least in the authoritative copy.

Therefore, inlining is not of much use beyond the obvious primitive case because otherwise the lists are impossible to reconstruct. For example:

    = 'foo [bar bif baz]

If foo is represented as 0f1f 09 ... 0f1f 0b ... 0f1f 08 ... 0f1f 80 00000000 c3, then how do we know that it was written as [bar bif baz] instead of [<definition of bar> <definition of bif>
<definition of baz>]? Preserving the function calls is important here:

    0f1f 09 e8 baz-offset 0f1f e8 bif-offset 0f1f 09 e8 bar-offset 0f1f 80 00000000 c3

It's starting to look like a mistake to use machine language as an authoritative list encoding for a few reasons:

    1. There is some overhead involved in allocating cons cells this way.
    2. There is also some overhead executing lists defined this way.
    3. Inlining is not generally possible, which will slow the processor down.
    4. Decoding is a pain and requires extra metadata.
    5. On processors/operating systems with the NX bit, we'll have to change the memory protection of the page where the list is allocated.

# Mutability vs immutability

Any recursive calls will need to be indirect, so it seems reasonable enough to just make the global table mutable. But it's fine if modifying it is expensive, since this should be an uncommon
case. Modifying a global symbol could invalidate arbitrarily much code, or it could even fail to be reflected in already-compiled functions.