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
    return "'" + value.toString();
  }
}
