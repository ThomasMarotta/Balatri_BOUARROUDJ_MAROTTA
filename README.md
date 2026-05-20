# Balatri

A simplified version of the Balatro card game — Java OOP Project, ESIEE E3 2026.

## Importing into Eclipse

1. Open Eclipse
2. `File > Import > General > Existing Projects into Workspace`
3. Select the unzipped project folder
4. Click `Finish`

> The project requires **Java 25**.  
> Check under `Project > Properties > Java Build Path` that the **Zen6** library is present.

## Running the project

Run the `Main.java` class (right-click > `Run As > Java Application`).

At startup, choose between the console view and the graphical view.

## Implemented features

- Modelling of all 52 cards (rank + suit)
- Poker hand detection (including the A-2-3-4-5 straight)
- Score calculation: `chips × multiplier`
- Full game loop: draw 8 cards, select 5, discard, automatic reshuffle
- Blind progression with score targets
- Planet rewards and application (permanent modifiers)
- Functional console view