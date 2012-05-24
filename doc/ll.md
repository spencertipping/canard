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

# Memory

There are four memory-related operators:

    * address = value     (dereference)
    ! value address       (assignment)
    ( size = address      (allocate)
    ) address             (free)

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