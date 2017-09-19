package com.idevicesinc.sweetblue.utils;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

public class StablePriorityQueue<E>
{
   private static class Node<E>
    {
        E mObj;
        long mInsertOrder;

        Node(E obj, long insertOrder)
        {
            mObj = obj;
            mInsertOrder = insertOrder;
        }

        @Override
        public boolean equals(Object other)
        {
            if (other instanceof Node)
            {
                return mObj.equals(((Node)other).mObj);
            }
            if (other != null)
            {
                return other.equals(mObj);
            }

            return false;
        }
    }

    protected long mInsertCounter = 0L;

    protected PriorityQueue<Node<E>> mPQ = null;

    protected Comparator<Node<E>> makeComparator(Comparator<? super E> baseComparator)
    {
        Comparator<Node<E>> cmp = (o1, o2) ->
        {
            int compareResult;

            if (baseComparator != null)
                compareResult = baseComparator.compare(o1.mObj, o2.mObj);
            else
            {
                Comparable<E> cmp1 = (Comparable<E>)o1.mObj;
                compareResult = cmp1.compareTo(o2.mObj);
            }

            // Break ties with the insert order
            if (compareResult == 0)
            {
                long tieBreaker = o1.mInsertOrder - o2.mInsertOrder;
                if (tieBreaker < 0)
                    compareResult = -1;
                else if (tieBreaker > 0)
                    compareResult = 1;
            }

            return compareResult;
        };

        return cmp;
    }

    protected long getNextInsertCounter()
    {
        return mInsertCounter++;
    }

    public StablePriorityQueue()
    {
        this(11);
    }

    public StablePriorityQueue(int initialCapacity)
    {
        mPQ = new PriorityQueue<>(initialCapacity, makeComparator(null));
    }

    public StablePriorityQueue(int initialCapacity, Comparator<? super E> comparator)
    {
        mPQ = new PriorityQueue<>(initialCapacity, makeComparator(comparator));
    }

    public StablePriorityQueue(Collection<? extends E> c)
    {
        this();

        if (c == null)
            throw new NullPointerException();

        // Move everything from the collection into the PQ
        for (E obj : c)
        {
            if (obj == null)
                throw new NullPointerException();
            Node n = new Node(obj, getNextInsertCounter());
            mPQ.add(n);
        }
    }

    public boolean add(E e)
    {
        if (e == null)
            throw new NullPointerException();
        return mPQ.add(new Node(e, getNextInsertCounter()));
    }

    public boolean offer(E e)
    {
        if (e == null)
            throw new NullPointerException();
        return mPQ.offer(new Node(e, getNextInsertCounter()));
    }

    public E peek()
    {
        Node<E> n = mPQ.peek();
        return n != null ? n.mObj : null;
    }

    public boolean remove(Object o)
    {
        if (o == null)
            return false;

        Iterator<Node<E>> it = mPQ.iterator();

        boolean removed = false;
        while (it.hasNext())
        {
            Node<E> n = it.next();
            if (o.equals(n.mObj))
            {
                it.remove();
                removed = true;
            }
        }

        return removed;
    }

    public boolean contains(Object o)
    {
        if (o == null)
            return false;

        Iterator<Node<E>> it = mPQ.iterator();

        while (it.hasNext())
        {
            Node<E> n = it.next();
            if (o.equals(n.mObj))
                return true;
        }

        return false;
    }

    public int size()
    {
        return mPQ.size();
    }

    public void clear()
    {
        mPQ.clear();
    }

    public E poll()
    {
        Node<E> n = mPQ.poll();
        return n != null ? n.mObj : null;
    }
}
