Canard standard library: comments | Spencer Tipping
Licensed under the terms of the MIT source code license

# Introduction

This library allows you to use nb[] to write comments. You should include it before including anything else. It also enables shebang-line syntax if you elect to use /usr/bin/env. If you want
to refer to a specific canard interpreter with an absolute path, you'll need to define the interpreter:

    =interpreter '#!/usr/bin/my-canard

    = '=interpreter [= %2ba []]
    = '#!/usr/bin/env [nb]
    = 'nb [%1]