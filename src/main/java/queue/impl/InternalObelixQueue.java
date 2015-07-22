package queue.impl;

import queue.interfaces.ObelixQueue;

import java.util.ArrayList;
import java.util.List;

public class InternalObelixQueue implements ObelixQueue {

    private List<ObelixQueueElement> queue;

    public InternalObelixQueue() {
        this.queue = new ArrayList<>();
    }

    @Override
    public final ObelixQueueElement pop() {
        if (this.queue.size() < 1) {
            return null;
        }
        ObelixQueueElement result = this.queue.get(0);
        this.queue.remove(0);
        return result;
    }

    @Override
    public final void push(final ObelixQueueElement element) {
        this.queue.add(element);
    }

    @Override
    public final List<ObelixQueueElement> getAll() {
        return queue;
    }

}
