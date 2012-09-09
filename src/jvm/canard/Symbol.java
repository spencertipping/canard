package canard;

public class Symbol implements Fn {
  private Fn resolution = null;
  private boolean resolved = false;
  public final String symbol;
  public final int hashCode;

  public Symbol(final String symbol) {
    this.symbol = symbol;
    this.hashCode = symbol.hashCode();
  }

  public Fn resolution(final Interpreter environment) {
    if (!resolved) {
      resolution = (Fn) environment.invoke(environment.resolver(), this);
      if (resolution == this) resolution = new Quote(this);
      resolved = true;
    }
    return resolution;
  }

  @Override public void apply(final Interpreter environment) {
    environment.rpush(resolution(environment));
  }

  @Override public String toString() {
    return symbol + (resolved ? "#" : "");
  }

  @Override public int hashCode() {
    return hashCode;
  }

  @Override public boolean equals(final Object o) {
    return o instanceof Symbol &&
           hashCode == ((Symbol) o).hashCode &&
           Stuff.eq(symbol, (((Symbol) o).symbol));
  }
}
