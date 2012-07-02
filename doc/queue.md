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

    r>: lea -8(%rdi), %rdi
        movq (%rdi), %rax
        pushq %rax

Doing it this way removes the need for the stash command, which is a good thing.
However, it may pose challenges when it comes to mixing return addresses and
data, depending on the granularity of continuations.

Doing it this way also more closely resembles a classical Turing machine, which
could prove useful (though I have no idea how at the moment).

# Problems with data-stack CPS

The biggest problem with this design is probably that r< and r> must work around
their own immediate continuations. That is, they need to be implemented this
way:

    r<: pop %rbx                  <- immediate continuation
        pop %rax                  <- value for return stack
        stosq                     <- store the value
        jmp *%rbx                 <- invoke immediate continuation

    r>: pop %rbx                  <- immediate continuation
        lea -8(%rdi), %rdi        <- decrement return stack pointer
        movq (%rdi), %rax         <- value from return stack
        push %rax                 <- add value to data stack
        jmp *%rbx                 <- invoke continuation