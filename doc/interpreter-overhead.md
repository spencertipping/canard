# Interpretation vs executable data structures

I'm mostly interested in cons cells here. If we represent a cons as a data
structure, then the logical model would be something like this:

    struct cons {
      uint64_t head;
      uint64_t tail;
    };

The interpreter for this data structure would encode the call-vs-jmp logic that
would otherwise be present:

    interpret_cons:       <- %rax points to the cons cell
      call *(%rax)
      jmp *8(%rax)

Conveniently, there isn't much overhead imposed here, and cons cell unpacking is
quite straightforward:

    unpack_cons:          <- %rax points to the cons cell
      movaps (%rax), %xmm0
      lea -16(%rsp), %rsp
      movaps %xmm0, (%rsp)

Note that I haven't yet specified how cons cells are stored. That's an open
question if data structures are non-executable, and arguably it should be
encoded somehow.

# Executable data structures

The encoding here is different. A list would have one of these three forms:

    1. e8 <32-bit relative head ptr> e9 <32-bit relative tail ptr>
    2. e9 <32-bit relative head ptr>
    3. c3

Case (1) is a nontrivial cons; case (2) is x :: nil, and case (3) is just nil.

Some sort of padding is probably necessary, though that could go at the end.
Cons cells need not have any particular alignment for the execution case.
However, loading unaligned values is expensive, so arguably the 32-bit relative
addresses should be aligned. Doing this yields something like this:

    <3-byte nop> e8 <32-bit head ptr> <3-byte nop> e9 <32-bit tail ptr>

The unpacking function is a little more complex than it is for interpreted
lists:

    unpack_cons:
      movq (%rax), %rbx
      lea 5(%rax), %rcx           <- assuming no padding
      testb %bl, $0xe8
      addl 1(%rax), %ecx
      jne unpack_trivial_cons
      lea 10(%rax), %rdx
      addl 6(%rax), %edx
      push %rdx
      push %rcx
      jmp end
    unpack_trivial_cons:
      xorq %rdx, %rdx
      push %rdx
      push %rcx

There is probably a more efficient way to write this, but there are certainly
more instructions this way. (There could hardly not be; the first representation
was just logical data.)

# Cache utilization

It's probably safe to assume that the implementation will either be CPU-bound or
cache-bound, and as applications become larger it is likely to be the latter.
Anything that reduces the code size is probably worth it, within reason.