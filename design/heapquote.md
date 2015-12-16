# Heap quoting
It should be quite easy to do this; all we need is a little virtual machine
that uses array-aliased state. C99 should optimize this. Then the instructions,
which can be concatenative as usual, can set registers that govern input/output
parameters. Erasure may be possible as well, depending on how the language is
expressed.

`argc` and `argv` can be placed at two predetermined locations within the state
array, so really we're providing a concatenative encoding for C primitives. I
think this should be fine, and in general we should try to mirror C as much as
possible. Then we define `::`, etc, in these terms.

Possible issues:

1. C probably can't optimize indirect accesses (i.e. those which involve
   register updates). This means addressing will need to be built into
   commands, most likely. That way they'll at least be optimized in the
   preprocessor version.
2. We need to make sure the representation provides some way to recursively
   address things like strings; i.e. the set of operations being executed needs
   to be expressed in terms of operations against an existing string.
3. Things don't just bottom out at the OS page level because that can't be
   abstracted. We need logical string values of some sort. This means maybe we
   need to build heap management libraries with some awareness of these
   constraints.

We do still need a "heap object" that exists at some well-defined location. We
also need some primitives to inspect and modify that heap object -- so it's a
register, most likely. Then `d`, `c`, and `r` are probably at heap offsets 0,
1, and 2 (or can this be abstracted?). We don't want heap primitives to rely on
any specifics of stack representation, although we could use the preprocessor
to simulate a primitive-level stack.

I've been assuming that heap allocation amounts to incrementing a pointer and
treating everything as a blob, which is fine but probably makes it difficult to
optimize. What if we assume that each allocation unit behaves like a string --
i.e. we're consing a string onto the heap? I think this helps from an
optimization perspective if not necessarily simplifying the design.

It's worth breaking down the assumptions that go into allocating memory. It's
possible that the heap allocation process itself is an abstract component,
distinct from the representation of objects within the heap. (Much like the
separation provided by C's `malloc`, actually.) So let's at least assume a
`malloc`-style interface for allocating memory, even if deallocation is done
generationally. The heap is itself an abstraction without knowing what it
contains.

Then we have the representation of things like cons cells, which are most
likely structures that follow a more general form. If we type-tag pointers:

1. We'll need to break into a subheap when we start using more data structures,
   since tag bits encroach on the address space. This might or might not be a
   good thing; it depends on the details of how JIT code is handled. (Actually,
   I suppose our JIT strategy implies generational GC to some degree.)
2. We get unboxed encoding for free, which is undeniably convenient.
3. We also get partially-polymorphic evaluation for free, which is very
   convenient.

We could easily implement a protocol in which objects are encoded as
type-tagged strings. Then the evaluation semantics are still defined, and the
objects themselves are allowed to be mutable. The defining characteristics of
cons cells are that they aren't mutable, and that the evaluator knows about
them. That seems appropriate, actually.

It does rule out any possibility of having unboxed objects, but with sufficient
mutability I'm not sure it's a big issue. Objects can easily establish a
protocol using meaningless references on the data stack, as long as they aren't
evaluated. Then JIT can happen automatically, since the addressed values are
opaque stack entries -- i.e. JIT is following its usual purpose by erasing
stack manipulation.

The fact that JIT can happen this way suggests something about the nature of
primitive operations that need to be supported. It's possible that we'd reduce
to something like SSA at this point and let C99 take care of the rest.

Can primitives assume stacks of some kind? Then addressing isn't a problem, and
everything works nicely with μ-canard.
