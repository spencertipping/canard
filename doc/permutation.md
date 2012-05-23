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