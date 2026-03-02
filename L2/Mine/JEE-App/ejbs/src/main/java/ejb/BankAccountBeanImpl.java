package ejb;

import interfaces.BankAccountBeanRemote;
import java.io.Serializable;

public class BankAccountBeanImpl implements BankAccountBeanRemote, Serializable{
    private Integer availableAmount = 0;

    @Override
    public Boolean withdraw(Integer amount) {
        if (availableAmount >= amount) {
            availableAmount -= amount;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void deposit(Integer amount) {
        availableAmount += amount;
    }

    @Override
    public Integer getBalance() {
        return 0;
    }
}
