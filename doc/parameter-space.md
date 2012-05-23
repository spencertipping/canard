Unary continuations.
One-to-one mapping of values to values; functions are guaranteed to be unary. No ability to delete values or return multiples directly. (Especially with currying.)

Stack-based concatenative.
Tacit programming focuses on stack top. The stack can be virtualized. Continuations have explicit arity and can be implemented using immutable conses for stack cells. Values that are not
literals are anonymous. Many-to-many mapping of values to values, but locality is constrained by stack variability.

The stack has a single focus point, and consuming a value yields an additional unique focus point. So tacit programming is possible at the expense of some shuffling. The linguistic tradeoff is
stack swapping to avoid explicit value specification. (Why not just use local variables to solve this problem?)
