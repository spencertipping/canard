package canard;

import clojure.lang.Symbol;

public class ExecutableSymbol implements Fn {
  private Fn resolution = null;
  public final Symbol symbol;

  public ExecutableSymbol(final Symbol symbol) {
    this.symbol = symbol;
  }

  public Fn resolution(final Interpreter environment) {
    if (resolution == null)
      resolution = (Fn) environment.invoke(environment.resolver(), this.symbol);
    return resolution;
  }

  @Override public void apply(final Interpreter environment) {
    environment.rpush(resolution(environment));
  }

  @Override public String toString() {
    return symbol.toString();
  }
}
