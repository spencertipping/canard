#define _POSIX_C_SOURCE 200112L
#define _XOPEN_SOURCE   600
#define _ISOC99_SOURCE
#define _ISOC9X_SOURCE

typedef unsigned long ref;
struct cons { ref h; ref t; };
static ref d, c, r;
static unsigned char *h0, *hp;
#define type(v) ((v) >> 30)
#define val(v)  ((v) & 0x3fffffffl)
#define paste(x, y) x##y
#define hd_cons(t, h) h
#define tl_cons(t, h) t
#define hd(x) paste(hd_, x)
#define tl(x) paste(tl_, x)
#define _3a3a(h, t, ...)    (cons(t, h), __VA_ARGS__)
#define _5e3a(x, ...)       (h(x), t(x), __VA_ARGS__)
#define _2473(x, y, ...)    (y, x, __VA_ARGS__)
#define _2c73(x, ...)       (__VA_ARGS__)
#define _3a73(x, ...)       (x, x, __VA_ARGS__)
#define _7273(x, y, z, ...) (y, z, x, __VA_ARGS__)
#define _5273(x, y, z, ...) (z, x, y, __VA_ARGS__)
#define _2e2127(...) (1, __VA_ARGS__)
#define commit(dn, cn, rn, ...) \
  do { \
    ref const _d = (dn); \
    ref const _c = (cn); \
    ref const _r = (rn); \
    d = _d; \
    c = _c; \
    r = _r; \
    goto fetch_next; \
  } while (0)
#define mc(f) commit f(d, c, r)
#define ev(x) x
static inline ref h(ref const c) {return ((struct cons*)(h0 + val(c)))->h;}
static inline ref t(ref const c) {return ((struct cons*)(h0 + val(c)))->t;}
static inline ref cons(ref const tl, ref const hd)
{
  struct cons *r = (struct cons*) hp;
  hp += sizeof(struct cons);
  r->t = tl;
  r->h = hd;
  return (unsigned char*) r - h0 | 2l << 30;
}
int main(int argc, char **argv)
{
  ref n;
  void *const *const fns[]   = { &&_5d5b,&&_2e,&&_223a2e,&&_2473,&&_2c73,&&_3a3a,&&_3a3a2e,&&_3a73,&&_3c2e,&&_3c72,&&_3c73,&&_3e2e,&&_3e72,&&_3e73,&&_403a2e,&&_5273,&&_5e3a,&&_7273 };
  void *const *const cases[] = { &&_213a2e, &&_403a2e, &&_3a3a2e, &&_223a2e };
  /* TODO: bootup continuation/resolver/etc */
fetch_next:
  for (n = 0; !n;) n = h(c), c = t(c);
  if (type(n) == 2) c = cons(c, t(n)), n = h(n);
  goto *cases[type(n)];
_213a2e: goto *fns[n];
_5d5b: goto fetch_next;
_2e: ev(ev(ev(ev(ev(mc(_2473 _3a3a _2473 _5273 _5e3a))))));
_223a2e: ev(ev(ev(ev(ev(ev(ev(mc(_3a3a _7273 _3a3a _7273 _5e3a _5e3a _2473))))))));
_2473: ev(ev(ev(ev(ev(ev(ev(ev(ev(mc(_3a3a _2473 _3a3a _7273 _2473 _5273 _5e3a _2473 _5e3a))))))))));
_2c73: ev(ev(mc(_2c73 _5e3a)));
_3a3a: ev(ev(ev(ev(ev(ev(mc(_3a3a _3a3a _5273 _5e3a _2473 _5e3a)))))));
_3a3a2e: ev(ev(ev(ev(ev(ev(ev(mc(_3a3a _7273 _3a3a _7273 _5e3a _5e3a _2473))))))));
_3a73: ev(ev(ev(ev(ev(ev(mc(_3a3a _2473 _3a3a _7273 _3a73 _5e3a)))))));
_3c2e: ev(ev(ev(ev(mc(_2473 _2c73 _5273 _5e3a)))));
_3c72: ev(ev(ev(ev(mc(_7273 _5e3a _2c73 _5273)))));
_3c73: ev(ev(ev(mc(_2c73 _2473 _5e3a))));
_3e2e: ev(ev(ev(ev(mc(_3a3a _7273 _3a73 _2473)))));
_3e72: ev(ev(ev(ev(ev(ev(ev(mc(_5273 _2473 _7273 _3a3a _7273 _3a73 _5273))))))));
_3e73: ev(ev(mc(_3a3a _3a73)));
_403a2e: ev(ev(ev(ev(ev(ev(ev(ev(ev(ev(ev(ev(ev(ev(ev(ev(mc(_7273 _3a3a _2473 _5273 _5e3a _2473 _5273 _3a3a _7273 _3a3a _2e2127 _3a3a _7273 _5e3a _5e3a _2473)))))))))))))))));
_5273: ev(ev(ev(ev(ev(ev(ev(ev(ev(ev(ev(ev(mc(_3a3a _2473 _3a3a _5273 _3a3a _5273 _2473 _5e3a _2473 _5e3a _2473 _5e3a)))))))))))));
_5e3a: ev(ev(ev(ev(ev(ev(mc(_3a3a _2473 _3a3a _7273 _5e3a _5e3a)))))));
_7273: ev(ev(ev(ev(ev(ev(ev(ev(ev(ev(ev(ev(mc(_3a3a _2473 _3a3a _2473 _3a3a _2473 _7273 _5e3a _7273 _5e3a _2473 _5e3a)))))))))))));
}