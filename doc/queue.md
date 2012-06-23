# Queue

The machine pushes a return address when you jump using e8. If you treat the
return address as a continuation (which it is), then you have a form of CPS
going on. So the roles of the data and return stacks become blurred.

Forth, for obvious and good reasons, separates the two stacks by their intended
use, but we don't have to do this. We can give the user a double stack instead:

    | <- stack 1 ----> |  ....  | <---- stack 2 -> |
                     rdi        rsp

It is straightforward to provide functions that move values from one stack to
the other:

    r<: popq %rax
        stosq

    r>: movq -8(%rdi), %rax
        lea -8(%rdi), %rdi
        pushq %rax

Doing it this way removes the need for the stash command, which is a good thing.
However, it may pose challenges when it comes to mixing return addresses and
data, depending on the granularity of continuations.

Doing it this way also more closely resembles a classical Turing machine, which
could prove useful (though I have no idea how at the moment).

# Problems with data-stack CPS

The biggest problem with this design is probably that r< and r> must work around
their own immediate continuations.