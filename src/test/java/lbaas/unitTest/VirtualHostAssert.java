package lbaas.unitTest;

import lbaas.Client;
import lbaas.Virtualhost;

import org.assertj.core.api.AbstractAssert;

public class VirtualHostAssert extends AbstractAssert<VirtualHostAssert, Virtualhost> {

    protected VirtualHostAssert(Virtualhost actual) {
        super(actual, VirtualHostAssert.class);
    }

    public static VirtualHostAssert assertThat(Virtualhost actual) {
        return new VirtualHostAssert(actual);
    }

    public VirtualHostAssert hasActionFail(boolean actionOk) {
        isNotNull();
        if (actionOk) {
            failWithMessage("Action not fail.", "");
        }
        return this;
    }

    public VirtualHostAssert hasActionOk(boolean actionOk) {
        isNotNull();
        if (!actionOk) {
            failWithMessage("Action fail.", "");
        }
        return this;
    }

    public VirtualHostAssert hasSize(Integer size, boolean endPointOk) {
        isNotNull();
        if (actual.getClients(endPointOk).size() != size) {
            failWithMessage("Expected size to be <%s> but was <%s>", size, actual.getClients(endPointOk).size());
        }
        return this;
    }

    public VirtualHostAssert containsReal(String realWithPort, boolean endPointOk) {
        isNotNull();
        if (!actual.getClients(endPointOk).contains(new Client(realWithPort, null))) {
            failWithMessage("%s not found at %s", realWithPort, actual.getVirtualhostName());
        }
        return this;
    }

    public VirtualHostAssert doesNotContainsReal(String realWithPort, boolean endPointOk) {
        isNotNull();
        if (actual.getClients(endPointOk).contains(new Client(realWithPort, null))) {
            failWithMessage("%s found at %s", realWithPort, actual.getVirtualhostName());
        }
        return this;
    }
}
