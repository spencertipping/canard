package canard;

abstract class Stuff {
  public static final boolean eq(final Object a, final Object b) {
    return (a == null) == (b == null) && (a == b || a.equals(b));
  }

  public static final String s(final Object o) {
    if (o == null) return "[]";
    else           return o.toString();
  }
}
