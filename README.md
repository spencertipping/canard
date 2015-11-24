# Canard: a language for language developers
Canard is a concatenative, portable, JIT-compiled language modeled after an
awkward mixture of Joy, FORTH, and, library-wise, APL. Its compilation strategy
is evaluation-based and goes through the POSIX-specified `c99` compiler, which
means most code should run at native speed on any architecture despite having
zero runtime footprint. The downside is that won't run on Windows or other
non-POSIX operating systems.
