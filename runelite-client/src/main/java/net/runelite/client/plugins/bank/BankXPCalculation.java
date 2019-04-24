package net.runelite.client.plugins.bank;

import com.google.common.collect.ImmutableList;
import lombok.Getter;
import net.runelite.api.*;
import net.runelite.client.game.ItemManager;

import javax.inject.Inject;
import java.util.*;

import static net.runelite.api.ItemID.*;

import static net.runelite.api.ItemID.COINS_995;
import static net.runelite.api.ItemID.PLATINUM_TOKEN;

public class BankXPCalculation {

    private static final ImmutableList<Varbits> TAB_VARBITS = ImmutableList.of(
            Varbits.BANK_TAB_ONE_COUNT,
            Varbits.BANK_TAB_TWO_COUNT,
            Varbits.BANK_TAB_THREE_COUNT,
            Varbits.BANK_TAB_FOUR_COUNT,
            Varbits.BANK_TAB_FIVE_COUNT,
            Varbits.BANK_TAB_SIX_COUNT,
            Varbits.BANK_TAB_SEVEN_COUNT,
            Varbits.BANK_TAB_EIGHT_COUNT,
            Varbits.BANK_TAB_NINE_COUNT
    );

    private final int[] herbs;
    private final int[] bones;

    //TODO: Create constants for each type of prayer multiplier (ecto, gilded, wildy, etc).
    private final double GILDED_ALTAR = 3.5; //Gilded Altar gives 350% xp.

    //Maps itemID to XP amount for creating potion with the herb.
    private final Map<Integer, Integer> herbList;
    private final Map<Integer, Integer> boneList;

    private final BankConfig config;
    private final ItemManager itemManager;
    private final Client client;

    // Used to avoid extra calculation if the bank has not changed
    private int itemsHash;

    @Inject
    BankXPCalculation(ItemManager itemManager, BankConfig config, Client client)
    {
        this.itemManager = itemManager;
        this.config = config;
        this.client = client;

        this.herbs = new int[14];
        this.bones = new int[12];
        this.herbList = new HashMap<Integer, Integer>();
        this.boneList = new HashMap<Integer, Integer>();
    }

    /**
     *
     * Below methods used to create the herb, grimyHerb, and unfPotion lists for now.
     * Will look into how to retrieve XP amounts without hard-coding later.
     *
     */

    void initHerbList(Map<Integer, Integer> herbList)
    {
        herbs[0] = GUAM_LEAF;
        herbs[1] = MARRENTILL;
        herbs[2] = TARROMIN;
        herbs[3] = HARRALANDER;
        herbs[4] = RANARR_WEED;
        herbs[5] = IRIT_LEAF;
        herbs[6] = AVANTOE;
        herbs[7] = KWUARM;
        herbs[8] = SNAPDRAGON;
        herbs[9] = CADANTINE;
        herbs[10] = LANTADYME;
        herbs[11] = DWARF_WEED;
        herbs[12] = TOADFLAX;
        herbs[13] = TORSTOL;

        herbList.put(ItemID.GUAM_LEAF, 25);
        herbList.put(ItemID.MARRENTILL, 37);
        herbList.put(ItemID.TARROMIN, 50);
        herbList.put(ItemID.HARRALANDER, 67);
        herbList.put(ItemID.RANARR_WEED, 87);
        herbList.put(ItemID.IRIT_LEAF, 100);
        herbList.put(ItemID.AVANTOE, 117);
        herbList.put(ItemID.KWUARM, 125);
        herbList.put(ItemID.SNAPDRAGON, 142);
        herbList.put(ItemID.CADANTINE, 150);
        herbList.put(ItemID.LANTADYME, 172);
        herbList.put(ItemID.DWARF_WEED, 162);
        herbList.put(ItemID.TOADFLAX, 180);
        herbList.put(ItemID.TORSTOL, 150);
    }

    void initBoneList(Map<Integer, Integer> boneList)
    {
        bones[0] = BIG_BONES;
        bones[1] = JOGRE_BONES;
        bones[2] = ZOGRE_BONES;
        bones[3] = BABYDRAGON_BONES;
        bones[4] = WYRM_BONES;
        bones[5] = DRAGON_BONES;
        bones[6] = WYVERN_BONES;
        bones[7] = DRAKE_BONES;
        bones[8] = LAVA_DRAGON_BONES;
        bones[9] = HYDRA_BONES;
        bones[10] = DAGANNOTH_BONES;
        bones[11] = SUPERIOR_DRAGON_BONES;

        boneList.put(BIG_BONES, 15);
        boneList.put(JOGRE_BONES, 15);
        boneList.put(ZOGRE_BONES, 22);
        boneList.put(BABYDRAGON_BONES, 30);
        boneList.put(WYRM_BONES, 50);
        boneList.put(DRAGON_BONES, 72);
        boneList.put(WYVERN_BONES, 72);
        boneList.put(DRAKE_BONES, 80);
        boneList.put(LAVA_DRAGON_BONES, 85);
        boneList.put(HYDRA_BONES, 110);
        boneList.put(DAGANNOTH_BONES, 125);
        boneList.put(SUPERIOR_DRAGON_BONES, 150);
    }

    Item[] getItems()
    {
        ItemContainer bankInventory = client.getItemContainer(InventoryID.BANK);

        if (bankInventory == null)
        {
            return null;
        }

        Item[] items = bankInventory.getItems();
        int currentTab = client.getVar(Varbits.CURRENT_BANK_TAB);

        if (currentTab > 0)
        {
            int startIndex = 0;

            for (int i = currentTab - 1; i > 0; i--)
            {
                startIndex += client.getVar(TAB_VARBITS.get(i - 1));
            }

            int itemCount = client.getVar(TAB_VARBITS.get(currentTab - 1));
            items = Arrays.copyOfRange(items, startIndex, startIndex + itemCount);
        }


        return items;
    }

    int calcHerbloreXp()
    {
        int xp = 0;
        Item[] items = getItems();
        Map<Integer, Integer> herbsInBank = new HashMap<Integer, Integer>(); //Maps itemID -> Quantity?
        initHerbList(this.herbList);

        if(items == null)
        {
            return xp;
        }

        for (Item item : items)
        {
            int quantity = item.getQuantity();

            if (item.getId() <= 0 || quantity == 0)
            {
                continue;
            }

            /**
             * Iterate through list of Items in the bank, if it's a herb, add the ID, quantity pair to the herbsInBank HashMap
             * which will be used to calculate total XP banked.
             */
            if(item.getId() == GUAM_LEAF || item.getId() == MARRENTILL || item.getId() == ItemID.TARROMIN || item.getId() == HARRALANDER ||
                    item.getId() == RANARR_WEED || item.getId() == IRIT_LEAF || item.getId() == AVANTOE || item.getId() == KWUARM || item.getId() == TOADFLAX ||
                    item.getId() == SNAPDRAGON || item.getId() == CADANTINE || item.getId() == LANTADYME || item.getId() == DWARF_WEED || item.getId() == TORSTOL)
            {
                herbsInBank.put(item.getId(), item.getQuantity());
            }
        }

        // Now do the calculations
        for(int i = 0; i < herbs.length; i++)
        {
            if(herbsInBank.get(herbs[i]) != null) {
                xp += (herbsInBank.get(herbs[i]) * herbList.get(herbs[i]));
            }
        }

        return xp;
    }

    /**
     * For now, calcPrayerXP will assume that the bones will be buried using a Gilded Altar.
     * In the future, we could add an option to specify Ecto, Gilded, Wildy, etc maybe have it passed as a parameter from BankConfig
     *
     * TODO: Add Ensouled Heads to total prayer xp.
     * TODO: Probably don't actually want to call initBoneList()/initHerbList() everytime the calc method is called...Move back to constructor?
     */
    int calcPrayerXp()
    {
        int xp = 0;
        double multiplier = GILDED_ALTAR;
        Item[] items = getItems();
        Map<Integer, Integer> bonesInBank = new HashMap<Integer, Integer>(); //Maps itemID -> Quantity?
        initBoneList(this.boneList);

        if(items == null)
        {
            return xp;
        }

        for (Item item : items)
        {
            int quantity = item.getQuantity();

            if (item.getId() <= 0 || quantity == 0)
            {
                continue;
            }

            /**
             * Iterate through list of Items in the bank, if it's a bone, add the (ID, quantity) pair to the bonesInBank HashMap
             * which will be used to calculate total XP banked.
             */
            if(item.getId() == BIG_BONES || item.getId() == JOGRE_BONES|| item.getId() == ItemID.ZOGRE_BONES || item.getId() == BABYDRAGON_BONES ||
                    item.getId() == WYRM_BONES || item.getId() == DRAGON_BONES || item.getId() == WYVERN_BONES || item.getId() == DRAKE_BONES || item.getId() == LAVA_DRAGON_BONES ||
                    item.getId() == HYDRA_BONES || item.getId() == DAGANNOTH_BONES || item.getId() == SUPERIOR_DRAGON_BONES )
            {
                bonesInBank.put(item.getId(), item.getQuantity());
            }
        }

        // Now do the calculations
        for(int i = 0; i < bones.length; i++)
        {
            if(bonesInBank.get(bones[i]) != null) {
                xp += (bonesInBank.get(bones[i]) * boneList.get(bones[i])) * multiplier;
            }
        }

        return xp;
    }

    private boolean isBankDifferent(Item[] items)
    {
        Map<Integer, Integer> mapCheck = new HashMap<>();

        for (Item item : items)
        {
            mapCheck.put(item.getId(), item.getQuantity());
        }

        int curHash = mapCheck.hashCode();

        if (curHash != itemsHash)
        {
            itemsHash = curHash;
            return true;
        }

        return false;
    }
}
