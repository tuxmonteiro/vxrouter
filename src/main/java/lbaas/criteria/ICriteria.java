package lbaas.criteria;

import java.util.Map;

import org.vertx.java.core.logging.Logger;

public interface ICriteria<T> {

    public abstract ICriteria<T> setLog(Logger log);

    public abstract ICriteria<T> given(Map<String, T> map);

    public abstract ICriteria<T> when(Object param);

    public abstract T getResult();

}
