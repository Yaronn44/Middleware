package com.alma.pay2bid.client.observable;

import com.alma.pay2bid.client.observer.IBidSoldObserver;

/**
 * An observable that notifies its observers when an item has been sold
 * @author Alexis Giraudet
 * @author Arnaud Grall
 * @author Thomas Minier
 */
public interface IBidSoldObservable {
    boolean addBidSoldObserver(IBidSoldObserver observer);

    boolean removeBidSoldObserver(IBidSoldObserver observer);
}
