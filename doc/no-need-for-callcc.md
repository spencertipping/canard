# No need for callcc

r< and r> serve the same purpose that callcc normally would. The reason is
simple: every function call is a callcc operation, since the program is
implicitly CPS-converted by return jumping. This is more intuitive in
concatenative than in applicative style (though it's also true in the latter: a
function call involves consuming and producing stack values and symmetrically
jumping into and out of the function).