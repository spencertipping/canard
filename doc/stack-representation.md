# Stack representation

Canard uses multiple stacks to execute a program. The central one is the data stack; this is the only stack that is implicitly accessed by primitive functions, and this stack maintains
pointers to the others. The other primary stack is used for return addresses.

Every stack is encoded as an immutable linked list and can be manipulated as such. In fact, the data stack can be accessed in a first-class way as well.

# Function calls

Canard doesn't provide a native calling convention; as such, stack commands aren't really functions. The function call abstraction is a by-product of the way the evaluation function (.) works.
Here are the relevant expansions:

    . :: x y -> . @ y r< [. x]
    . []     -> . r>

Here, the recursive definition is really a way to encode a jump operation since all calls are tail calls. The "current continuation" can be pushed onto the data stack as a list using @cc. The
return stack is used only in the explicit context implied by r< and r>; the interpreter itself doesn't do anything particularly interesting with it.

Note: This evaluation strategy imposes a significant amount of GC overhead. It may be worth reconsidering. However, I think there are allocation heuristics that can be used to ameliorate the
effect.