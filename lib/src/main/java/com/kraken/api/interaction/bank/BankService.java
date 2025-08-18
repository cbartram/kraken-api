package com.kraken.api.interaction.bank;

import com.kraken.api.core.AbstractService;
import com.kraken.api.interaction.widget.WidgetService;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
public class BankService extends AbstractService {

    @Inject
    private WidgetService widgetService;

    /**
     * Checks whether the bank interface is open.
     *
     * @return {@code true} if the bank interface is open, {@code false} otherwise.
     */
    public boolean isOpen() {
        return widgetService.hasWidgetText("Rearrange mode", 12, 18, false);
    }
}
