# Canard: a language for language developers
Canard is a concatenative, portable, JIT-compiled language modeled after an
awkward mixture of Joy, FORTH, and, library-wise, APL. Its compilation strategy
is evaluation-based and goes through the POSIX-specified `c99` compiler, which
means most code should run at native speed on any architecture despite having
zero installation footprint. The downside is that won't run on Windows or other
non-POSIX operating systems.

## Syntax and basic usage
Canard is minimalistic in its syntax, with the exception that it gives you a
lot of ways to write strings of various types (just because it's such a downer
to work with languages that don't support the type of string you need).
Primitive syntactic elements are:

- lists, written with square brackets and consed right-associatively
    - `[]` is nil, which is the only false value
    - `[x y z]` is `[x y]` (the tail) consed with `z` (the head)
    - lists are self-quoting, exactly as in Joy
- symbols: `/[^]["\s][^][\s]*/`
- strings, which are just mutable blocks of memory:
    - `"X...X`: exactly equivalent to Perl's `qX...X` construct; all of the
      same delimiters are supported
    - `"#EOF`: equivalent to Perl's `<<'EOF'`, but syntax continues immediately
      after the heredoc's end marker; you can't continue syntax on the same
      line
    - strings are self-quoting

Symbols and strings are interconvertible:

- `@< "[foo]` -> `'foo`
- `@> 'foo` -> `"[foo]`

### Library-provided syntax
Libraries can provide syntax-like abstractions by implementing resolvers for
groups of symbols. The core library has a few such resolvers:

- unboxed 30-bit numbers, written in usual C notation
    - **warning:** numbers on the continuation stack are interpreted as native
      fn references and may cause segfaults or other undefined behavior
- quoted symbols: `'foo` pushes `foo` onto the data stack
- comments:
    - `# [commented stuff]`
    - `#a-commented-word [but this list is not commented]`
    - `#!/usr/bin/canard`
- named variables with destructuring: `[+ x y : [[x] y]]`

### Macros
Any function can modify its calling continuation, which enables you to write
macros. `:` uses this strategy. For example:

```
. [this is the continuation of f x y]
```

If you run the above, `f`'s topmost continuation will be the list `[this is the
continuation of]`, and `f` is at liberty to quote, transform, and replace its
continuation accordingly. Note that macros only work leftwards; syntax to the
right will be forgotten by the time `f` is called.

Macros may seem like they would slow things down, but the standard abstract
evaluator/JIT will flatten them just like it does normal function calls.
(**TODO:** convince myself that this is actually possible)

## Execution semantics
Canard is a stack-based language whose state is encoded in three registers:

- `d`: a reference to the data stack cons
- `c`: a reference to the continuation stack cons
- `r`: a reference to the resolver stack cons

Assuming the notation `h(cons)` and `t(cons)` for head and tail, respectively,
the interpreter works like this (JIT is done by adding new native functions):

```c
while (true)
{
  // c = [a b c d] -> c = [a b c], command = d
  // It's important to remove nils here because nil normally pushes itself onto
  // the data stack. As a continuation it should be a nop.
  for (command = 0; !command;)
  {
    command = h(c);
    c = t(c);
  }

  // The expected format of the continuation stack is a list of lists: then
  // it's just one cons operation to create a new call frame. But native
  // functions can atomically cons multiple items, as in the case of symbol
  // resolution below. Because of this, we support both unwrapped things
  // (symbols and numbers) and conses. If it's a cons, we automatically go
  // through its entries. The nil is native fn 0, which does nothing.
  //
  // c = [a b c], command = [d e] -> c = [a b c [d]], command = e
  if (type(command) == CONS)
  {
    c = cons(c, t(command));
    command = h(command);
  }

  // Execute the command
  v = value(command);
  switch (type(command))
  {
    case NUMBER:
      // A way to arbitrarily modify d, c, and r. This is the escape hatch for
      // basic stack functions and the set of primitives required to make the
      // language work. (See src/self.canard.sdoc for the full list)
      (*native_functions[v])(&d, &c, &r);
      break;

    case SYMBOL:
      // Push the symbol and schedule the resolver to be executed on the next
      // iteration. Note that resolvers exist in a stack, so only the head
      // (i.e. stack top) is used here.
      d = cons(d, v);
      c = cons(cons(c, EVAL_NATIVE_NUMBER), h(r));
      break;

    case CONS:
    case STRING:
      // Both of these are self-quoting, so just push onto the data stack and
      // continue.
      d = cons(d, v);
      break;
  }
}
```
