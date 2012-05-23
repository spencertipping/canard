Low-level basis.
The purpose of the low-level environment is to make it possible to write a compiler. Ideally, the compiler can compile itself, so the basis should be consistent modulo even lower-level
optimizations that can be emitted later on. The basic operation categories are:

| 1. Stack manipulation of various sorts. This should be covered by stash, permute, and literals.
  2. Toplevel definition and introspection, even if this isn't used by compilers. This establishes the identity of Canard as an interpreter with compiler backends.
  3. Direct allocation, freeing, and access of memory.
  4. Integer and floating-point arithmetic, but basic instructions only. Instruction fusion should be compiler-specific.
  5. Cons and uncons, as well as symbol inspection and ordering. This can form the basis for things like sets.
