# Why implement continuations

It may not be necessary to implement any sort of CPS natively. The reason is that concatenative programs are much easier to transform than applicative ones. The program is arguably already
CPS-converted; the evaluation function can be easily modified to maintain a heap-allocated return stack.

# Cases where continuations are useful

There are two cases where continuations are particularly compelling. One is to implement coroutines, and the other is to implement backtracking search algorithms.