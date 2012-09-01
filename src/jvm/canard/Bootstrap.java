package canard;

public class Bootstrap {
  // List functions
  public static final Fn cons = new Fn() {
      @Override public void apply(final Interpreter environment) {
        final Object head = environment.pop();
        final Object tail = environment.pop();
        environment.push(Cons.cons(head, (ISeq) tail));
      }
    };

  public static final Fn uncons = new Fn() {
      @Override public void apply(final Interpreter environment) {
        final Cons top = (Cons) environment.pop();
        environment.push(top.next());
        environment.push(top.first());
      }
    };

  public static final Fn iscons = new Fn() {
      @Override public void apply(final Interpreter environment) {
        final Object top = environment.pop();
        environment.push(top instanceof Cons ? top : null);
      }
    };

  public static final Fn quote = new Fn() {
      @Override public void apply(final Interpreter environment) {
        environment.push(new Quote(environment.pop()));
      }
    };

  public static final Fn isquote = new Fn() {
      @Override public void apply(final Interpreter environment) {
        final Object top = environment.pop();
        environment.push(top instanceof Quote ? top : null);
      }
    };

  public static final Fn apply = new Fn() {
      @Override public void apply(final Interpreter environment) {
        environment.rpush((Fn) environment.pop());
      }
    };

  // Conditionals
  public static final Fn ift = new Fn() {
      @Override public void apply(final Interpreter environment) {
        if (environment.pop() != null) environment.rpush((Fn) environment.pop());
        else                           environment.pop();
      }
    };

  public static final Fn ifte = new Fn() {
      @Override public void apply(final Interpreter environment) {
        final Object conditional = environment.pop();
        final Fn thenCase = (Fn) environment.pop();
        final Fn elseCase = (Fn) environment.pop();
        environment.rpush(conditional ? thenCase : elseCase);
      }
    };

  // Resolution
  public static final Fn resolver$set = new Fn() {
      @Override public void apply(final Interpreter environment) {
        environment.resolver((Fn) environment.pop());
      }
    };

  public static final Fn resolver$get = new Fn() {
      @Override public void apply(final Interpreter environment) {
        environment.push(environment.resolver());
      }
    };
}
