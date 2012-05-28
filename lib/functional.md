Canard standard library: function manipulation | Spencer Tipping
Licensed under the terms of the MIT source code license

# Composition

Functions can be composed by appending their list forms. . @o f g x is the same as f g x.

    = '@o [:+ @: ^1 [@:]]

# Objectification

Functions can be objectified (converted to anonymous list form) by using @ on any symbols that are present. Lists pass through unmodified, since they are already functions.

    = '@: [? [] [@] :? %0]

# Recursive map

Like the regular map function, but distributes over sub-lists recursively. This is primarily useful for editing functions in their list form.

    :** [f] :: x y -> :* [:** [f]] :: x y
    :** [f] _      -> f _

You can use it like this: :** [+ 1] [1 2 [3 4] 5], yielding [2 3 [4 5] 6].

    = ':** [? [:* :: :: [] ':**] [.] :? %1]