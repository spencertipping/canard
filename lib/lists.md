Canard standard library: list functions | Spencer Tipping
Licensed under the terms of the MIT source code license

# Introduction

This library defines useful list functions, including map, flatmap, filter, append, etc. All traversal goes from right to left, as per the natural cons cell ordering.

# Flatmap definition

Flat-map is structured just like map. The only difference is that the intermediate results are appended rather than consed. Also, it needs to make a recursive call to itself rather than to the
map function.

    = ':~ :** [? [':~ %v] [] $= ':* %0 ? [':+ %v] [] $= ':: %0] @ ':*

# Map-function template

Here is the derivation for flatmap:

    :~ [f] :: x y -> :+ :~ [f] x f y
    ^1 [:^]      [f] :: x y  = [f] x y
    %% 2 [0 1 0] [f] x y     = [f] x [f] y
    ^2 [.]       [f] x [f] y = [f] x f y
    :+ :~        [f] x f y   = :+ :~ [f] x f y

Notice, however, that a number of functions here can be replaced to do other things. For instance, the :+ invocation in flatmap can be replaced with ::, at which point the function will be a
regular map. What we really have is a 'map template' that can be specialized in different ways. To leverage this, we first define the map function, then we write the map template, then we
write flatmap as a substitution over the definition of 'map'.

    = ':* [? [:: :* ^2 [.] %% 2 [0 1 0] ^1 [:^]] [%v] :? %1]

# List append

Appends each element from the second list to the first. The head of :+ x y is the head of y. Here are the cases:

    :+ _ :: t h -> :: :+ _ t h
    :+ x []     -> x

    = ':+ [? [:: :+ ^1 [:^]] [%v %s] :? %1]

# List length

This is the usual recursive length function over lists.

    = ':# [? [+ 1 :# :t] [0 %v] :? %0]

# Cons accessors

These retrieve the individual pieces of a cons cell.

    = ':h [%v :^]
    = ':t [%% 2 [0] :^]