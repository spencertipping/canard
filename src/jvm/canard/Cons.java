package canard;

import clojure.lang.IPersistentMap;
import clojure.lang.ISeq;
import clojure.lang.ASeq;

public abstract class Cons extends ASeq implements Fn {
  static final class Cell extends Cons {
    public final Object head;
    public final ISeq tail;

    Cell(final Object head, final ISeq tail) {
      this.head = head;
      this.tail = tail;
    }

    @Override public Object first() {
      return head;
    }

    @Override public ISeq next() {
      return tail;
    }
  }

  public static final Cell cons(final Object head, final ISeq tail) {
    return new Cell(head, tail);
  }

  public abstract Object first();
  public abstract ISeq next();

  @Override public void apply(Interpreter environment) {
    environment.execute((Fn) first());
    environment.rpush((Fn) next());
  }

  public Cons withMeta(final IPersistentMap meta) {
    throw new UnsupportedOperationException("metadata not supported for canard.Cons");
  }
}
