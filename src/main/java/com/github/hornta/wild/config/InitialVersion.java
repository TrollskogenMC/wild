package com.github.hornta.wild.config;

import com.github.hornta.versioned_config.Configuration;
import com.github.hornta.versioned_config.IConfigVersion;
import com.github.hornta.versioned_config.Patch;
import com.github.hornta.versioned_config.Type;

import java.util.Collections;

public class InitialVersion implements IConfigVersion<ConfigKey> {
  @Override
  public int version() {
    return 1;
  }

  @Override
  public Patch<ConfigKey> migrate(Configuration<ConfigKey> configuration) {
    Patch<ConfigKey> patch = new Patch<>();
    patch.set(ConfigKey.LANGUAGE, "language", "english", Type.STRING);
    patch.set(ConfigKey.COOLDOWN, "cooldown", 60, Type.INTEGER);
    patch.set(ConfigKey.NO_BORDER_SIZE, "no_border_size", 5000, Type.INTEGER);
    patch.set(ConfigKey.USE_VANILLA_WORLD_BORDER, "use_vanilla_world_border", false, Type.BOOLEAN);
    patch.set(ConfigKey.IMMORTAL_DURATION_AFTER_TELEPORT, "immortal_duration_after_teleport", 5000, Type.INTEGER);
    patch.set(ConfigKey.CHARGE_ENABLED, "charge.enabled", false, Type.BOOLEAN);
    patch.set(ConfigKey.CHARGE_AMOUNT, "charge.amount", 10, Type.DOUBLE);
    patch.set(ConfigKey.DISABLED_WORLDS, "disabled_worlds", Collections.emptyList(), Type.LIST);
    patch.set(ConfigKey.WILD_ON_FIRST_JOIN_ENABLED, "wild_on_first_join.enabled", false, Type.BOOLEAN);
    patch.set(ConfigKey.WILD_ON_FIRST_JOIN_WORLD, "wild_on_first_join.world", "@same", Type.STRING);
    patch.set(ConfigKey.WILD_ON_DEATH_ENABLED, "wild_on_death.enabled", false, Type.BOOLEAN);
    patch.set(ConfigKey.WILD_ON_DEATH_WORLD, "wild_on_death.world", "@same", Type.STRING);
    patch.set(ConfigKey.WILD_DEFAULT_WORLD, "default_world", "@same", Type.STRING);
    patch.set(ConfigKey.PERF_BUFFER_SIZE, "performance.buffer_size", 256, Type.INTEGER);
    patch.set(ConfigKey.PERF_BUFFER_INTERVAL, "performance.buffer_interval", 20 * 2, Type.INTEGER);
    patch.set(ConfigKey.PERF_KEEP_BUFFER_LOADED, "performance.keep_buffer_loaded_size", 3, Type.INTEGER);
    patch.set(ConfigKey.PERF_COMMAND_MAX_TRIES, "performance.command_max_tries", 2, Type.INTEGER);
    patch.set(ConfigKey.VERBOSE, "verbose", false, Type.BOOLEAN);
    patch.set(ConfigKey.DROP_FROM_ABOVE_HEIGHT, "drop_from_above_height", 0, Type.INTEGER);
    patch.set(ConfigKey.CLAIMS_TOWNY_ENABLED, "claims.towny.enabled", false, Type.BOOLEAN);
    patch.set(ConfigKey.CLAIMS_TOWNY_ALLOW_WILD_TO_TOWN, "claims.towny.allow_wild_to_town", false, Type.BOOLEAN);
    patch.set(ConfigKey.CLAIMS_SABER_FACTIONS_ENABLED, "claims.saber_factions.enabled", false, Type.BOOLEAN);
    patch.set(ConfigKey.CLAIMS_SABER_FACTIONS_ALLOW_WILD_TO_FACTION, "claims.saber_factions.allow_wild_to_faction", false, Type.BOOLEAN);
    patch.set(ConfigKey.CLAIMS_GRIEF_PREVENTION_ENABLED, "claims.grief_prevention.enabled", false, Type.BOOLEAN);
    patch.set(ConfigKey.CLAIMS_GRIEF_PREVENTION_ALLOW_WILD_TO_CLAIM, "claims.grief_prevention.allow_wild_to_claim", false, Type.BOOLEAN);
    patch.set(ConfigKey.CLAIMS_FACTIONS_UUID_ENABLED, "claims.factions_uuid.enabled", false, Type.BOOLEAN);
    patch.set(ConfigKey.CLAIMS_FACTIONS_UUID_ALLOW_WILD_TO_FACTION, "claims.factions_uuid.allow_wild_to_faction", false, Type.BOOLEAN);
    patch.set(ConfigKey.CLAIMS_LANDS_ENABLED, "claims.lands.enabled", false, Type.BOOLEAN);
    patch.set(ConfigKey.CLAIMS_LANDS_ALLOW_WILD_TO_CLAIM, "claims.lands.allow_wild_to_claim", false, Type.BOOLEAN);
    return patch;
  }
}
