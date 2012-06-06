# Optimization

Optimization libraries will probably fall into one of two categories. High-level optimizers remove unnecessary operations from a function by detecting invariants. Low-level optimizers find
ways to encode various operations efficiently on the target processor.

Note that high-level optimizers often don't use the usual set of primitives. These are domain-specific languages that know about certain classes of optimizations.