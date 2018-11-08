package com.alma.pay2bid.client.observable;

import com.alma.pay2bid.client.observer.ITimerObserver;

import java.util.UUID;

/**
 * An observable that notifies its observers when a timer has been updated
 * @author Alexis Giraudet
 * @author Arnaud Grall
 * @author Thomas Minier
 */
public interface ITimerObservable  {
    void addTimerObserver(UUID auctionId, ITimerObserver observer);

    void removeTimerObserver(UUID auctionId);
}
