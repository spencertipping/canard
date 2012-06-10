# Stack permutation

What is the most concise, systematic way to permute the stack? Having a series of operators dedicated for the purpose seems like a reasonable solution if a solid basis exists, but I get the
sense that no such basis exists at the moment. Maybe I'm wrong about this. But it probably varies widely across implementations; Forth and Joy wouldn't use the same primitives.

## Stashing

It needs to be possible to grab values from the data stack, ignore them, and restore them later on. The initial Canard implementation uses 'stash' to do this; 'stash [f]' executes 'f' on the
tail of the stack, restoring the head afterwards. This concept could be generalized to stash multiple stack entries rather than just one.

## Shuffling

All forms of shuffling, dropping, and duplication can be generalized into a permutation vector. Permutation could be implemented like this:

    permute n [i1 i2 ... ik]

The idea is that the top n items would be dropped, then replaced by items at i1, i2, ..., ik in the original stack. For instance, swap would be defined as 'permute 2 [1 0]'. Drop is 'permute
1 []'. Dup is 'permute 1 [0 0]'. Get is 'permute 0 cons []'.

# A better idea

There is no reason to make permutation general-purpose, since arguably the stack is used with fixed arity. Therefore, we can abbreviate by providing a series of stack transformations up-front,
akin to the usual dup, swap, drop, etc. These are more compact, however:

    %0     <- identity (not provided)
    %0a    <- dup
    %0b    <- nip
    %0c    <- nip2
    %1     <- drop
    %1a    <- identity (not provided)
    %2ab   <- identity (not provided)
    %2ba   <- swap
    %3cba  <- swap around middle item
    %4badc <- swap two pairs, within each
    %4cdab <- swap two pairs, each intact
    ...

Up to four stack items can be rearranged into any order. This requires about 300 predefined functions, but these functions can be assembled into a small algebra. Stashing also works by using
predefined functions for up to four levels:

    ^1 [f] x y z     = x f y z
    ^2 [f] x y z     = x y f z
    ^3 [f] x y z t   = x y z f t
    ^4 [f] x y z t u = x y z t f u