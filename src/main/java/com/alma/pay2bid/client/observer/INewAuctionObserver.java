package com.alma.pay2bid.client.observer;

import com.alma.pay2bid.bean.AuctionBean;

/**
 * An observer notified when a new auction has begun
 * @author Alexis Giraudet
 * @author Arnaud Grall
 * @author Thomas Minier
 */
public interface INewAuctionObserver {
    void updateNewAuction(AuctionBean auction);
}
