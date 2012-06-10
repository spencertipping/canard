Canard standard library: list functions | Spencer Tipping
Licensed under the terms of the MIT source code license

# Introduction

This library defines useful list functions, including map, flatmap, filter, append, etc. All traversal goes from right to left, as per the natural cons cell ordering.

# Recursive flatmap definition

This is the :** equivalent of flat-map. It gives you the flexibility to replace one item with many, even within a sublist. Behavior is this:

    :~* [f] :: x y -> :: [] :~ [:~* [f]] :: x y   (lists are singly-mapped)
    :~* [f] _      -> :! f _                      (item results are coerced into lists if not already)

    = ':~* [? [:: [] :~ :: [:~*]] [:! .] :? %1]

# List folds

These two functions fold up the elements of lists, passing them to a binary function. This function, presumably, returns one value for every two it consumes; but it is free to do other things
as well.

    :/ n [f] :: x y -> f :/ n [f] x y
    | :/ n [f] []     -> n

    = ':/ [? [. ^1 [:/] %1 ^2 [:^]] [^1 [%% 02 []]] :? %2]

# Flatmap definition

Flat-map is structured just like map. The only difference is that the intermediate results are appended rather than consed. Also, it needs to make a recursive call to itself rather than to the
map function.

    = ':~ :** @o @/ ':* [':~] @/ ':: [':+] @ ':*

# Recursive map

Like the regular map function, but distributes over sub-lists recursively. This is primarily useful for editing functions in their list form.

    :** [f] :: x y -> :* [:** [f]] :: x y
    :** [f] _      -> f _

You can use it like this: :** [+ 1] [1 2 [3 4] 5], yielding [2 3 [4 5] 6]. Note that you cannot use this function until map (:*) has been defined.

    = ':** [? [:* :: [:**]] [.] :? %1]

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

    = ':* [? [:: :* ^2 [.] %% 02 [00 01 00] ^1 [:^]] [%v] :? %1]

# List append

Appends each element from the second list to the first. The head of :+ x y is the head of y. Here are the cases:

    :+ _ :: t h -> :: :+ _ t h
    :+ x []     -> x

    = ':+ [? [:: :+ ^1 [:^]] [%v %s] :? %1]

# List length

This is the usual recursive length function over lists.

    = ':# [? [+ 01 :# :t] [00 %v] :? %0]

# Cons accessors

These retrieve the individual pieces of a cons cell. :! conses an object to nil if it is not already a cons.

    = ':! [? [] [:: []] :? %0]
    = ':h [%v :^]
    = ':t [%% 02 [00] :^]