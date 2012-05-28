Canard standard library: lexical scope | Spencer Tipping
Licensed under the terms of the MIT source code license

# Introduction

Writing concatenative code is interesting, but sometimes it's useful to have the usual lexical scoping with destructuring binds. Canard supports this via quotation and compiles the lexical
forms into concatenative ones. Right now it does not support applicative destructuring; the only thing you can destructure is the stack itself.

# Implementation

The implementation combs through the given code and performs beta-rewriting, doing nothing to avoid collision cases. This means that you should be very careful about lexical closures, as they
may exhibit incorrect identifier collisions. The most obvious case is defining the constant function k:

    = 'k fn [x] [fn [y] [x]]
    = 'return-y k 'y
    return-y 5                    <- returns 5, not y

This happens because fn is not aware of the global fn[] binding when it compiles the block into stack code; the second fn[] binding won't be compiled until the first function is run. In
general, it is unsafe to use lexical closures at all because of this problem; however, this can be fixed by replacing 'y' with a gensym at compile-time. Another option is to compile the inner
fn[] first, yielding concatenative code that invokes the 'x' function:

    = 'k fn [x] fn [y] [x]

This will force fn [y] [x] to be compiled ahead of time, yielding [x %% 1 []].