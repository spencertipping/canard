package canard;

public class Interpreter implements Fn {
  public static final int DATA_STACK_DEPTH = 131072;
  public static final int RETURN_STACK_DEPTH = 65536;

  protected final Object[] dataStack;
  protected final Fn[] returnStack;
  protected int dataStackPointer = 0;
  protected int returnStackPointer = 0;
  protected Fn resolver;

  protected Interpreter(final Fn initialResolver) {
    dataStack = new Object[DATA_STACK_DEPTH];
    returnStack = new Fn[RETURN_STACK_DEPTH];
    resolver = initialResolver;
  }

  public static Interpreter bootstrap() {
    return new Interpreter(Bootstrap.loadedResolver());
  }

  public void push(final Object o) {
    dataStack[dataStackPointer++] = o;
  }

  public Object pop() {
    return dataStack[--dataStackPointer];
  }

  public Object at(final int i) {
    return dataStack[dataStackPointer - i - 1];
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
      if (next instanceof Cons) {
        final Cons c = (Cons) next;
        if (c.tail == null || c.tail instanceof Fn)
          rpush((Fn) c.tail);
        else
          throw new RuntimeException("cannot run improper list " + c);
        this.execute((Fn) c.head);
      } else if (next == null);         // Null = return
      else
        this.execute(next);
    }
  }

  public void execute(final Fn f) {
    if (f == null) push(null);
    else           f.apply(this);
  }

  public Object invoke(final Fn f, final Object ... args) {
    final Interpreter i = new Interpreter(resolver);
    for (final Object o : args) i.push(o);
    i.push(f);
    i.apply(i);
    resolver(i.resolver());
    return i.pop();
  }
}
