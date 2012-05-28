# Self-hosting

The execution model as implemented in the bootstrap compiler is too high-level. Here's the definition of the evaluation function, assuming two functions r< and r> from Forth:

    . :: x y -> .i y r< [. x]   .i :: x y -> :: x y
    . []     -> . r>            .i []     -> []
    . _      -> _               .i _      -> . D[_]

This isn't quite right, but it is the right idea. The r< function doesn't make sense in Canard because of the explicitness of composition.