package canard;

public interface Interpreter extends Fn {
  void push(Object o);
  Object pop();

  void rpush(Fn continuation);
  Fn rpop();

  void resolver(Fn resolver);
  Fn resolver();

  void execute(Fn f);
}
