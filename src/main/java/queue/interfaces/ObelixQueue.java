package queue.interfaces;


import queue.impl.ObelixQueueElement;

import java.util.List;

public interface ObelixQueue {

    ObelixQueueElement pop();
    void push(ObelixQueueElement element);
    List<ObelixQueueElement> getAll();

}
