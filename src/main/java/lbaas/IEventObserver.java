package lbaas;

public interface IEventObserver {
    public void setVersion(Long version);
    public void postAddEvent(String message);
    public void postDelEvent(String message);
}
