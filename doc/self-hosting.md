# Self-hosting

The execution model as implemented in the bootstrap compiler is too high-level. Here's the definition of the evaluation function, assuming two functions r< and r> from Forth:

    . :: x y -> . @ y r< [. x]
    . []     -> . r>
    . _      -> _

    @ :: x y -> :: x y
    @ []     -> []
    @ s      -> D[s]