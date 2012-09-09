package canard;

public final class Quote implements Fn {
  public final Object value;
  public Quote(final Object value) {
    this.value = value;
  }

  @Override
  public void apply(final Interpreter environment) {
    environment.push(value);
  }

  @Override
  public String toString() {
    return "'" + Stuff.s(value);
  }

  @Override
  public boolean equals(final Object o) {
    return o == this ||
           o instanceof Quote &&
           Stuff.eq(((Quote) o).value, value);
  }
}
