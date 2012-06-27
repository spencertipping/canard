# Frequency vs cost

Machine-coded lists are expensive to inspect but cheap to execute. Canard is
primarily about execution (I'll explain further in a moment), so it makes
perfect sense to use a machine encoding.

Why primarily about execution? Because most of the heavy lifting will not
involve lists at all. You don't generally use linked lists in
performance-critical situations. Instead, you allocate memory and use vectors or
some such. Most list-oriented code is probably something to do with
metaprogramming or non-performance-critical symbolic manipulation.

I think that means this issue is closed: executable data structures are a win
here. (The performance win comes from delegating dispatch to the processor's
decoder rather than using soft decoding, which would be necessary provided that
we also need RTTI and polymorphism.)