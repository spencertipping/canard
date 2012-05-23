# Continuations

In Canard, expressions (lists) are executed from right to left following the cons tree. So E[x :: y] = E[x] I[y], making cons homomorphic to composition. Because of this, a continuation can be
represented as a tuple (list, return-stack-head, data-stack-head), where the list is code and the two stack heads are pointers to immutable cons cells.

The return stack is required because blocks have return continuations, but it might not be required if CPS-conversion were performed up front. I'm not sure about this.