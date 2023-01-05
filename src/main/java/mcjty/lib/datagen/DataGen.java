package mcjty.lib.datagen;

import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.data.event.GatherDataEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class DataGen {

    private final String modid;
    private final GatherDataEvent event;
    private final List<Dob> dobs = new ArrayList<>();

    public DataGen(String modid, GatherDataEvent event) {
        this.modid = modid;
        this.event = event;
    }

    public void add(Dob.Builder... builder) {
        for (Dob.Builder b : builder) {
            dobs.add(b.build());
        }
    }

    public void generate() {
        DataGenerator generator = event.getGenerator();
        generator.addProvider(event.includeServer(), new BaseRecipeProvider(generator) {
            @Override
            protected void buildCraftingRecipes(Consumer<FinishedRecipe> consumer) {
                for (Dob dob : dobs) {
                    dob.recipe().accept((builder, pattern) -> build(consumer, builder, pattern));
                }
            }
        });
        generator.addProvider(event.includeServer(), new BaseLootTableProvider(generator) {
            @Override
            protected void addTables() {
                for (Dob dob : dobs) {
                    dob.loot().accept(new ILootFactory() {
                        @Override
                        public void simpleTable(Supplier<? extends Block> block) {
                            addSimpleTable(block.get());
                        }

                        @Override
                        public void standardTable(Supplier<? extends Block> block, Supplier<? extends BlockEntityType<?>> be) {
                            addStandardTable(block.get(), be.get());
                        }
                    });
                }
            }
        });
        BaseBlockTagsProvider blockTags = new BaseBlockTagsProvider(generator, modid, event.getExistingFileHelper()) {
            @Override
            protected void addTags() {
                for (Dob dob : dobs) {
                    dob.blockTags().accept(new ITagFactory() {
                        @Override
                        public void blockTags(Supplier<? extends Block> blockSupplier, TagKey<Block>... tags) {
                            for (TagKey<Block> tag : tags) {
                                tag(tag).add(blockSupplier.get());
                            }
                        }

                        @Override
                        public void itemTags(Supplier<? extends Item> itemSupplier, TagKey<Item>... tags) {
                            // No op
                        }
                    });
                }
            }
        };
        generator.addProvider(event.includeServer(), blockTags);
        generator.addProvider(event.includeServer(), new ItemTagsProvider(generator, blockTags, modid, event.getExistingFileHelper()) {
            @Override
            protected void addTags() {
                for (Dob dob : dobs) {
                    dob.blockTags().accept(new ITagFactory() {
                        @Override
                        public void blockTags(Supplier<? extends Block> blockSupplier, TagKey<Block>... tags) {
                            // No op
                        }

                        @Override
                        public void itemTags(Supplier<? extends Item> itemSupplier, TagKey<Item>... tags) {
                            for (TagKey<Item> tag : tags) {
                                tag(tag).add(itemSupplier.get());
                            }
                        }
                    });
                }
            }
        });

        generator.addProvider(event.includeClient(), new BaseBlockStateProvider(generator, modid, event.getExistingFileHelper()) {
            @Override
            protected void registerStatesAndModels() {
                for (Dob dob : dobs) {
                    dob.blockstate().accept(this);
                }
            }
        });
        generator.addProvider(event.includeClient(), new BaseItemModelProvider(generator, modid, event.getExistingFileHelper()) {
            @Override
            protected void registerModels() {
                BaseItemModelProvider provider = this;
                for (Dob dob : dobs) {
                    dob.item().accept(new IItemFactory() {
                        @Override
                        public void parented(Supplier<? extends Block> blockSupplier, String model) {
                            provider.parentedBlock(blockSupplier.get(), model);
                        }

                        @Override
                        public void parented(Supplier<? extends Block> blockSupplier) {
                            provider.parentedBlock(blockSupplier.get());
                        }

                        @Override
                        public void generated(Supplier<? extends Item> itemSupplier, String texture) {
                            provider.itemGenerated(itemSupplier.get(), texture);
                        }

                        @Override
                        public void cubeAll(Supplier<? extends Item> itemSupplier, ResourceLocation texture) {
                            provider.cubeAll(name(itemSupplier.get()), texture);
                        }
                    });
                }
            }
        });
    }

    public static InventoryChangeTrigger.TriggerInstance has(ItemLike item) {
        return inventoryTrigger(ItemPredicate.Builder.item().of(item).build());
    }

    public static InventoryChangeTrigger.TriggerInstance inventoryTrigger(ItemPredicate... itemPredicate) {
        return new InventoryChangeTrigger.TriggerInstance(EntityPredicate.Composite.ANY, MinMaxBounds.Ints.ANY, MinMaxBounds.Ints.ANY, MinMaxBounds.Ints.ANY, itemPredicate);
    }

}
