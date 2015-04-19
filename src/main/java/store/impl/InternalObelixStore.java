package store.impl;

import org.json.JSONObject;
import store.interfaces.ObelixStore;

import java.util.HashMap;
import java.util.Map;

public class InternalObelixStore implements ObelixStore {

    private Map<String, JSONObject> store = new HashMap<>();

    @Override
    public void set(String key, ObelixStoreElement value) {
        this.store.put(key, value.data);
    }

    @Override
    public ObelixStoreElement get(String key) {
        return new ObelixStoreElement(this.store.get(key));
    }

}