# Low-level basis

The purpose of the low-level environment is to make it possible to write a compiler. Ideally, the compiler can compile itself, so the basis should be consistent modulo even lower-level
optimizations that can be emitted later on. The basic operation categories are:

    1. Stack manipulation of various sorts. This should be covered by stash, permute, and literals.
    2. Toplevel definition and introspection, even if this isn't used by compilers. This establishes the identity of Canard as an interpreter with compiler backends.
    3. Direct allocation, freeing, and access of memory.
    4. Integer and floating-point arithmetic, but basic instructions only. Instruction fusion should be compiler-specific.
    5. Control flow.

# Stack manipulation

Every literal pushes a value onto the stack. Canard provides three functions that manipulate stack values:

    ^ k [f] x0 x1 x2 ... xk-1 xk ... = x0 x1 x2 ... xk-1 f xk ...         (stash)
    < k x0 x1 x2 ... xk-1 xk ...     = xk ...                             (drop)
    > k x0 x1 x2 ... xk ...          = xk x0 x1 x2 ... xk ...             (get)

# Definition

Canard gives you two functions that deal with definitions:

    = name value          (define)
    @ name                (get-value)

Low-level interpreters are allowed to impose an O(n) time overhead to resolve symbol definitions.

# Memory

There are four memory-related operators:

    * address = value     (dereference)
    ! value address       (assignment)
    ( size = address      (mmap; generally requires multiples of 4096 bytes)
    ) address             (munmap)
    { address             (mprotect +x; make page executable but not writable)
    } address             (mprotect +w; make page writable but not executable)

# Arithmetic

These mirror processor intrinsics:

    + x y   = x+y
    - x y   = x-y
    / x y   = x/y x%y
    % x y   = x*y
    & x y   = x&y
    | x y   = x|y
    ~ x     = ~x
    not x   = !x
    xor x y = x^y
    << n x  = x<<n
    >> n x  = x>>n
    >>> n x = x>>>n

# Control flow

There is only one control flow statement:

    ? 0 [true-branch] [false-branch] = false-branch
    ? x [true-branch] [false-branch] = true-branch        (x != 0)

# Primitive representation

What happens when you inspect something like the ^ function? Theoretically you would see a list of stack instructions, but in this case that doesn't make sense; there aren't other stack
instructions that will end up producing ^. Maybe the right move here is to provide some sort of machine code representation of the intrinsic. It could be the case that lists are homomorphic to
machine code anyway, at least across evaluation. (!)