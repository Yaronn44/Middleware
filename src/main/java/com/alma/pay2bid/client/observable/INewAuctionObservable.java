package com.alma.pay2bid.client.observable;

import com.alma.pay2bid.client.observer.INewAuctionObserver;

/**
 * A observable that notifies its observers when a new auction has begun
 * @author Alexis Giraudet
 * @author Arnaud Grall
 * @author Thomas Minier
 */
public interface INewAuctionObservable {
    boolean addNewAuctionObserver(INewAuctionObserver observer);

    boolean removeNewAuctionObserver(INewAuctionObserver observer);
}
