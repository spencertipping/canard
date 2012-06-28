# List ambiguity

Does tail call optimization make lists ambiguous? Consider:

    [x y z]               <- e8 zz e8 yy e9 xx
    cons [x y] :< [z]     <- e8 zz e9 AA ... AA: e8 yy e9 xx
    cons [[w x] y] <: [z] <- e8 zz e9 AA ... AA: e8 yy e9 BB
                                         ... BB: e8 xx e9 ww

The third case is identical to this:

    cons [w x y] <: [z]   <- e8 zz e9 AA ... AA: e8 yy e8 xx e9 ww

The only difference is prefused consing, but this is not a reliable indicator
since it depends on the ordering of potentially unrelated cons operations.

Therefore, we cannot encode tail calls as a reader optimization.