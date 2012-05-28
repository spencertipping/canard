# Continuations

In Canard, expressions (lists) are executed from right to left following the cons tree. So E[x :: y] = E[x] I[y], making cons homomorphic to composition. Because of this, a continuation can be
represented as a tuple (list, return-stack-head, data-stack-head), where the list is code and the two stack heads are pointers to immutable cons cells.

The return stack is required because blocks have return continuations, but it might not be required if CPS-conversion were performed up front. I'm not sure about this.

# That's a cop-out

Right, it is. Ok, so here's how cons cells are evaluated:

    E[:: x y] = E[x] I[y]

So cons corresponds to a form of composition. Here, the continuation of I[y] is E[x]; so in general the continuation of x y z is [x y]. E[] doesn't represent a tail-evaluation because it must
itself keep track of the continuation E[x]. Someone needs to keep track of this in any case; otherwise there is no implicit 'next command'.

# The return stack

This works as a stack of lists. Suppose, for example, you've got something like this:

    a b . [c d . [e] f] g

Here's the sequence of return stack contents after each step:

     0. Receive input             [a b . [c d . [e] f] g]
     1. Execute g                 [a b . [c d . [e] f]]
     2. Execute [c d . [e] f]     [a b .]
     3. Execute .                 [c d . [e] f] [a b]
     4. Execute f                 [c d . [e]] [a b]
     5. Execute [e]               [c d .] [a b]
     6. Execute .                 [e] [c d] [a b]
     7. Execute e                 [] [c d] [a b]
     8. Execute []                [c d] [a b]
     9. Execute d                 [c] [a b]
    10. Execute c                 [] [a b]
    11. Execute []                [a b]
    12. Execute b                 [a]
    13. Execute a                 []
    14. Execute []                <empty>

So a continuation is the pair of data/return stack values, assuming immutable cons cells and stack entries in each case.