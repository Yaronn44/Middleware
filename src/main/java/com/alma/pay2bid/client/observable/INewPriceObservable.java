package com.alma.pay2bid.client.observable;

import com.alma.pay2bid.client.observer.INewPriceObserver;

/**
 * An observable that notifies its observers when an item has a new price
 * @author Alexis Giraudet
 * @author Arnaud Grall
 * @author Thomas Minier
 */
public interface INewPriceObservable {
    boolean addNewPriceObserver(INewPriceObserver observer);

    boolean removeNewPriceObserver(INewPriceObserver observer);
}
