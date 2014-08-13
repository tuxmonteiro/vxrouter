/*
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
package lbaas.test.unit.assertj.custom;

import lbaas.Backend;
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

    public VirtualHostAssert hasSize(Integer size, boolean backendOk) {
        isNotNull();
        if (actual.getBackends(backendOk).size() != size) {
            failWithMessage("Expected size to be <%s> but was <%s>", size, actual.getBackends(backendOk).size());
        }
        return this;
    }

    public VirtualHostAssert containsBackend(String backendWithPort, boolean backendOk) {
        isNotNull();
        if (!actual.getBackends(backendOk).contains(new Backend(backendWithPort, null))) {
            failWithMessage("%s not found at %s", backendWithPort, actual.getVirtualhostName());
        }
        return this;
    }

    public VirtualHostAssert doesNotContainsBackend(String backendWithPort, boolean backendOk) {
        isNotNull();
        if (actual.getBackends(backendOk).contains(new Backend(backendWithPort, null))) {
            failWithMessage("%s found at %s", backendWithPort, actual.getVirtualhostName());
        }
        return this;
    }

    public VirtualHostAssert hasProperty(String property) {
        isNotNull();
        if (!actual.containsField(property)) {
            failWithMessage("%s haven't the %s property", actual.getVirtualhostName(), property);
        }
        return this;
    }

    public VirtualHostAssert haventProperty(String property) {
        isNotNull();
        if (!actual.containsField(property)) {
            failWithMessage("%s has the %s property", actual.getVirtualhostName(), property);
        }
        return this;
    }
}
