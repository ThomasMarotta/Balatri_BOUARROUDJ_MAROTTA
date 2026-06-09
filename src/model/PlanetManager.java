package model;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import domain.HandRank;
import domain.Planet;

public class PlanetManager {
	private final Map<HandRank, Integer> levels;

	public PlanetManager() {
		this.levels = Arrays.stream(HandRank.values()).collect(Collectors.toMap(rank -> rank, level -> 1));
	}

	public void applyPlanet(Planet planet) {
		Objects.requireNonNull(planet);
		var target = planet.target;
		var currentLevel = levels.get(target);
		levels.put(target, currentLevel + 1);
	}

	public int getLevel(HandRank rank) {
		return levels.get(rank);
	}

	public Planet getRandomPlanet() {
		var planets = Planet.values();
		return planets[(int) (Math.random() * planets.length)];
	}

}
