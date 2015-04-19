package store.interfaces;

import store.impl.ObelixStoreElement;

public interface ObelixStore {

    void set(String key, ObelixStoreElement element);
    ObelixStoreElement get(String key);

}
