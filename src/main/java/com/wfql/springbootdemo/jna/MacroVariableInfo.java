package com.wfql.springbootdemo.jna;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MacroVariableInfo {

    private boolean success;
    private int variableNumber;
    private int value;
    private short decimalPlaces;
    private short errorCode;
    private String errorMessage;

    public double getActualValue() {
        if (!success) {
            return Double.NaN;
        }
        if (value == 0 && decimalPlaces == -1) {
            return Double.NaN;
        }
        return value * Math.pow(10, -decimalPlaces);
    }

    @Override
    public String toString() {
        if (success) {
            return "MacroVariableInfo{success=true, variableNumber=" + variableNumber +
                    ", value=" + value +
                    ", decimalPlaces=" + decimalPlaces +
                    ", actualValue=" + getActualValue() + "}";
        } else {
            return "MacroVariableInfo{success=false, errorCode=" + errorCode +
                    ", errorMessage='" + errorMessage + "'" +
                    '}';
        }
    }

}