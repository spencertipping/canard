Canard iteration library | Spencer Tipping
Licensed under the terms of the MIT source code license

# Introduction

This library provides trivial functions to iterate over numeric quantities.

    = '#* [? [#* ^2 [.] %2abab - 01] [%2] %0a]

    #* n [f] x -> #* (n-1) [f] f (n-1) x
    %2abab - 1 n [f] x               = (n-1) [f] [f] (n-1) x
    #* ^2 [.]  (n-1) [f] [f] (n-1) x = #* (n-1) [f] f (n-1) x

    #* 0 [f] x -> x
    %2 0 [f] x = x