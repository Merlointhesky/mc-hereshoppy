package com.hereshoppy.hereshoppy.model;

import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    private double balance;
    private int shopXp;
    private int shopLevel;
    private double lifetimeEarned;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.balance = 0;
        this.shopXp = 0;
        this.shopLevel = 0;
        this.lifetimeEarned = 0;
    }

    public UUID getUuid() {
        return uuid;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public int getShopXp() {
        return shopXp;
    }

    public void setShopXp(int shopXp) {
        this.shopXp = shopXp;
    }

    public int getShopLevel() {
        return shopLevel;
    }

    public void setShopLevel(int shopLevel) {
        this.shopLevel = shopLevel;
    }

    public double getLifetimeEarned() {
        return lifetimeEarned;
    }

    public void setLifetimeEarned(double lifetimeEarned) {
        this.lifetimeEarned = lifetimeEarned;
    }

    public void addBalance(double amount) {
        this.balance += amount;
        if (amount > 0) {
            this.lifetimeEarned += amount;
        }
    }

    public void removeBalance(double amount) {
        this.balance -= amount;
    }

    public void addShopXp(int amount) {
        this.shopXp += amount;
    }
}
