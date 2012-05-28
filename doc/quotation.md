# Quotation

There are some strange things happening around quoted symbols. Here are the specifics:

    1. E[s] for some symbol s is the same as E[D[s]], where D[s] is the definition of s.
    2. The prefix ' returns a function which pushes the given symbol onto the data stack.
    3. E[:: x y] = E[x] I[y], not E[x] E[y].
    4. I[s] = E[s] for any symbol.
    5. I[:: x y] pushes :: x y onto the data stack.

The whole point behind the E/I distinction is that lists are quoted when in the head position. We want evaluation to distribute only across list tails, not heads. Therefore, we need to define
two separate functions to handle this case.