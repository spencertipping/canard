package canard;

import clojure.lang.Symbol;

public class ExecutableSymbol implements Fn {
  private Fn resolution = null;
  public final Symbol symbol;

  public ExecutableSymbol(final Symbol symbol) {
    this.symbol = symbol;
  }

  public Fn resolution() {
    if (resolution == null) {
      environment.push(this.symbol);
      environment.rpush(null);
      environment.resolver().apply(environment);
      resolution = environment.pop();
    }
    return resolution;
  }

  @Override public void apply(final Interpreter environment) {
    resolution().apply(environment);
  }
}
