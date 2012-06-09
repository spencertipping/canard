Canard standard library: function manipulation | Spencer Tipping
Licensed under the terms of the MIT source code license

# Piecewise construction

This serves the purpose of something like 'where' in functional languages. The idea is to provide a series of local definitions that will be inlined into the form in question. This is done
first by defining a substitution function, then deriving a recursive variant from that.

    :/ symbol replacement -> [? :: replacement '%v [] $= 'symbol %0]
    %s $:                     symbol replacement                      = replacement 'symbol
    :: %s ''%v                replacement 'symbol                     = (:: replacement '%v) 'symbol
    ^1 [:: %s '%0 :: [[] $=]] (:: replacement '%v) 'symbol            = (:: replacement '%v) [[] $= 'symbol %0]
    :+ [?] :+ :: []           (:: replacement '%v) [[] $= 'symbol %0] = [? (:: replacement '%v) [] $= 'symbol %0]

The :/ function constructs a closure, but does not actually perform substitution. To do that, you would use :*, :~, or :**.

    = ':/ [:+ [?] :+ :: [] ^1 [:: %s '%0 :: [[] $=]] :: %s '%v %s $:]

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