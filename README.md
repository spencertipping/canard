# Canard: a language for language developers
Canard is a concatenative, portable, JIT-compiled language modeled after an
awkward mixture of Joy, FORTH, and, library-wise, APL. Its compilation strategy
is evaluation-based and goes through the POSIX-specified `c99` compiler, which
means most code should run at native speed on any architecture despite having
zero runtime footprint. The downside is that won't run on Windows or other
non-POSIX operating systems.

## Syntax and basic usage
Canard is minimalistic in its syntax, with the exception that it gives you a
ton of ways to write strings of various types (just because it's such a downer
to work with languages that don't support this). Primitive syntactic elements
are:

- lists, written with square brackets and consed right to left
    - `[]` is nil
    - `[x y z]` is `[x y]` (the tail) consed with `z` (the head)
- symbols: `/[^]["\s][^][\s]*/`
- strings, written several different ways:
    - `"[...]` exactly like Perl's `q[...]` (and with any of Perl's allowed
      string delimiters), but written with `"` instead of `q`
    - `"<<EOF` starts a heredoc until `EOF`
    - `""$"[x $foo$ bar]`: starts a string preindexed with occurrences of `$`
    - `""foo"<<EOF`: starts a heredoc preindexed with occurrences of `foo`

Strings don't (and won't) know anything about encoding. They're just bytes.

There isn't a syntax for comments, but the standard library resolves `#x`
symbols to null lists and `#` to a function that drops its argument. This
allows you to structurally comment things under evaluation, and to write
shebang lines:

```
#!/this/shebang/is/a/nop
#this-will-disappear... [but this list will execute normally]
f #[this list will disappear] x
```

### Symbols and strings
Symbols and strings are interconvertible:

- `@< "[foo]` -> `'foo`
- `@> 'foo` -> `"[foo]`

The core library resolves `'foo` to a function that pushes the symbol `foo`.

### Preindexed strings
Interpolation is too high-level for canard to implement, but we can get most of
the benefit by preindexing a string around occurrences of a pattern. The result
is a string that can be efficiently used as a general-purpose template.
Implementation-wise preindexing is simple; every string has the following
primitive accessors:

- `n"`: string length in bytes
- `b"`: byte at given offset
- `n""`: number of index entries
- `i""`: byte offset of given index entry

Preindexed strings don't actually contain the substring being indexed; as a
result `""$"[foo $bar$ bif]` renders as `foo bar bif`, and the two indexed
points are at byte offsets 4 and 7.

## Execution semantics
Canard is a stack-based language whose state is encoded in three registers:

- `d`: a reference to the data stack cons
- `c`: a reference to the continuation stack cons
- `r`: a reference to the resolver stack cons

Assuming the notation `h(cons)` and `t(cons)` for head and tail, respectively,
the interpreter works like this:

```c
while (true)
{
  // c = [a b c d] -> c = [a b c], command = d
  command = h(c);
  c = t(c);

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
      *(native_functions[v])(&d, &c, &r);
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
