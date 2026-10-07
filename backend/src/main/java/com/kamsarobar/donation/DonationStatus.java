package com.kamsarobar.donation;

public enum DonationStatus {
    /** Member says they transferred the money; waiting for the city admin to match it with the bank statement. */
    PENDING,
    /** City admin confirmed the money arrived - counts towards the collected total. */
    VERIFIED,
    /** City admin could not find the transfer. */
    REJECTED
}
