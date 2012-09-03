package canard;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;

public class Bootstrap {
  // List functions
  public static final Fn cons = new NamedFn("::") {
      @Override public void apply(final Interpreter environment) {
        final Object tail = environment.pop();
        final Object head = environment.pop();
        environment.push(Cons.cons(head, tail));
      }
    };

  public static final Fn uncons = new NamedFn(":^") {
      @Override public void apply(final Interpreter environment) {
        final Cons top = (Cons) environment.pop();
        environment.push(top.head);
        environment.push(top.tail);
      }
    };

  public static final Fn iscons = new NamedFn(":?") {
      @Override public void apply(final Interpreter environment) {
        final Object top = environment.pop();
        environment.push(top instanceof Cons ? top : null);
      }
    };

  public static final Fn quote = new NamedFn("'") {
      @Override public void apply(final Interpreter environment) {
        environment.push(new Quote(environment.pop()));
      }
    };

  public static final Fn isquote = new NamedFn("'?") {
      @Override public void apply(final Interpreter environment) {
        final Object top = environment.pop();
        environment.push(top instanceof Quote ? top : null);
      }
    };

  public static final Fn apply = new NamedFn(".") {
      @Override public void apply(final Interpreter environment) {
        environment.rpush((Fn) environment.pop());
      }
    };

  public static final Fn isapplicable = new NamedFn(".?") {
      @Override public void apply(final Interpreter environment) {
        final Object v = environment.pop();
        environment.push(v instanceof Fn ? v : null);
      }
    };

  // Return stack manipulation
  public static final Fn rpop = new NamedFn("r>") {
      @Override public void apply(final Interpreter environment) {
        final Fn continuation = environment.rpop();
        environment.push(environment.rpop());
        environment.rpush(continuation);
      }
    };

  public static final Fn rpush = new NamedFn("r<") {
      @Override public void apply(final Interpreter environment) {
        final Fn continuation = environment.rpop();
        environment.rpush((Fn) environment.pop());
        environment.rpush(continuation);
      }
    };

  // Conditionals
  public static final Fn ifte = new NamedFn("?") {
      @Override public void apply(final Interpreter environment) {
        final Fn thenCase = (Fn) environment.pop();
        final Fn elseCase = (Fn) environment.pop();
        final Object conditional = environment.pop();
        environment.rpush(conditional != null ? thenCase : elseCase);
      }
    };

  public static final Fn eq = new NamedFn("=") {
      @Override public void apply(final Interpreter environment) {
        final Object v1 = environment.pop();
        final Object v2 = environment.pop();
        environment.push(v1 == v2 || v1 != null && v1.equals(v2) ? v1 : null);
      }
    };

  public static final Fn not = new NamedFn("!") {
      @Override public void apply(final Interpreter environment) {
        environment.push(environment.pop() == null ? new Quote(null) : null);
      }
    };

  // Resolution
  public static final Fn resolver$set = new NamedFn("@<") {
      @Override public void apply(final Interpreter environment) {
        environment.resolver((Fn) environment.pop());
      }
    };

  public static final Fn resolver$get = new NamedFn("@>") {
      @Override public void apply(final Interpreter environment) {
        environment.push(environment.resolver());
      }
    };

  public static final Fn coreResolver = new NamedFn("core-resolver") {
      private final Map<String, Fn> coreResolutionMap = new HashMap<String, Fn>();
      {
        coreResolutionMap.put("::", cons);
        coreResolutionMap.put(":^", uncons);
        coreResolutionMap.put(":?", iscons);
        coreResolutionMap.put("'", quote);
        coreResolutionMap.put("'?", isquote);
        coreResolutionMap.put("!", not);
        coreResolutionMap.put(".", apply);
        coreResolutionMap.put(".?", isapplicable);
        coreResolutionMap.put("r>", rpop);
        coreResolutionMap.put("r<", rpush);
        coreResolutionMap.put("?", ifte);
        coreResolutionMap.put("=", eq);
        coreResolutionMap.put("@>", resolver$get);
        coreResolutionMap.put("@<", resolver$set);
      }

      @Override public void apply(final Interpreter environment) {
        final String name = ((Symbol) environment.at(0)).symbol;
        if (coreResolutionMap.containsKey(name)) {
          environment.pop();
          environment.push(coreResolutionMap.get(name));
          environment.rpop();
        }
      }
    };

  public static final Fn literalResolver = new NamedFn("literal-resolver") {
      @Override public void apply(final Interpreter environment) {
        final String name = ((Symbol) environment.at(0)).symbol;
        if (name.charAt(0) == '\'') {
          environment.pop();
          environment.push(new Quote(new Symbol(name.substring(1))));
          environment.rpop();
        }
      }
    };

  public static final Fn jvmResolver = new NamedFn("jvm-resolver") {
      @Override public void apply(final Interpreter environment) {
        final String name = ((Symbol) environment.at(0)).symbol;
        if (name.charAt(0) == '#') {
          environment.pop();
          environment.push(new NamedFn(name) {
              @Override public void apply(final Interpreter environment) {
                final Class c = (Class) environment.pop();
                try {
                  environment.push(c.getDeclaredField(name.substring(1)));
                } catch (final NoSuchFieldException e) {
                  environment.push(null);
                } catch (final SecurityException e) {
                  environment.push(null);
                }
              }
            });
          environment.rpop();
        } else if (name.charAt(0) == '.') {
          environment.pop();
          environment.push(new NamedFn(name) {
              @Override public void apply(final Interpreter environment) {
                final Class c = (Class) environment.pop();
                final Object[] formalArray = Cons.toArray((Cons) environment.pop());
                final Class[] castedArray = Arrays.copyOf(formalArray, formalArray.length,
                                                          Class[].class);
                try {
                  final Method m = c.getDeclaredMethod(name.substring(1), castedArray);
                  environment.push(new NamedFn(m.toString()) {
                      @Override public void apply(final Interpreter environment) {
                        final Object instance = environment.pop();
                        final Object[] arguments = Cons.toArray((Cons) environment.pop());
                        try {
                          environment.push(m.invoke(instance, arguments));
                        } catch (final IllegalAccessException e) {
                          environment.push(null);
                        } catch (final InvocationTargetException e) {
                          environment.push(null);
                        }
                      }
                    });
                } catch (final NoSuchMethodException e) {
                  environment.push(null);
                } catch (final SecurityException e) {
                  environment.push(null);
                }
              }
            });
          environment.rpop();
        } else
          try {
            final Class c = Class.forName(name);
            environment.pop();
            environment.push(new Quote(c));
            environment.rpop();
          } catch (final ClassNotFoundException e) {}
      }
    };

  public static final Fn stackFnResolver = new NamedFn("stack-fn-resolver") {
      @Override public void apply(final Interpreter environment) {
        final String name = ((Symbol) environment.at(0)).symbol;
        final String hexDigits = "0123456789abcdef";
        if (name.charAt(0) == '%') {
          final int base = hexDigits.indexOf(name.charAt(1));
          final Object[] added = new Object[name.length() - 2];

          environment.pop();
          environment.push(new NamedFn(name) {
              @Override public void apply(final Interpreter environment) {
                int addedIndex = 0;
                for (int i = 2; i < name.length(); ++i) {
                  final int x = hexDigits.indexOf(name.charAt(i));
                  added[addedIndex++] = environment.at(x);
                }
                for (int i = 0; i < base; ++i) environment.pop();
                while (addedIndex > 0)
                  environment.push(added[--addedIndex]);
              }
            });
          environment.rpop();
        } else if (name.charAt(0) == '^') {
          final int n = hexDigits.indexOf(name.charAt(1));
          final Object[] stash = new Object[n];

          environment.pop();
          environment.push(new NamedFn(name) {
              @Override public void apply(final Interpreter environment) {
                final Fn f = (Fn) environment.pop();
                for (int i = 0; i < n; ++i) stash[i] = environment.pop();
                environment.push(f);
                environment.apply(environment);
                for (int i = n - 1; i >= 0; --i) environment.push(stash[i]);
              }
            });
          environment.rpop();
        }
      }
    };

  public static Fn loadedResolver() {
    return Cons.list(coreResolver, stackFnResolver, literalResolver, jvmResolver);
  }
}
