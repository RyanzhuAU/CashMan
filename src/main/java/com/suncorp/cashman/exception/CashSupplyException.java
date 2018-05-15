
package com.suncorp.cashman.exception;

/**
 * Exception thrown when the CashMachine cannot supply the required amountRequired.
 *
 * Created by ryan.zhu on 13/05/2018.
 *
 */

public class CashSupplyException extends Exception {

    /** The amount required to be withdrawn. */
    private int amountRequired;

    /** The amount that can actually be supplied. */
    private int amountSupplied;

    public CashSupplyException(String errorMessage) {
        super(errorMessage);
    }

    public CashSupplyException(int amountRequired) {
        super(createMessage(amountRequired));
        this.amountRequired = amountRequired;
    }

    public CashSupplyException(Throwable throwable, int amountRequired) {
        super(createMessage(amountRequired), throwable);
        this.amountRequired = amountRequired;
    }

    public CashSupplyException(int amountRequired, int amountSupplied, boolean overLimit) {
        super(createMessage(amountRequired, amountSupplied, overLimit));
        this.amountRequired = amountRequired;
        this.amountSupplied = amountSupplied;
    }

    public CashSupplyException(Throwable throwable, int amountRequired, int amountSupplied) {
        super(createMessage(amountRequired, amountSupplied, false), throwable);
        this.amountRequired = amountRequired;
        this.amountSupplied = amountSupplied;
    }

    public int getAmountRequired() {
        return amountRequired;
    }

    public int getAmountSupplied() {
        return amountSupplied;
    }

    private static String createMessage(int amountRequired) {
        return "Sorry, this ATM cannot supply the amount required $" + amountRequired + " with current stock. Please try again later.";
    }

    private static String createMessage(int amountRequired, int amountSupplied, boolean overLimit) {
        if (overLimit) {
            return "Sorry, the amount $" + amountRequired + " is over your withdraw limitation. The amount you can withdraw is $" + amountSupplied + " today.";
        } else {
            return "Sorry, this ATM cannot supply the amount required $" + amountRequired + " with current stock. " +
                    "The closest amount that can be supplied is $" + amountSupplied + ". Please try again later.";
        }
    }
}