package org.example.item;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import java.util.function.Supplier;
import com.google.common.base.Suppliers;

public enum MHTiers implements Tier {
   WHITE(4, 2500, 10.0F, 5.0F, 18, () -> Ingredient.of(Items.IRON_INGOT)),
   PURPLE(5, 4000, 12.0F, 6.0F, 22, () -> Ingredient.of(Items.AMETHYST_SHARD)),
   BLACK(6, 5200, 13.5F, 7.0F, 26, () -> Ingredient.of(Items.NETHERITE_INGOT));

   private final int level;
   private final int uses;
   private final float speed;
   private final float damage;
   private final int enchantmentValue;
   private final Supplier<Ingredient> repairIngredient;

   MHTiers(int level, int uses, float speed, float damage, int enchantmentValue, Supplier<Ingredient> repairIngredient) {
      this.level = level;
      this.uses = uses;
      this.speed = speed;
      this.damage = damage;
      this.enchantmentValue = enchantmentValue;
      this.repairIngredient = Suppliers.memoize(repairIngredient::get);
   }

   @Override
   public int getUses() {
      return this.uses;
   }

   @Override
   public float getSpeed() {
      return this.speed;
   }

   @Override
   public float getAttackDamageBonus() {
      return this.damage;
   }

   @Override
   public int getLevel() {
      return this.level;
   }

   @Override
   public int getEnchantmentValue() {
      return this.enchantmentValue;
   }

   @Override
   public Ingredient getRepairIngredient() {
      return this.repairIngredient.get();
   }
}
