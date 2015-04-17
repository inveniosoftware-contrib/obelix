package queue.interfaces;


import java.util.List;

public interface ObelixQueue {

    String pop();
    void push(String element);
    List<String> getAll();

}
