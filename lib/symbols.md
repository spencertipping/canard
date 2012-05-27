Canard standard library: symbol manipulation | Spencer Tipping
Licensed under the terms of the MIT source code license

# Introduction

This library provides a bunch of functions designed to help you rewrite lists of symbols. It depends on the list library.

# Search/replace

This is a simple piecewise function that preserves all symbols except for the one being sought. If that one is encountered, an alternative is returned:

    . s/ 'a 'b 'a -> 'b
    . s/ 'a 'b 'c -> 'c

This function builds a closure by consing up a list with wildcards. Here's the derivation:

    s/ 'a 'b -> [? ['b %v] [] $= 'a]
    :+ [?] :: [] :+ %s [%v] :: [] $: %s      a b           = [? ['b %v]] a
    :+ %^ 1 [:+ :+ [[] $=] %s [%0] :: [] $:] [? ['b %v]] a = [? ['b %v] [] $= 'a %0]

    = 's/ [:+ %^ 1 [:+ :+ [[] $=] %s [%0] :: [] $:] :+ [?] :: [] :+ %s [%v] :: [] $: %s]