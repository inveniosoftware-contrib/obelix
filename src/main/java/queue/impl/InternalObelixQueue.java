package queue.impl;

import queue.interfaces.ObelixQueue;

import java.util.ArrayList;
import java.util.List;

public class InternalObelixQueue implements ObelixQueue {

    private List<String> queue;

    public InternalObelixQueue() {
        this.queue = new ArrayList<>();
    }

    @Override
    public String pop() {
        String result = this.queue.get(0);
        this.queue.remove(0);
        return result;
    }

    @Override
    public void push(String element) {
        this.queue.add(element);
    }

    @Override
    public List<String> getAll() {
        return queue;
    }

}
