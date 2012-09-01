package canard;

import clojure.lang.Symbol;

public class ExecutableSymbol implements Fn {
  private Fn resolution = null;
  public final Symbol symbol;

  public ExecutableSymbol(final Symbol symbol) {
    this.symbol = symbol;
  }

  public Fn resolution(final Interpreter environment) {
    if (resolution == null) {
      environment.push(this.symbol);
      environment.rpush(null);
      environment.execute(environment.resolver());
      resolution = (Fn) environment.pop();
    }
    return resolution;
  }

  @Override public void apply(final Interpreter environment) {
    environment.execute(resolution(environment));
  }
}
