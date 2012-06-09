Binary emitters | Spencer Tipping
Licensed under the terms of the MIT source code license

# Introduction

These are a series of functions that allow you to represent numbers in various ways, most importantly in hexadecimal and binary. Rather than doing anything particularly creative, I'm just
building up custom entries in the main symbol table. There are 256 for each of hexadecimal and binary:

    b00000000
    b00000001
    b00000010
    b00000011
    ...
    b11111111
    x00
    x01
    x02
    ...
    xff

Each of these just pushes the given number onto the stack; there is no runtime conversion.

# Numeric constant definitions

This is simple enough. For each number, we just cons up that number to a list, convert the number to each base, and assign the function.

    #* 256 [= $+ 'x #16 %s :: [] %0
            = $+ 'b #2  %s :: [] %0 %0]

# Base conversion

These functions convert decimal numbers to hex and binary, respectively. Each one operates on bytes and prepends zeroes to fill to eight bits.

    = '#16 [nb #* 2 [^1 [$+ %s $ $^ %s '0123456789abcdef & 15 >>> * 4] %% 2 [1 0 1]] %s ']
    = '#2  [nb #* 8 [^1 [$+ %s $ $^ %s '01               & 1  >>>]     %% 2 [1 0 1]] %s ']

    #b n -> nb #* b [block] n ""

    [block] i n s -> n ($+ %s ($ $^ %s 'alphabet & mask >>> i n) s)
    %% 2 [1 0 1]                         i n s   = n i n s
    ^1 [$+ $ $^ %s 'alphabet & mask >>>] n i n s = n ($+ %s ($ $^ %s 'alphabet & mask >>> i n) s)]