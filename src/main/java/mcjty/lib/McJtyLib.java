package mcjty.lib;

import mcjty.lib.base.ModBase;
import mcjty.lib.base.StyleConfig;
import mcjty.lib.multipart.MultipartBlock;
import mcjty.lib.multipart.MultipartHelper;
import mcjty.lib.multipart.MultipartTE;
import mcjty.lib.network.IServerCommand;
import mcjty.lib.network.PacketSendPreferencesToClient;
import mcjty.lib.network.PacketSetGuiStyle;
import mcjty.lib.preferences.PreferencesDispatcher;
import mcjty.lib.preferences.PreferencesProperties;
import mcjty.lib.proxy.ClientProxy;
import mcjty.lib.proxy.ServerProxy;
import mcjty.lib.setup.ModSetup;
import mcjty.lib.proxy.IProxy;
import mcjty.lib.typed.TypedMap;
import mcjty.lib.varia.Logging;
import mcjty.lib.worlddata.AbstractWorldData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Mod(McJtyLib.MODID)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
//
//@Mod(modid = McJtyLib.PROVIDES, name = "McJtyLib",
//        acceptedMinecraftVersions = "[1.12,1.13)",
//        version = McJtyLib.VERSION,
//        dependencies = "after:forge@[14.23.5.2800,);after:enderio@[5.0.21,)")
public class McJtyLib implements ModBase {

    public static final String VERSION = "3.5.3";
    public static final String MODID = "mcjtylib";

    private static final ResourceLocation PREFERENCES_CAPABILITY_KEY;

    @CapabilityInject(PreferencesProperties.class)
    public static Capability<PreferencesProperties> PREFERENCES_CAPABILITY;

    public static IProxy proxy = DistExecutor.runForDist(() -> () -> new ClientProxy(), () -> () -> new ServerProxy());
    public static ModSetup setup = new ModSetup();

    public static McJtyLib instance;

    public static SimpleChannel networkHandler;
    private static boolean init;
    public static boolean tesla, cofhapiitem;

    private static final Map<Pair<String, String>, IServerCommand> serverCommands = new HashMap<>();
    private static final Map<Pair<String, String>, IServerCommand> clientCommands = new HashMap<>();

    private static final Map<String, ModBase> mods = new HashMap<>();

    public McJtyLib() {
        instance = this;
        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::init);

        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, StyleConfig.CLIENT_CONFIG);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, StyleConfig.SERVER_CONFIG);

        StyleConfig.loadConfig(StyleConfig.CLIENT_CONFIG, FMLPaths.CONFIGDIR.get().resolve("mcjtylib-client.toml"));
        StyleConfig.loadConfig(StyleConfig.SERVER_CONFIG, FMLPaths.CONFIGDIR.get().resolve("mcjtylib-server.toml"));
    }

    public static void registerMod(ModBase mod) {
        mods.put(mod.getModId(), mod);
    }

    public static void forEachMod(Consumer<ModBase> consumer) {
        for (ModBase mod : mods.values()) {
            consumer.accept(mod);
        }
    }

    @SubscribeEvent
    public void serverStarted(FMLServerAboutToStartEvent event) {
        Logging.log("Preparing all world data");
        AbstractWorldData.clearInstances();
    }

    @SubscribeEvent
    public void serverStopped(FMLServerStoppedEvent event) {
        Logging.log("Cleaning up all world data: " + AbstractWorldData.getDataCount() + " data blobs");
        AbstractWorldData.clearInstances();
    }

    @Override
    public String getModId() {
        return MODID;
    }

    @Override
    public void openManual(PlayerEntity player, int bookindex, String page) {

    }

    public static void registerCommand(String modid, String id, IServerCommand command) {
        serverCommands.put(Pair.of(modid, id), command);
    }

    public static void registerClientCommand(String modid, String id, IServerCommand command) {
        clientCommands.put(Pair.of(modid, id), command);
    }

    public static boolean handleCommand(String modid, String id, PlayerEntity player, TypedMap arguments) {
        IServerCommand command = serverCommands.get(Pair.of(modid, id));
        if (command == null) {
            return false;
        }
        return command.execute(player, arguments);
    }

    public static boolean handleClientCommand(String modid, String id, PlayerEntity player, TypedMap arguments) {
        IServerCommand command = clientCommands.get(Pair.of(modid, id));
        if (command == null) {
            return false;
        }
        return command.execute(player, arguments);
    }

    public void init(final FMLCommonSetupEvent event) {
        setup.preInit(event);
        proxy.preInit(event);
    }

    public static void preInit(final FMLCommonSetupEvent event) {
        if (init) {
            return;
        }
        registerCapabilities();
        networkHandler = NetworkRegistry.newSimpleChannel(new ResourceLocation(MODID, MODID), () -> "1.0", s -> true, s -> true);
        networkHandler.registerMessage(PacketSendPreferencesToClient.Handler.class, PacketSendPreferencesToClient.class, 0, Side.CLIENT);
        networkHandler.registerMessage(PacketSetGuiStyle.Handler.class, PacketSetGuiStyle.class, 1, Side.SERVER);
        MinecraftForge.EVENT_BUS.register(new EventHandler());
        init = true;
        tesla = ModList.get().isLoaded("tesla");
        cofhapiitem = ModList.get().isLoaded("cofhapi|item");
    }

    public static PreferencesProperties getPreferencesProperties(PlayerEntity player) {
        return player.getCapability(PREFERENCES_CAPABILITY, null);
    }

    public static class EventHandler {

        private EventHandler(){
        }

        @SubscribeEvent
        public void onPlayerTickEvent(TickEvent.PlayerTickEvent event) {
            if (event.phase == TickEvent.Phase.START && !event.player.getEntityWorld().isRemote) {
                PreferencesProperties preferencesProperties = getPreferencesProperties(event.player);
                preferencesProperties.tick((PlayerEntityMP) event.player);
            }
        }

        @SubscribeEvent
        public void onEntityConstructing(AttachCapabilitiesEvent<Entity> event){
            if (event.getObject() instanceof PlayerEntity) {
                if (!event.getCapabilities().containsKey(PREFERENCES_CAPABILITY_KEY) && !event.getObject().hasCapability(PREFERENCES_CAPABILITY, null)) {
                    event.addCapability(PREFERENCES_CAPABILITY_KEY, new PreferencesDispatcher());
                } else {
                    throw new IllegalStateException(event.getObject().toString());
                }
            }
        }

        @SubscribeEvent
        public void onPlayerInteract(PlayerInteractEvent.LeftClickBlock event) {
            World world = event.getWorld();
            BlockPos pos = event.getPos();
            BlockState state = world.getBlockState(pos);
            if (state.getBlock() instanceof MultipartBlock) {
                TileEntity tileEntity = world.getTileEntity(pos);
                if (tileEntity instanceof MultipartTE) {
                    if (!world.isRemote) {
                        if (MultipartHelper.removePart((MultipartTE) tileEntity, state, event.getPlayerEntity(), event.getHitVec())) {
                            world.setBlockToAir(pos);
                        }
                    }
                }
                event.setCanceled(true);
            }
        }

    }

    private static void registerCapabilities(){
        CapabilityManager.INSTANCE.register(PreferencesProperties.class, new Capability.IStorage<PreferencesProperties>() {

            @Override
            public NBTBase writeNBT(Capability<PreferencesProperties> capability, PreferencesProperties instance, Direction side) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void readNBT(Capability<PreferencesProperties> capability, PreferencesProperties instance, Direction side, NBTBase nbt) {
                throw new UnsupportedOperationException();
            }

        }, () -> {
            throw new UnsupportedOperationException();
        });
    }

    static {
        PREFERENCES_CAPABILITY_KEY = new ResourceLocation(MODID, "Preferences");
    }

}
