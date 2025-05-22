package com.example.group;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class ArrayList implements java.util.List<Main.MCQOption> {
    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean contains(Object o) {
        return false;
    }

    @Override
    public Iterator<Main.MCQOption> iterator() {
        return null;
    }

    @Override
    public Object[] toArray() {
        return new Object[0];
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return null;
    }

    @Override
    public boolean add(Main.MCQOption mcqOption) {
        return false;
    }

    @Override
    public boolean remove(Object o) {
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean addAll(Collection<? extends Main.MCQOption> c) {
        return false;
    }

    @Override
    public boolean addAll(int index, Collection<? extends Main.MCQOption> c) {
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return false;
    }

    @Override
    public void clear() {

    }

    @Override
    public Main.MCQOption get(int index) {
        return null;
    }

    @Override
    public Main.MCQOption set(int index, Main.MCQOption element) {
        return null;
    }

    @Override
    public void add(int index, Main.MCQOption element) {

    }

    @Override
    public Main.MCQOption remove(int index) {
        return null;
    }

    @Override
    public int indexOf(Object o) {
        return 0;
    }

    @Override
    public int lastIndexOf(Object o) {
        return 0;
    }

    @Override
    public ListIterator<Main.MCQOption> listIterator() {
        return null;
    }

    @Override
    public ListIterator<Main.MCQOption> listIterator(int index) {
        return null;
    }

    @Override
    public List<Main.MCQOption> subList(int fromIndex, int toIndex) {
        return List.of();
    }
}
