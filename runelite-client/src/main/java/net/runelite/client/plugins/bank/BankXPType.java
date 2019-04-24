package net.runelite.client.plugins.bank;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum BankXPType
{
    HERBLORE("Herblore"),
    PRAYER("Prayer"),
    SMITHING("Smithing");

    private final String name;

    @Override
    public String toString()
    {
        return name;
    }
}
