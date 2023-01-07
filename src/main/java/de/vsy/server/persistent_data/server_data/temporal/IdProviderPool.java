package de.vsy.server.persistent_data.server_data.temporal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.Map;
import java.util.Queue;

@JsonTypeName("idProviderPool")
public class IdProviderPool {

    private static final int STD_POOL_SIZE = 10;
    private final Map<IdType, Integer> idCounterMap;
    private final Map<IdType, Queue<Integer>> idPool;

    private IdProviderPool(Map<IdType, Integer> counterMap,
                           Map<IdType, Queue<Integer>> currentIdPool) {
        this.idCounterMap = counterMap;
        this.idPool = currentIdPool;
    }

    @JsonCreator
    public static IdProviderPool instantiate(
            @JsonProperty("idCounterMap") Map<IdType, Integer> readCounterMap,
            @JsonProperty("idPool") Map<IdType, Queue<Integer>> readIdPool) {
        return new IdProviderPool(readCounterMap, readIdPool);
    }

    @JsonIgnore
    public void returnUnusedId(IdType type, int returnedId) {
        var currentPool = this.idPool.get(type);

        if (!currentPool.contains(returnedId)) {
            currentPool.add(returnedId);
        }
    }

    @JsonIgnore
    public int getNextId(IdType forType) {
        var availableIds = this.idPool.get(forType);

        if (availableIds.isEmpty()) {
            var nextId = this.idCounterMap.get(forType);

            for (int poolCounter = 0; poolCounter < STD_POOL_SIZE; poolCounter++) {
                availableIds.add(nextId);
                nextId++;
            }
            this.idCounterMap.put(forType, nextId);
        }
        return availableIds.remove();
    }

    public Map<IdType, Integer> getIdCounterMap() {
        return this.idCounterMap;
    }

    public Map<IdType, Queue<Integer>> getIdPool() {
        return this.idPool;
    }
}
