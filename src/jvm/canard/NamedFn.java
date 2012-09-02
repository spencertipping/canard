package canard;

public abstract class NamedFn implements Fn {
  public final String name;

  public NamedFn(final String name) {
    this.name = name;
  }

  @Override public String toString() {
    return name;
  }
}
