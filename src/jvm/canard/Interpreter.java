package canard;

public interface Interpreter extends Fn {
  void push(Object o);
  Object pop();
  Object at(int i);

  void rpush(Fn continuation);
  Fn rpop();

  void resolver(Fn resolver);
  Fn resolver();

  void execute(Fn f);
  Object invoke(Fn f, Object ... args);
}
