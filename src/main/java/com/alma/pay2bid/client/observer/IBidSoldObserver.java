package com.alma.pay2bid.client.observer;

import com.alma.pay2bid.client.IClient;

/**
 * An observer notified when an item has been sold
 * @author Alexis Giraudet
 * @author Arnaud Grall
 * @author Thomas Minier
 */
public interface IBidSoldObserver {
    void updateBidSold(IClient client);
}
