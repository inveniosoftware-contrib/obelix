package store.interfaces;

public interface ObelixStore {

    void set(String key, String value);
    String get(String key);

}
