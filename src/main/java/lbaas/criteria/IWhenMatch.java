package lbaas.criteria;

public interface IWhenMatch {

    public String getHeader(String header);

    public String getParam(String param);

    public boolean isNull();

}
