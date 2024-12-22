package me.matsubara.listenmode.util;

import com.cryptomorin.xseries.reflection.XReflection;
import org.bukkit.*;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public final class ItemBuilder {

    private final ItemStack item;

    private static final MethodHandle SET_BASE_POTION_TYPE;

    static {
        SET_BASE_POTION_TYPE = XReflection.supports(20, 6) ?
                Reflection.getMethod(PotionMeta.class, "setBasePotionType", PotionType.class) :
                null;
    }

    public ItemBuilder(ItemStack item) {
        this.item = item;
    }

    public ItemBuilder(Material material) {
        item = new ItemStack(material);
    }

    public ItemBuilder(String material) {
        this(Material.valueOf(material));
    }

    public ItemBuilder setType(Material type) {
        item.setType(type);
        return this;
    }

    public ItemBuilder setAmount(int amount) {
        item.setAmount(amount);
        return this;
    }

    public ItemBuilder setDamage(int damage) {
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable)) return this;

        ((Damageable) meta).setDamage(damage);
        item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder setOwningPlayer(UUID uuid) {
        return setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
    }

    public ItemBuilder setOwningPlayer(OfflinePlayer player) {
        if (!(item.getItemMeta() instanceof SkullMeta)) return this;

        try {
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            meta.setOwningPlayer(player);
            item.setItemMeta(meta);
        } catch (ClassCastException ignore) {
        }
        return this;
    }

    public ItemBuilder setDisplayName(String displayName) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return this;

        meta.setDisplayName(PluginUtils.translate(displayName));
        item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder setLore(String... lore) {
        setLore(Arrays.asList(lore));
        return this;
    }

    @SuppressWarnings("ConstantConditions")
    public ItemBuilder setLore(List<String> lore) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return this;

        List<String> finalLore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        finalLore.addAll(PluginUtils.translate(lore));

        meta.setLore(finalLore);
        item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder setLore(@NotNull List<String> lore, UnaryOperator<String> replace) {
        lore.replaceAll(replace);
        return setLore(lore);
    }

    public ItemBuilder setLeatherArmorMetaColor(Color color) {
        if (!(item.getItemMeta() instanceof LeatherArmorMeta)) return this;

        try {
            LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
            meta.setColor(color);
            item.setItemMeta(meta);
        } catch (ClassCastException ignore) {
        }
        return this;
    }

    public ItemBuilder addEnchantment(Enchantment enchantment, int level) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return this;

        meta.addEnchant(enchantment, level, true);
        item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder removeEnchantment(Enchantment enchantment) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return this;

        meta.removeEnchant(enchantment);
        item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder addItemFlags(ItemFlag... flags) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return this;

        meta.addItemFlags(flags);
        item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder removeItemFlags(ItemFlag... flags) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return this;

        meta.removeItemFlags(flags);
        item.setItemMeta(meta);
        return this;
    }

    @SuppressWarnings("deprecation")
    public ItemBuilder setBasePotionData(PotionType type) {
        if (!(item.getItemMeta() instanceof PotionMeta meta)) return this;

        if (SET_BASE_POTION_TYPE != null) {
            try {
                SET_BASE_POTION_TYPE.invoke(meta, type);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        } else {
            meta.setBasePotionData(new org.bukkit.potion.PotionData(type));
        }

        item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder setBannerColor(DyeColor color) {
        if (!(item.getItemMeta() instanceof BannerMeta)) return this;

        try {
            BannerMeta meta = (BannerMeta) item.getItemMeta();
            if (meta == null) return this;

            meta.addPattern(new Pattern(color, PatternType.BASE));
            item.setItemMeta(meta);
        } catch (ClassCastException ignore) {

        }
        return this;
    }

    public ItemBuilder replace(String target, String replace) {
        return replaceName(target, replace).replaceLore(target, replace);
    }

    public ItemBuilder replaceName(String target, String replace) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return this;

        if (meta.hasDisplayName()) {
            meta.setDisplayName(meta.getDisplayName().replace(target, replace));
        }

        item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder replaceLore(String target, String replace) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return this;

        if (meta.hasLore() && meta.getLore() != null) {
            meta.setLore(meta.getLore().stream().map(line -> line.replace(target, replace)).collect(Collectors.toList()));
        }

        item.setItemMeta(meta);
        return this;
    }

    public ItemStack build() {
        return item;
    }
}