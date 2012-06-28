# Function continuation management

Normal functions will want to immediately stash their continuations onto the
return stack, then reload that continuation just prior to their last instruction
(which will be a tail call). So they will generally look like this:

    f [x y z]     -> f [x r> y z r<]              (defining f)
    g [x]         -> g [x]                        (defining g)
    h []          -> []                           (defining h)

The same is true of anonymous functions, including those inside conditionals:

    f [? [x r> y r<] [z r> t r<] r> g r<]

This is, quite frankly, miserable and hackish. What's worse is that we can't
realistically get rid of the inconvenience using metaprogramming, since
anonymous functions are indistinguishable from data lists. (Adding a marker
would be adding syntax, which is lame.)

# Underlying algebra

The apparent irregularity of the above r< and r> invocations is actually one
optimization removed from a very regular quasi-monadic form:

    f [x y z]     -> f [x r> y z r<]
                   = f [x r> y r< r> z r<]
                   = f [r> return r< r> x r< r> y r< r> z r<]