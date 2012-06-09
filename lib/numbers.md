Canard numeric library | Spencer Tipping
Licensed under the terms of the MIT source code license

# Introduction

This library implements some functions to make numeric manipulation more convenient. Functions provided include:

    1. Single-digit numeric aliases; e.g. 1 -> 01, 2 -> 02, ... 9 -> 09
    2. Single-digit numeric adjustments: +1 -> [+ 01], -1 -> [- 01], ... +9 -> [+ 09] (same for * and /)

    #* 0a [= #d %s :: [] $+ '0 #d %0  =#:prefix-op '+ %0  =#:prefix-op '* %0
                                      =#:prefix-op '- %0  =#:prefix-op '/ %0]

    = '=#:prefix-op [= $+ ^1 [#d ^1 [:: :: [] ^1 [$+ '0 #d]]] %% 02 [00 01 00 01]]

    =#:prefix-op prefix n -> = $+ prefix #d n :: :: [] prefix $+ '0 n
    %% 2 [0 1 0 1]  prefix n                         = prefix n prefix n
    %^ 3 [$+ '0 #d] prefix n prefix n                = prefix n prefix $+ '0 #d n
    ^2 [:: :: []]   prefix n prefix $+ '0 n          = prefix n :: :: [] prefix $+ '0 #d n
    = $+ ^1 [#d]    prefix n :: :: [] prefix $+ '0 n = = $+ prefix #d n :: :: [] prefix $+ '0 #d n

    = '#d [$ + 30]