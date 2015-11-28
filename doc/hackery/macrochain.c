#define swap(x, y, ...) (y, x, __VA_ARGS__)
#define plus(x, y, ...) (x + y, __VA_ARGS__)
#define dup(x, ...)     (x, x, __VA_ARGS__)
#define eval(x)         x

plus plus dup (4, 5)
eval(eval(plus plus dup (4, 5)))

// arity errors:
#define _swap(x, y, ...) y, x, __VA_ARGS__
#define _plus(x, y, ...) x + y, __VA_ARGS__
#define _dup(x, ...)     x, x, __VA_ARGS__
_plus(_plus(_dup(4, 5)))
