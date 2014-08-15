package lbaas.test.integration.util;

public class NextAction {
    public RequestForTest request;
    public ExpectedResponse response;
    public NextAction next;
    
    public NextAction setRequest(RequestForTest request) {
        this.request = request;
        return this;
    }
    public NextAction setResponse(ExpectedResponse response) {
        this.response = response;
        return this;
    }
    public NextAction setNext(NextAction next) {
        this.next = next;
        return this;
    }
    
}
