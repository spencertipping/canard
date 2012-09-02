package canard;

public class DebugInterpreter extends BaseInterpreter {
  DebugInterpreter(final Fn resolver) {
    super(resolver);
  }

  @Override public void push(final Object value) {
    System.err.println("< " + value);
    super.push(value);
  }

  @Override public Object pop() {
    final Object result = super.pop();
    System.err.println("> " + result);
    return result;
  }

  @Override public void rpush(final Fn f) {
    System.err.println("r< " + f);
    super.rpush(f);
  }

  @Override public Fn rpop() {
    final Fn f = super.rpop();
    System.err.println("r> " + f);
    return f;
  }
}
