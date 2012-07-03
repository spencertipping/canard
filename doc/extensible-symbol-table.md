# Extensible symbol table

The symbol table is a linked list of partial functions, not of symbols verbatim.
Each partial function responds to a symbol and determines whether that symbol is
within its domain. If so, it returns a pointer to a definition that should be
used for that symbol; otherwise, it searches the rest of the symbol table.

Matcher functions can be generated from symbols easily enough; we just map each
byte into a string comparison function. For example, here's the string we want
to match:

    06000000 43 61 6e 61 72 64    <- six bytes long, string is 'Canard'

And here's the resulting code:

    dpop %rbx                     <- pop the string to be tested
    testl (%rbx), $0x00000006     <- check length
    jne nope
      testb 4(%rbx), $0x43        <- length is ok, check bytes
      jne nope                    <- lack of vectorization is lame ...
      testb 5(%rbx), $0x61        <- ... but probably acceptable
      jne nope
      testb 6(%rbx), $0x6e
      jne nope
      testb 7(%rbx), $0x61
      jne nope
      testb 8(%rbx), $0x72
      jne nope
      testb 9(%rbx), $0x64
      jne nope
      dpush $0xXXXXXXXX           <- match was successful; push the binding
    ret
    nope:
      dpush $0                    <- match failed; push 0
    ret

However, this model generalizes nicely to non-matcher functions. For example, we
could write a function that supported wildcard characters (denoted here by .) by
simply skipping some byte comparisons. So you could write a matcher for
hexadecimal bytes:

    0x..

The first two characters are literal, the last two are wildcards that accept any
values. The length must still match for this symbol pattern to be used.

Given this pattern, bootstrapping Canard involves constructing the image of a
symbol table with enough stuff to be useful.