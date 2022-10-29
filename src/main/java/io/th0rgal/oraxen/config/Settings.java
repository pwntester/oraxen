package io.th0rgal.oraxen.config;

import io.th0rgal.oraxen.OraxenPlugin;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public enum Settings {

    PLUGIN_LANGUAGE("Plugin.language"),
    REPAIR_COMMAND_ORAXEN_DURABILITY("Plugin.commands.repair.oraxen_durability_only"),
    GENERATE_DEFAULT_ASSETS("Plugin.generation.default_assets"),
    GENERATE_DEFAULT_CONFIGS("Plugin.generation.default_configs"),
    FORMAT_INVENTORY_TITLES("Plugin.formatting.inventory_titles"),
    FORMAT_TITLES("Plugin.formatting.titles"),
    FORMAT_SUBTITLES("Plugin.formatting.subtitles"),
    FORMAT_ACTION_BAR("Plugin.formatting.action_bar"),
    FORMAT_ANVIL("Plugin.formatting.anvil"),


    CONFIGS_VERSION("configs_version"),
    UPDATE_CONFIGS("ConfigsTools.enable_configs_updater"),
    AUTOMATICALLY_SET_GLYPH_CODE("ConfigsTools.automatically_set_glyph_code"),
    AUTOMATICALLY_SET_MODEL_DATA("ConfigsTools.automatically_set_model_data"),
    SKIPPED_MODEL_DATA_NUMBERS("ConfigsTools.skipped_model_data_numbers"),
    ERROR_ITEM("ConfigsTools.error_item"),

    RESET_RECIPES("Misc.reset_recipes"),
    ARMOR_EQUIP_EVENT_BYPASS("Misc.armor_equip_event_bypass"),
    SHIELD_DISPLAY("Misc.shield_display"),
    BOW_DISPLAY("Misc.bow_display"),
    CROSSBOW_DISPLAY("Misc.crossbow_display"),
    AUTO_UPDATE_ITEMS("Misc.auto_update_items"),

    GENERATE("Pack.generation.generate"),
    ARMOR_RESOLUTION("Pack.generation.armor_resolution"),
    AUTOMATICALLY_GENERATE_SHADER_COMPATIBLE_ARMOR("Pack.generation.automatically_generate_shader_compatible_armor"),
    COMPRESSION("Pack.generation.compression"),
    PROTECTION("Pack.generation.protection"),
    COMMENT("Pack.generation.comment"),

    UPLOAD_TYPE("Pack.upload.type"),
    UPLOAD("Pack.upload.enabled"),
    UPLOAD_OPTIONS("Pack.upload.options"),

    POLYMATH_SERVER("Pack.upload.polymath.server"),

    SEND_PACK("Pack.dispatch.send_pack"),
    SEND_PACK_DELAY("Pack.dispatch.delay"),
    SEND_PACK_ADVANCED("Pack.dispatch.send_pack_advanced.enabled"),
    SEND_PACK_ADVANCED_MANDATORY("Pack.dispatch.send_pack_advanced.mandatory"),
    SEND_PACK_ADVANCED_MESSAGE("Pack.dispatch.send_pack_advanced.message"),
    SEND_JOIN_MESSAGE("Pack.dispatch.join_message.enabled"),
    JOIN_MESSAGE_DELAY("Pack.dispatch.join_message.delay"),

    RECEIVE_ENABLED("Pack.receive.enabled"),

    RECEIVE_ALLOWED_ACTIONS("Pack.receive.accepted.actions"),
    RECEIVE_LOADED_ACTIONS("Pack.receive.loaded.actions"),
    RECEIVE_FAILED_ACTIONS("Pack.receive.failed_download.actions"),
    RECEIVE_DENIED_ACTIONS("Pack.receive.denied.actions");

    private final String path;

    Settings(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public Object getValue() {
        return OraxenPlugin.get().getConfigsManager().getSettings().get(path);
    }

    @Override
    public String toString() {
        return (String) getValue();
    }

    public Boolean toBool() {
        return (Boolean) getValue();
    }

    public List<String> toStringList() {
        return OraxenPlugin.get().getConfigsManager().getSettings().getStringList(path);
    }

    public ConfigurationSection toConfigSection() {
        return OraxenPlugin.get().getConfigsManager().getSettings().getConfigurationSection(path);
    }

}
