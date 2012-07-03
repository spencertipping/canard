# Prefused cons nop tagging

I claimed earlier that memory allocation should not use the cons heap because
cons cells are strictly by-value. But this isn't actually true; we can just
intersperse allocations onto the cons heap:

    cons [] x             %rsi ->                                   | e8 x ...
    allocate 8            %rsi ->                             | <8> | e8 x ...
    cons swap             %rsi -> | e8 05000000 | e9 08000000 | <8> | e8 x ...
                                        |             |          ^    ^
                                        +-------------+----------+    |
                                                      +---------------+

We don't want to use nop tagging here because cons cells ultimately should be
references. It would be difficult to express more than 64 bits of data in any
case; you'd have to refer to it, at which point you have what is logically a
reference anyway. (Giving the destination of the reference by-value semantics
would be misleading.)

So cons really is a way to compose functions, not to allocate arbitrary chunks
of memory.