package com.scheible.risingempire.game.impl2.colonization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.scheible.risingempire.game.api.universe.Player;
import com.scheible.risingempire.game.impl2.apiinternal.Credit;
import com.scheible.risingempire.game.impl2.apiinternal.Population;
import com.scheible.risingempire.game.impl2.apiinternal.Position;
import com.scheible.risingempire.game.impl2.apiinternal.ResearchPoint;
import com.scheible.risingempire.game.impl2.apiinternal.Rounds;
import com.scheible.risingempire.game.impl2.apiinternal.ShipClassId;
import com.scheible.risingempire.game.impl2.colonization.AnnexedSystemsProvider.AnnexedSystem;
import com.scheible.risingempire.game.impl2.colonization.SpaceDock.ConstructionProgress;
import com.scheible.risingempire.game.impl2.colonization.SpaceDock.SpaceDockOutput;
import com.scheible.risingempire.game.impl2.common.Command;

import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;

/**
 * @author sj
 */
public class Colonization {

	private final List<Colony> colonies;

	private final ColonyFleetProvider colonyFleetProvider;

	private final ShipCostProvider shipCostProvider;

	private InitialShipClassProvider initialShipClassProvider;

	private AnnexedSystemsProvider annexedSystemsProvider;

	private final Map<Position, Map<ShipClassId, Integer>> newShips = new HashMap<>();

	private final Set<Position> newColonies = new HashSet<>();

	private final Set<Colonize> colonizationCommands;

	public Colonization(ColonyFleetProvider colonyFleetProvider, ShipCostProvider shipCostProvider,
			InitialShipClassProvider initialShipClassProvider, AnnexedSystemsProvider annexedSystemsProvider) {
		this.colonies = new ArrayList<>(List.of( //
				new Colony(Player.BLUE, new Position("6.173", "5.026"), SpaceDock.UNINITIALIZED, new Population(50)),
				new Colony(Player.YELLOW, new Position("9.973", "5.626"), SpaceDock.UNINITIALIZED, new Population(50)),
				new Colony(Player.WHITE, new Position("4.080", "8.226"), SpaceDock.UNINITIALIZED, new Population(50))));

		this.colonyFleetProvider = colonyFleetProvider;
		this.shipCostProvider = shipCostProvider;
		this.initialShipClassProvider = initialShipClassProvider;
		this.annexedSystemsProvider = annexedSystemsProvider;

		this.colonizationCommands = new HashSet<>();
	}

	private Colonization(List<Colony> colonies, ColonyFleetProvider colonyFleetProvider,
			ShipCostProvider shipCostProvider, InitialShipClassProvider initialShipClassProvider,
			AnnexedSystemsProvider annexedSystemsProvider, Set<Colonize> colonizationCommands) {
		this.colonies = colonies;

		this.colonyFleetProvider = colonyFleetProvider;
		this.shipCostProvider = shipCostProvider;
		this.initialShipClassProvider = initialShipClassProvider;
		this.annexedSystemsProvider = annexedSystemsProvider;

		this.colonizationCommands = colonizationCommands;
	}

	public void initialize() {
		for (int i = 0; i < this.colonies.size(); i++) {
			Colony colony = this.colonies.get(i);

			ShipClassId initalShipClass = this.initialShipClassProvider.initial();
			ConstructionProgress progress = new ConstructionProgress(initalShipClass, new Credit(0));
			SpaceDockOutput output = spaceDockOutput(colony, initalShipClass, progress).spaceDockOutput();

			this.colonies.set(i, new Colony(colony.player(), colony.position(),
					new SpaceDock(initalShipClass, output, progress), colony.population()));
		}
	}

	public Colonization apply(List<ColonizationCommand> commands) {
		Set<Colonize> currrentColonizationCommands = commands.stream()
			.filter(Colonize.class::isInstance)
			.map(Colonize.class::cast)
			.collect(Collectors.toSet());
		Colonization copy = new Colonization(new ArrayList<>(this.colonies), this.colonyFleetProvider,
				this.shipCostProvider, this.initialShipClassProvider, this.annexedSystemsProvider,
				currrentColonizationCommands);

		copy.updateColonies(
				commands.stream().filter(ColonyCommand.class::isInstance).map(ColonyCommand.class::cast).toList());

		return copy;
	}

	public boolean colonizable(Player player, Position system) {
		return colony(system).isEmpty() && this.colonyFleetProvider.colonizableSystems(player).contains(system);
	}

	public Set<Colony> colonies(Player player) {
		return this.colonies.stream().filter(c -> c.player().equals(player)).collect(Collectors.toSet());
	}

	public Optional<Colony> colony(Player player, Position system) {
		return colony(system).filter(c -> c.player() == player);
	}

	public Optional<Colony> colony(Position system) {
		return this.colonies.stream().filter(c -> c.position().equals(system)).findFirst();
	}

	public void updateColonies(List<ColonyCommand> commands) {
		this.newColonies.clear();

		Map<Position, List<ColonyCommand>> colonyCommandMapping = commands.stream()
			.filter(ColonyCommand.class::isInstance)
			.map(ColonyCommand.class::cast)
			.collect(Collectors.groupingBy(ColonyCommand::colony));

		for (int i = 0; i < this.colonies.size(); i++) {
			Colony colony = this.colonies.get(i);
			List<ColonyCommand> colonyCommands = colonyCommandMapping.getOrDefault(colony.position(), List.of());

			SpaceDock spaceDock = colony.spaceDock();

			ShipClassId spaceDockShipClassId = colonyCommands.stream()
				.filter(SpaceDockShipClass.class::isInstance)
				.map(SpaceDockShipClass.class::cast)
				.findFirst()
				.map(SpaceDockShipClass::shipClassId)
				.orElse(spaceDock.current());

			SpaceDockOutput output = spaceDockOutput(colony, spaceDockShipClassId, spaceDock.progress())
				.spaceDockOutput();

			this.colonies.set(i, new Colony(colony.player(), colony.position(),
					new SpaceDock(spaceDockShipClassId, output, spaceDock.progress()), colony.population()));
		}
	}

	public void growPopulations() {
		for (int i = 0; i < this.colonies.size(); i++) {
			Colony colony = this.colonies.get(i);

			this.colonies.set(i, new Colony(colony.player(), colony.position(), colony.spaceDock(),
					colony.population().grow(new Population(100))));
		}
	}

	public void buildShips() {
		this.newShips.clear();

		for (int i = 0; i < this.colonies.size(); i++) {
			Colony colony = this.colonies.get(i);
			SpaceDock spaceDock = colony.spaceDock();

			SpaceDockOutputWithRemainingInvest finishRoundOutput = spaceDockOutput(colony, spaceDock.current(),
					spaceDock.progress());

			ConstructionProgress progress = new ConstructionProgress(spaceDock.current(),
					finishRoundOutput.remainingInvest());

			int newShipsCount = finishRoundOutput.spaceDockOutput().nextRoundCount();
			if (newShipsCount > 0) {
				this.newShips.put(colony.position(), Map.of(spaceDock.current(), newShipsCount));
			}

			SpaceDockOutput nextRoundOutput = spaceDockOutput(colony, spaceDock.current(), progress).spaceDockOutput();

			this.colonies.set(i, new Colony(colony.player(), colony.position(),
					new SpaceDock(spaceDock.current(), nextRoundOutput, progress), colony.population()));
		}
	}

	private SpaceDockOutputWithRemainingInvest spaceDockOutput(Colony colony, ShipClassId current,
			ConstructionProgress progress) {
		Credit buildCapacity = buildCapacity(colony.player(), colony.position());
		Credit invest = progress.build(current, buildCapacity);

		Credit shipCost = this.shipCostProvider.cost(colony.player(), current);
		int newShipsCount = invest.integerDivide(shipCost);

		Rounds roundsPerShip = new Rounds(1);
		if (newShipsCount == 0) {
			roundsPerShip = new Rounds(shipCost.subtract(invest).divideRoundUp(buildCapacity) + 1);
		}

		return new SpaceDockOutputWithRemainingInvest(new SpaceDockOutput(roundsPerShip, newShipsCount),
				invest.modulo(shipCost));
	}

	public void colonizeSystems(List<Colonize> commands) {
		for (Colonize colonize : commands) {
			if (colonize.skip()) {
				continue;
			}

			if (this.colonyFleetProvider.colonizableSystems(colonize.player()).contains(colonize.system())) {

				Population population = new Population(20);
				Colony preliminaryColony = new Colony(colonize.player(), colonize.system(), SpaceDock.UNINITIALIZED,
						population);

				ShipClassId initalShipClass = this.initialShipClassProvider.initial();
				ConstructionProgress progress = new ConstructionProgress(initalShipClass, new Credit(0));
				SpaceDockOutput output = spaceDockOutput(preliminaryColony, initalShipClass, progress)
					.spaceDockOutput();

				this.colonies.add(new Colony(colonize.player(), colonize.system(),
						new SpaceDock(initalShipClass, output, progress), population));
				this.newColonies.add(colonize.system());
			}
		}
	}

	public void annexSystems() {
		Map<Position, Player> annexedSystems = this.annexedSystemsProvider.annexedSystems()
			.stream()
			.collect(Collectors.toMap(AnnexedSystem::system, AnnexedSystem::player));

		for (int i = 0; i < this.colonies.size(); i++) {
			Colony colony = this.colonies.get(i);

			Player annexPlayer = annexedSystems.get(colony.position());
			if (annexPlayer != null) {
				this.colonies.set(i,
						new Colony(annexPlayer, colony.position(), colony.spaceDock(), colony.population()));
			}
		}
	}

	public void welcomeColonistTransports() {
	}

	public boolean transfareable(Player player, Position colony, int colonists) {
		return false;
	}

	public Credit buildCapacity(Player player, Position system) {
		return new Credit(1500);
	}

	public ResearchPoint researchPoints(Player player) {
		return new ResearchPoint(0);
	}

	public boolean homeSystem(Player player, Position position) {
		if (player == Player.BLUE && position.equals(new Position("6.173", "5.026"))) {
			return true;
		}
		else if (player == Player.YELLOW && position.equals(new Position("9.973", "5.626"))) {
			return true;
		}
		else if (player == Player.WHITE && position.equals(new Position("4.080", "8.226"))) {
			return true;
		}
		else {
			return false;
		}
	}

	public Map<Position, Map<ShipClassId, Integer>> newShips() {
		return unmodifiableMap(this.newShips);
	}

	public Map<Position, Map<ShipClassId, Integer>> newShips(Player player) {
		return this.newShips.entrySet()
			.stream()
			.filter(e -> colony(player, e.getKey()).isPresent())
			.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
	}

	public boolean colonizeCommand(Player player, Position system) {
		return this.colonizationCommands.stream()
			.filter(cc -> !cc.skip && cc.player() == player && cc.system().equals(system))
			.findFirst()
			.isPresent();
	}

	public Set<Position> newColonies() {
		return unmodifiableSet(this.newColonies);
	}

	public sealed interface ColonizationCommand extends Command {

	}

	public record Colonize(Player player, Position system, boolean skip) implements ColonizationCommand {

	}

	public sealed interface ColonyCommand extends ColonizationCommand {

		Position colony();

	}

	public record SpaceDockShipClass(Player player, Position colony, ShipClassId shipClassId) implements ColonyCommand {

	}

	public record AllocateResources(Player player, Position colony) implements ColonyCommand {

	}

	private record SpaceDockOutputWithRemainingInvest(SpaceDockOutput spaceDockOutput, Credit remainingInvest) {

	}

}
