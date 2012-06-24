# Self-defining symbols

It's tempting to have symbols be self-defining; that is, no quotation form is
used during definitions:

    foo [bar]             <- if 'foo' is unbound, binds 'foo' to [bar]
    foo                   <- since 'foo' is now bound, evaluates to [bar]

The problem with this approach is that it makes analysis difficult, but it's
unclear how much of a problem this is considering that analysis is difficult
with the usual quote-define model as well.

# Caveat: Undetectable library collision

This is bad. Suppose you have two libraries that attempt to define the same
symbol. Then you'll end up executing code that looks something like this:

    x [definition 2]
    ...
    x [definition 1]

The problem here is that x [definition 2] will use the existing definition of x,
as if this had happened:

    definition 1 [definition 2]
    ...
    x [definition 1]

This happens because symbol definition syntax is identical to usage syntax, so
there is no opportunity to throw an error. I'm not convinced that this is a
problem, outside of making Canard irrepressibly hostile to collaboration.