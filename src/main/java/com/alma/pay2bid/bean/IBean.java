package com.alma.pay2bid.bean;

import java.io.Serializable;
import java.util.UUID;

/**
 * A Bean that can be exchanged between two machines through RMI
 * @author Alexis Giraudet
 * @author Arnaud Grall
 * @author Thomas Minier
 */
public interface IBean extends Serializable {
    UUID getUUID();
}
