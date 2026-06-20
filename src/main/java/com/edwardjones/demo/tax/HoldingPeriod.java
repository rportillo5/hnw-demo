package com.edwardjones.demo.tax;

/**
 * IRS holding period classification.
 * SHORT_TERM: held <= 365 days — taxed as ordinary income.
 * LONG_TERM: held > 365 days — taxed at preferential capital gains rates.
 *
 * Simplified for demo purposes — ignores special rules for inherited
 * assets, gifted assets, or wash-sale-adjusted holding periods.
 */
public enum HoldingPeriod {
    SHORT_TERM,
    LONG_TERM
}
