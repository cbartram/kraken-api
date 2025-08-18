package com.kraken.api.core;

import com.google.inject.Inject;
import com.kraken.api.Context;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;

public abstract class AbstractService {

    @Inject
    @Getter
    protected Client client;

    @Inject
    @Getter
    protected ClientThread clientThread;

    @Inject
    @Getter
    protected Context context;
}
