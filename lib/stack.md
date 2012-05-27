Canard standard library: stack functions | Spencer Tipping
Licensed under the terms of the MIT source code license

# Introduction

This library provides a bunch of stack functions that prevent you from having to use the long forms of %% and %^ for common situations. The functions provided here are:

    %0    dup x           = x x
    %1    nip x y         = y x y
    %2    nip2 x y z      = z x y z
    %#    nth n x1 ... xn = xn x1 ... xn

    %v    drop x y               = y
    %v#   dropn n x1 ... xn xn+1 = xn+1

    %s    swap x y = y x
    %r3   rot3 x y z = y z x
    %R3   Rot3 x y z = z x y

    ^1    stash [f] x y = x f y
    ^2    stash2 [f] x y z = x y f z

    = '%0  [%% 0 [0]]
    = '%1  [%% 0 [1]]     = '%s  [%% 2 [1 0]]
    = '%2  [%% 0 [2]]     = '%r3 [%% 3 [1 2 0]]
    = '%#  [%% 0 :: []]   = '%R3 [%% 3 [2 0 1]]

    = '%v  [%% 1 []]      = '^1  [%^ 1]
    = '%v# [%% %s []]     = '^2  [%^ 2]