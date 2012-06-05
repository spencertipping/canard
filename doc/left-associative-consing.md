# Left-associative consing

The main reason I want to cons from the left has to do with notational familiarity. I find it more natural to write things like 'f 3 4' than '4 3 f', just because it looks more like
applicative notation (though it is fundamentally different). However, it creates a few problems for lower-level notation. In particular:

    a b c d -> (((nil :: a) :: b) :: c) :: d) -> e8 d e8 c e8 b e8 a c3

So ultimately, the only stage at which the cons tree is reversed is in the initial parse phase -- this is a little counterintuitive, but otherwise fine. Trees are probably also serialized in
reverse, so that the parser can be treated as a projection into list-space. For now, the reverse-consing notation is fine.