package canard;

abstract class Stuff {
  public static final boolean eq(final Object a, final Object b) {
    return (a == null) == (b == null) && (a == b || a.equals(b));
  }
}
