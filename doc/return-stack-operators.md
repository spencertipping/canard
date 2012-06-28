# Return stack operators

Note: This design is incomplete; calling conventions need to deal with the
return continuation more explicitly than this.

Return stack values are manipulated using r< and r>. However, there is some
subtlety to think about here. The reason is that each one of these is a function
call; therefore, here's what happens when you use one:

    continuation r< x y ... =[1] (definition of r<) [continuation] x y ...
                            =[2] [continuation] y ...
                            =[3] continuation y ...

The transition from [1] to [2] above involved putting x onto the return stack.
Here's what r< looks like in assembly language:

    pop %rbx      <- continuation
    pop %rax      <- data to push onto return stack
    stosq         <- push onto return stack
    jmp *%rbx     <- invoke continuation

The definition of r> is similar:

    pop %rbx              <- continuation
    movq -8(%rdi), %rax   <- load value from return stack into %rax
    lea  -8(%rdi), %rdi
    pushq %rax            <- push onto data stack
    jmp *%rbx             <- invoke continuation

So far so good. The problem is that the enclosing function has its own
continuation that needs to be dealt with.