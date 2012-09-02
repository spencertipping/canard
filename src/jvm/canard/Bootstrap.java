package canard;

import clojure.lang.ISeq;
import clojure.lang.Symbol;

import java.util.Map;
import java.util.HashMap;

public class Bootstrap {
  // List functions
  public static final Fn cons = new NamedFn("::") {
      @Override public void apply(final Interpreter environment) {
        final Object head = environment.pop();
        final Object tail = environment.pop();
        environment.push(Cons.cons(head, (ISeq) tail));
      }
    };

  public static final Fn uncons = new NamedFn(":^") {
      @Override public void apply(final Interpreter environment) {
        final Cons top = (Cons) environment.pop();
        environment.push(top.next());
        environment.push(top.first());
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

  public static final Fn apply = new NamedFn(".?") {
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
        final Object conditional = environment.pop();
        final Fn thenCase = (Fn) environment.pop();
        final Fn elseCase = (Fn) environment.pop();
        environment.rpush(conditional != null ? thenCase : elseCase);
      }
    };

  public static final Fn eq = new NamedFn("=") {
      @Override public void apply(final Interpreter environment) {
        final Object v = environment.pop();
        environment.push(v.equals(environment.pop()) ? v : null);
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
      private final Map<Symbol, Fn> coreResolutionMap = new HashMap<Symbol, Fn>();
      {
        coreResolutionMap.put(Symbol.intern("canard", "::"), cons);
        coreResolutionMap.put(Symbol.intern("canard", ":^"), uncons);
        coreResolutionMap.put(Symbol.intern("canard", ":?"), iscons);
        coreResolutionMap.put(Symbol.intern("canard", "'"), quote);
        coreResolutionMap.put(Symbol.intern("canard", "'?"), isquote);
        coreResolutionMap.put(Symbol.intern("canard", "."), apply);
        coreResolutionMap.put(Symbol.intern("canard", ".?"), isapplicable);
        coreResolutionMap.put(Symbol.intern("canard", "r>"), rpop);
        coreResolutionMap.put(Symbol.intern("canard", "r<"), rpush);
        coreResolutionMap.put(Symbol.intern("canard", "?"), ifte);
        coreResolutionMap.put(Symbol.intern("canard", "="), eq);
        coreResolutionMap.put(Symbol.intern("canard", "@>"), resolver$get);
        coreResolutionMap.put(Symbol.intern("canard", "@<"), resolver$set);
      }

      @Override public void apply(final Interpreter environment) {
        final Symbol s = (Symbol) environment.pop();
        if (coreResolutionMap.containsKey(s)) {
          environment.push(coreResolutionMap.get(s));
          environment.rpop();
        } else
          environment.push(s);
      }
    };

  public static final Fn literalResolver = new NamedFn("literal-resolver") {
      @Override public void apply(final Interpreter environment) {
        final Symbol s = (Symbol) environment.pop();
        final String name = s.getName();
        if (name.charAt(0) == '\'') {
          environment.push(new Quote(Symbol.intern("canard", name.substring(1))));
          environment.rpop();
        } else if (name.charAt(0) == 'x') {
          environment.push(new Quote(Long.parseLong(name.substring(1), 16)));
          environment.rpop();
        } else if (name.charAt(0) == 'd') {
          environment.push(new Quote(Double.parseDouble(name.substring(1))));
          environment.rpop();
        } else
          environment.push(s);
      }
    };

  public static final Fn jvmResolver = new NamedFn("jvm-resolver") {
      @Override public void apply(final Interpreter environment) {
        final Symbol s = (Symbol) environment.pop();
        final String name = s.getName();
        try {
          environment.push(new Quote(Class.forName(name)));
          environment.rpop();
        } catch (final ClassNotFoundException e) {
          environment.push(s);
        }
      }
    };

  public static final Fn bailoutResolver = new NamedFn("bailout-resolver") {
      @Override public void apply(final Interpreter environment) {
        final Symbol s = (Symbol) environment.pop();
        final String name = s.getName();
        environment.push(new Quote(Symbol.intern("unresolved", name)));
        environment.rpop();
      }
    };

  public static final Fn stackFnResolver = new NamedFn("stack-fn-resolver") {
      @Override public void apply(final Interpreter environment) {
        final Symbol s = (Symbol) environment.pop();
        final String name = s.getName();
        final String hexDigits = "0123456789abcdef";
        if (name.charAt(0) == '%') {
          final int base = hexDigits.indexOf(name.charAt(1));
          final Object[] added = new Object[name.length() - 2];

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
        } else
          environment.push(s);
      }
    };

  public static Fn loadedResolver() {
    return Cons.list(coreResolver, stackFnResolver, literalResolver, jvmResolver, bailoutResolver);
  }
}
