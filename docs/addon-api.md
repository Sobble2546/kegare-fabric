# Kegare addon API

Kegare's addon API lets your mod add **innate techniques**, **shared abilities** and **traits** to Kegare. They're built on the same
types as Kegare's own and go through the same systems: the server-side cast pipeline, Malice costs and cooldowns, Refinement unlocks,
Refinement and Assessment, rolls, the loadout, the HUD and the Sorcerer screen.

Everything public is in the package `com.cnethuka.kegare.api`. **Use nothing outside it.** The rest of Kegare is internal and may
change in any release.

> **Experimental until Kegare 1.0.** Within one minor version (0.2.0, 0.2.1, ...) nothing in `api` changes in a way that breaks
> your addon. A new minor version (0.3.0) may. Depend on one minor range at a time.

Design reference: [DESIGN.md §3.34](../DESIGN.md#334-addon-api-package-api).

## Setting up

**Dependency.** In your `neoforge.mods.toml`:

```toml
[[dependencies.yourmod]]
    modId = "kegare"
    type = "required"
    versionRange = "[0.2,0.3)"
    ordering = "AFTER"
    side = "BOTH"
```

Kegare isn't on a Maven repository yet (that's planned). Until then, build Kegare's jar (see the README) and add it to your project
as a local dependency. Kegare needs Player Animation Library, Photon, LDLib2 and Curios at runtime, so your dev environment needs
them too.

**Both sides.** Kegare's registries sync to clients, so your addon must be installed on the server and on every client, like Kegare.

**Server only.** Everything an ability does runs on the logical server. The client only sends key presses; never decide gameplay on
the client.

## A new innate technique

An innate technique is a rarity, an ordered list of abilities (cast with the number keys), each optionally with a special (cast with
R + the number), and an optional ultimate (V). Register it with a `DeferredRegister` from your mod's constructor:

```java
@Mod(MyAddon.MOD_ID)
public final class MyAddon {
    public static final String MOD_ID = "myaddon";

    private static final DeferredRegister<InnateTechnique> TECHNIQUES =
            DeferredRegister.create(KegareApi.INNATE_TECHNIQUES, MOD_ID);

    public static final Ability JOLT = new BaseAbility(ResourceLocation.fromNamespaceAndPath(MOD_ID, "jolt"), CastType.INSTANT) {
        @Override public double cost(ServerPlayer player) { return 30; }
        @Override public int cooldownTicks(ServerPlayer player) { return 40; }

        @Override
        public void execute(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            ctx.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(5), e -> e != player && e.isAlive())
                    .stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                    .ifPresent(target -> target.hurt(player.damageSources().indirectMagic(player, player), 3f));
        }
    };

    public static final DeferredHolder<InnateTechnique, InnateTechnique> SPARK = TECHNIQUES.register("spark",
            () -> InnateTechnique.builder(InnateTechniqueRarity.RARE)
                    .ability(JOLT)
                    .defaultUnlockLevels()
                    .lockedUltimate()
                    .maliceColor(0x55CC88)
                    .signatureAnimation(ResourceLocation.fromNamespaceAndPath(MOD_ID, "jolt_cast"))
                    .tabIcon(Items.LIGHTNING_ROD)
                    .build());

    public MyAddon(IEventBus modBus) {
        TECHNIQUES.register(modBus);
    }
}
```

What the builder takes:

| Method | What it does |
|---|---|
| `ability(a)` | The next ability in loadout order. At least one is required. |
| `ultimate(a)` / `lockedUltimate()` | The V ability, or a padlocked V slot. |
| `defaultUnlockLevels()` / `unlockLevel(a, level)` | **Required.** At which Refinement level each part unlocks (below). A definition without either throws. |
| `maliceColor(0xRRGGBB)` | The Malice bar's color while a player holds it. |
| `signatureAnimation(id)` | The player animation the roll reveal plays. |
| `tabIcon(item)` | The icon of its advancement tab, if you ship one. |
| `neverRolled()` | Rolls never land on it (below). |

Subclass `InnateTechnique` (passing the builder to its constructor) to use the hooks: `onAssigned`, `onRemoved` (clean up anything
the player left in the world), and `environment` (make costs depend on surroundings, as Rime is cheaper in the cold).

### Unlock levels

`defaultUnlockLevels()` follows Kegare's table, by the order you added the abilities: abilities at Refinement 0, 0, 2, 4 and 6, their
specials at 1, 3, 5, 7 and 9, the ultimate at 8. A sixth ability onward waits at 10 until you set it yourself. `unlockLevel(a, level)`
sets one ability, special or ultimate directly, and wins over the table. Server owners can override any of them in
`refinement.unlockLevelOverrides`.

### Rolls

Your technique is **rolled by default**, like Kegare's own. It shares its rarity's weight with the others of that rarity: an addon
Rare splits the Rare chance with Rime, and the chance of rolling a Rare doesn't change.

- Build it with `neverRolled()` if players should get it some other way (a quest, an item, your own command).
- Server owners can set `roll.addonTechniques = false` in `kegare-server.toml` to keep rolls to Kegare's own techniques.
- An admin can always give any technique with `/kegare innate set <player> <short name>`.

## Abilities

An ability is an `Ability`: usually a `BaseAbility` (its ID and cast type) with the methods you need overridden.

| Cast type | What happens (on the server) |
|---|---|
| `INSTANT` | press, checks, pay `cost`, `execute`, cooldown |
| `CHARGED` | press, checks, charge (`tick` each tick), release after `minChargeTicks`: pay, `execute` with `ctx.charge()` 0 to 1, `onEnd(RELEASED)`, cooldown. An early release costs nothing. |
| `CHANNELED` | press, checks, pay `cost`, `execute`, then each tick pay `upkeepPerSecond` / 20 and `tick`, until released, out of Malice, `maxChannelTicks` or a stun: `onEnd`, cooldown |
| `TOGGLE` | press, checks, pay `cost`, `execute`, then upkeep and `tick` each tick until pressed again or out of Malice: `onEnd`, cooldown. Stuns don't end toggles. |

Kegare runs the shared checks before your code: the ability is unlocked, the player is alive, not Severed, not stunned, not busy, off
cooldown, meets the requirements and can pay. Add your own with `canCast` (and `canRelease` for CHARGED); a refusal costs nothing. Once
`execute` runs, the cost is paid and the cooldown starts, so refuse in `canCast`, not in `execute`.

Other things an ability can override: `shownCost` and `shownCooldownTicks` (what the screens quote), `releaseCost` (a CHARGED cost that
grows with the charge), `requirements`, `hasNoTarget`, `visuals`, `category`, `descriptionArgs` (numbers for its description's `%s`).

### Specials

Override `special()` on the base ability and build the special on `SpecialAbility`. It's a full ability with its own cast type, cost and
effect, and it shares its base's cooldown. Requirements are checked with the shared checks:

```java
public static final Ability HUM = new BaseAbility(id("hum"), CastType.TOGGLE) {
    // cost, cooldown, upkeep...
    @Override public Optional<Ability> special() { return Optional.of(HUM_BURST); }
};

public static final SpecialAbility HUM_BURST =
        new SpecialAbility(id("hum_burst"), CastType.INSTANT, () -> HUM, Requirement.toggleActive(HUM)) {
    @Override public double cost(ServerPlayer player) { return 15; }
    @Override public void execute(AbilityContext ctx) { /* ... */ }
};
```

`Requirement.toggleActive`, `Requirement.trait` and `Requirement.of(test, description)` build requirements.

**Every special needs a tell:** something other players can see or hear at the moment it's cast (an animation, an effect, a sound). This
is a Kegare design rule (DESIGN §3.15) that your specials should follow too.

### Visuals

`visuals()` returns an `AbilityVisuals` built per phase (`cast`, `chargeLoop`, `release`, `channelLoop`, `toggleOn`, `toggleLoop`,
`toggleOff`, `projectileTrail`, `impact`). Each phase takes a player animation ID (Player Animation Library, in
`assets/<your namespace>/player_animations/`) and/or a Photon effect ID (in `assets/<your namespace>/fx/`). The server sends them to
clients; they never affect gameplay.

## Shared abilities

A shared ability isn't tied to a technique: it's under the Malice or Melee tab of the Sorcerer screen (its `category()`), a player
unlocks it once and keeps it through rerolls, and any loadout slot can hold it. Register it from your mod's constructor:

```java
KegareApi.registerSharedAbility(MY_ABILITY);            // unlocked with KegareApi.unlockSharedAbility(player, MY_ABILITY)
KegareApi.registerSharedAbilityForEveryone(MY_ABILITY); // every Malice user has it from the start
```

Deciding when a player unlocks it is up to you; call `KegareApi.unlockSharedAbility` when they earn it. Severed players can't have shared
abilities.

## Traits

A trait is a `Trait`: flags in `Trait.Properties` (`unique`, `requiresMalice`, `inheritable`, `lostOnPvpDeath`, `birthChance`) and hooks
(`onAdded`, `onRemoved`, `canRollFor`, `onBearerKilledByPlayer`, succession settings for inheritable ones). Register it with a
`DeferredRegister` on `KegareApi.TRAITS`. Kegare rolls it at birth with its `birthChance`, saves it, syncs it and handles uniqueness and
succession. What it *does* is your code: check for it with `Requirement.trait(...)` or in your own events.

## Refinement and Assessment

Most of it is automatic. While an ability runs (`execute`, `tick` and `onEnd`), anything it hurts, pushes or puts an effect on counts as
that ability landing, and so does anything an entity spawned during the cast does later. Call `KegareProgression` only for what can't be
seen from a hit:

| Method | When |
|---|---|
| `landed(caster, target, ability)` | It landed without hurting (a swap, a tether, a mark). |
| `during(caster, ability, effect)` | A delayed effect (a timer, a scheduled burst): run it inside the cast so its hits count. |
| `landedOnItsOwn(owner, target, ability)` | A summon hit something on its own: it only earns Refinement while its owner is fighting too. |
| `addAttributor(attributor)` | Damage that's yours but not dealt in a cast or by an entity a cast spawned: say which ability it was. |

An entity that belongs to a player (a summon, a decoy) implements `OwnedEntity`, so its hits are its owner's and its owner gets nothing
for hitting it. An ability with nothing to land on (it moves or buffs the caster) overrides `hasNoTarget()`: it then counts when used in
a real fight.

## Text

Kegare checks only its own lang file. Ship yours in `assets/<your namespace>/lang/en_us.json`, with every key filled in:

| Key | For |
|---|---|
| `innate_technique.<ns>.<path>` | The technique's name |
| `innate_technique.<ns>.<path>.desc` | Its description |
| `innate_technique.<ns>.<path>.epithet` | One short line under its name and in the Epic and Legendary reveals |
| `ability.<ns>.<path>` and `.desc` | Each ability's and special's name and description |
| `trait.<ns>.<path>` and `.desc` | Each trait's name and description |

Player-facing text uses display names, never raw IDs.

## Kegare's rules your content should keep

- **Bosses** (the `c:bosses` entity tag) take damage but are never moved, swapped or disabled.
- **Mobs and players:** physical effects (damage, movement, knockback, status effects) work on mobs too; identity effects (reading,
  copying or disguising as a player) are players only.
- **Controls:** casting is the number keys, R + number for specials and V for the ultimate, in Technique Mode. Right-click stays vanilla.
- Put your gameplay numbers in your own config, so server owners can tune them.

## What's yours, not Kegare's

Your lang, config, assets, networking and saved data. Kegare's network version never changes because of an addon.

Kegare's **code** is MIT-licensed, but its **assets** (models, textures, sounds, player animations and effects) are All Rights Reserved:
make your own instead of copying Kegare's into your addon. See the README's License section.

## Uninstalling an addon

Players are safe if a server removes your addon: anyone who held one of its techniques gets a free reroll on login, slots holding its
abilities are emptied, and its traits are dropped.

## Commands

`/kegare innate set`, `/kegare trait give` and the other commands name your technique, trait or ability by its short name (the path of
its ID, `spark`) or its full ID (`myaddon:spark`), with suggestions and hover text like Kegare's own. If your short name is also one of
Kegare's, Kegare's keeps it and yours goes by its full ID in suggestions and lists. Picking names Kegare doesn't use keeps them short.

## Not in the API yet

Planned for later updates: changing Kegare's own techniques (adding, tweaking and replacing their abilities), one shared event for making
your entities immune to swaps and time stops, hooking into the Impact System, read-only events for casts, technique changes and
progression, client hooks (custom drawing, HUD lines), and a Maven repository. If you need one of these now, open an issue.
