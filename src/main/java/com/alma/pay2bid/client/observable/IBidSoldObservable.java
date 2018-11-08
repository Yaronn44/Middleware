package com.alma.pay2bid.client.observable;

import com.alma.pay2bid.client.observer.IBidSoldObserver;

import java.util.UUID;

/**
 * An observable that notifies its observers when an item has been sold
 * @author Alexis Giraudet
 * @author Arnaud Grall
 * @author Thomas Minier
 */
public interface IBidSoldObservable {
    void addBidSoldObserver(UUID auctionId, IBidSoldObserver observer);

    void removeBidSoldObserver(UUID auctionId);
}
