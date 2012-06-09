Canard iteration library | Spencer Tipping
Licensed under the terms of the MIT source code license

# Introduction

This library provides trivial functions to iterate over numeric quantities.

    = '#* [? [#* ^2 [.] %% 2 [0 1 1 0] - 1] [%% 2 []] %0]
       nb [#* n [f] x -> #* (n-1) [f] f (n-1) x
           %% 2 [0 1 1 0] - 1 n [f] x               = (n-1) [f] [f] (n-1) x
           #* ^2 [.]          (n-1) [f] [f] (n-1) x = #* (n-1) [f] f (n-1) x

           #* 0 [f] x -> x
           %% 2 [] 0 [f] x = x]