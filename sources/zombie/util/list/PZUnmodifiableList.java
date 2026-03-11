/*
 * Decompiled with CFR 0.152.
 */
package zombie.util.list;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import zombie.UsedFromLua;
import zombie.util.list.PZUnmodifiableCollection;

@UsedFromLua
public class PZUnmodifiableList<E>
extends PZUnmodifiableCollection<E>
implements List<E> {
    final List<? extends E> list;

    public static <T> List<T> wrap(List<? extends T> list) {
        if (list.getClass() == PZUnmodifiableList.class) {
            return list;
        }
        return new PZUnmodifiableList<T>(list);
    }

    PZUnmodifiableList(List<? extends E> list) {
        super(list);
        this.list = list;
    }

    @Override
    public boolean equals(Object o) {
        return o == this || this.list.equals(o);
    }

    @Override
    public int hashCode() {
        return this.list.hashCode();
    }

    @Override
    public E get(int index) {
        return this.list.get(index);
    }

    @Override
    public E set(int index, E element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(int index, E element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public E remove(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int indexOf(Object o) {
        return this.list.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return this.list.lastIndexOf(o);
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void replaceAll(UnaryOperator<E> operator) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sort(Comparator<? super E> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListIterator<E> listIterator() {
        return this.listIterator(0);
    }

    @Override
    public ListIterator<E> listIterator(final int index) {
        return new ListIterator<E>(this){
            private final ListIterator<? extends E> i;
            final /* synthetic */ PZUnmodifiableList this$0;
            {
                PZUnmodifiableList pZUnmodifiableList = this$0;
                Objects.requireNonNull(pZUnmodifiableList);
                this.this$0 = pZUnmodifiableList;
                this.i = this.this$0.list.listIterator(index);
            }

            @Override
            public boolean hasNext() {
                return this.i.hasNext();
            }

            @Override
            public E next() {
                return this.i.next();
            }

            @Override
            public boolean hasPrevious() {
                return this.i.hasPrevious();
            }

            @Override
            public E previous() {
                return this.i.previous();
            }

            @Override
            public int nextIndex() {
                return this.i.nextIndex();
            }

            @Override
            public int previousIndex() {
                return this.i.previousIndex();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void set(E e) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void add(E e) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void forEachRemaining(Consumer<? super E> action) {
                this.i.forEachRemaining(action);
            }
        };
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        return new PZUnmodifiableList<E>(this.list.subList(fromIndex, toIndex));
    }
}

