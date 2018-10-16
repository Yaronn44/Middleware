package com.alma.pay2bid.client.observer;

/**
 * An observer notified when a timer has been updated
 * @author Alexis Giraudet
 * @author Arnaud Grall
 * @author Thomas Minier
 */
public interface ITimerObserver {
    void updateTimer(String time);
}
