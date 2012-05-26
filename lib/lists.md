Canard standard library: list functions | Spencer Tipping
Licensed under the terms of the MIT source code license

# Introduction

This library defines useful list functions, including map, flatmap, filter, append, etc. All traversal goes from right to left, as per the natural cons cell ordering.

# Flatmap and map definitions

We use the initial definition of :* below as the template, replacing the :: invocation with other functions as appropriate. As a test case, we self-apply :* to its own definition.

# List append

Appends each element from the second list to the first. The head of :+ x y is the head of y. Here are the cases:

    :+ _ :: t h -> :: :+ _ t h
    :+ x []     -> x

    = ':+ [? [:: :+ %^ 1 [:^]] [%v %s] :? %1]

# Recursive combinator

Recursively descends throughout a list; useful for doing search/replace on chunks of code. This is also templated to support flat-mapping recursively; as such, it is a transformation of the
block given to a map or flat-map function. Here are the cases:

    :** [map] [f] :: x y -> map :** [map] [f] :: x y
    :** [map] [f] _      -> f _

You can use it like this: :* :** [+ 1] [1 2 [3 4] 5], yielding [2 3 [4 5] 6].

    = ':** [? [. %^ 1 [:**] %0] [. %v] :? %2]

# Map-function template

Here is the derivation for flatmap:

    :~ [f] :: x y -> :+ :~ [f] x f y
    %^ 1 [:^]    [f] :: x y  = [f] x y
    %% 2 [0 1 0] [f] x y     = [f] x [f] y
    %^ 2 [.]     [f] x [f] y = [f] x f y
    :+ :~        [f] x f y   = :+ :~ [f] x f y

Notice, however, that a number of functions here can be replaced to do other things. For instance, the :+ invocation in flatmap can be replaced with ::, at which point the function will be a
regular map. What we really have is a 'map template' that can be specialized in different ways. To leverage this, we first define the map function, then we write the map template, then we
rewrite the map function in terms of that.

    = ':* [? [:: :* %^ 2 [.] %% 2 [0 1 0] %^ 1 [:^]] [%v] :? %1]