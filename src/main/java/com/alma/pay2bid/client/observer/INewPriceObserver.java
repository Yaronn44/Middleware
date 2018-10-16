package com.alma.pay2bid.client.observer;

import java.util.UUID;

/**
 * An observer notified when an item has a new price
 * @author Alexis Giraudet
 * @author Arnaud Grall
 * @author Thomas Minier
 */
public interface INewPriceObserver {
    void updateNewPrice(UUID auctionID, Integer price);
}
