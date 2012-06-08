Canard iteration library | Spencer Tipping
Licensed under the terms of the MIT source code license

# Introduction

This library provides trivial functions to iterate over numeric quantities.

    = '#* [? [#* - 1 ^1 [^1 [.] %0]] [%% 2 []] %0]

      nb [#* n [f] x -> #* (n-1) [f] f x
          ^1 [^1 [.] %0] n [f] x   = n [f] f x
          #* - 1         n [f] f x = #* (n-1) [f] f x

          #* 0 [f] x -> x
          %% 2 [] 0 [f] x = x]