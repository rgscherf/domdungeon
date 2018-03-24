# Planning for FINAL DUNGEON

Final dungeon is a roguelike featuring fully customizable characters.
It includes a SNES-style combat system. Basically, it's FF6 with totally
malleable characters.

The main hook is that characters' battle actions are 'equipped' like items.

For example, you may find a 'WHITE MAG.' command in the dungeon and add it to
your fighter's command list. Now, you have a paladin-style character.

Commands can further be equipped with spells/techniques matching their
categories. Continuing the paladin example, the 'WHITE MAG.' command is empty
until the player finds a 'Cure' spell or whatever. 

Decisions happen when the player finds a 'better', or more fitting, command
that what she already has equipped. All spells/techniques are lost when dumping
an equipped command.

## Possible Commands

### With sub-commands:

- WHITE MAG. -> mental
    - Cure/2/3
    - Cure All/2/3
    - Regen
    - Regen All
    - Life
- BLACK MAG. -> mental
    - Attack/2/3
    - Attack All/2/3
    - Poison
    - Drain MP
- SUPPORT MAG. -> mental
    - Restore MP
    - Restore MP All
    - Haste
    - Slow
    - Dispel
    - Flee fight
- TOOLS -> phys or mental, by skill
    - Restore MP -> mental
    - Attack -> physical
    - Attack All -> physical
    - Confuse -> mental
- LORE -> phys or mental, by skill
    - Cast monster skill
- KATA (SWRDTECH) -> phys or mental, by skill
    - Huge single attack, ignore def -> phys
    - 4x attack, ignore def -> phys
    - Attack All -> phys
    - Restore HP -> mental
    - Life -> mental
- ITEM -> phys or mental, by item
    - Has all spell effects, but consumable

### No sub-commands:

- MIMIC (repeat last action) -> phys or mental, by skill
- FIGHT -> phys
- RAGE (b'serk) -> phys
- TAUNT (all attacks to you) -> def
- RUNIC ('taunt' next spell & absorb MP) -> def
- MEDIC (Relm's brush effect) -> mental

## Stats

Status come from equipment and affect skills.

- Physical
- Mental
- HP
- MP
- Defense (physD = def * physical, mentD = def * mental)
- Speed

## Equipment

Equipment slots:

- Armor
- R Hand
- L Hand

Armor mostly has Defense, with some other stats.

Hand slots can be any combination of weapons, shields, trinkets, etc. Main way
to influence stat distribution.

## Equations

### FIGHT damage

bpwr * (clamp 0.5 (pstr / pdef) 1.5)
