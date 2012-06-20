# Self-defining symbols

It's tempting to have symbols be self-defining; that is, no quotation form is
used during definitions:

    foo [bar]             <- if 'foo' is unbound, binds 'foo' to [bar]
    foo                   <- since 'foo' is now bound, evaluates to [bar]

The problem with this approach is that it makes analysis difficult, but it's
unclear how much of a problem this is considering that analysis is difficult
with the usual quote-define model as well.