package canard;

public class BaseInterpreter implements Interpreter {
  public static final int DATA_STACK_DEPTH = 131072;
  public static final int RETURN_STACK_DEPTH = 65536;

  private final Object[] dataStack;
  private final Fn[] returnStack;
  private int dataStackPointer = 0;
  private int returnStackPointer = 0;
  private Fn resolver;

  private BaseInterpreter(final Object initialResolver) {
    dataStack = new Object[DATA_STACK_DEPTH];
    returnStack = new Fn[RETURN_STACK_DEPTH];
    resolver = initialResolver;
  }

  public static BaseInterpreter bootstrap() {
    return new BaseInterpreter(minimalResolver());
  }

  public void push(final Object o) {
    dataStack[dataStackPointer++] = o;
  }

  public Object pop() {
    return dataStack[--dataStackPointer];
  }

  public void rpush(final Fn continuation) {
    returnStack[returnStackPointer++] = continuation;
  }

  public Fn rpop() {
    return returnStack[--returnStackPointer];
  }

  public void resolver(final Fn resolver) {
    this.resolver = resolver;
  }

  public Fn resolver() {
    return resolver;
  }

  @Override
  public void apply(final Interpreter parent) {
    // An interpreter interprets the stack top.
    rpush((Fn) parent.pop());
    while (returnStackPointer != 0) {
      final Fn next = rpop();
      if (next != null) next.apply(this);
    }
  }

  public Object invoke(final Fn f, final Object ... args) {
    for (final Object o : args) push(o);
    f.apply(this);
    return pop();
  }
}
