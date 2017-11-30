package ufl.cs1.controllers;

import game.controllers.DefenderController;
import game.models.Attacker;
import game.models.Defender;
import game.models.Game;
import game.models.Node;

import java.util.List;

public final class StudentController implements DefenderController
{
	private Game currentGame;

	public void init(Game game) {
	}

	public void shutdown(Game game) { }

	public int[] update(Game game,long timeDue)
	{
		// Assign the global variable for functions outside the scope
		this.currentGame = game;
		// Initialize the actions array
		int[] actions = new int[Game.NUM_DEFENDER];

		// Iterate through each ghost to assign directions
		for(int ghostNum = 0; ghostNum < game.NUM_DEFENDER; ghostNum++)
		{
			// If pacman is near a power pill, send a sacrifice
			if (isNearPowerPill()) {
				switch (ghostNum) {
					// Sacrifice a ghost
					case 1:
						actions[ghostNum] = chasePacman(ghostNum);
						break;
					// Everyone else run!
					default:
						actions[ghostNum] = runAway(ghostNum);
						break;
				}
			}
			// Otherwise, follow individual unique behaviors
			switch (ghostNum)
			{
				case 0:
					// First ghost is a predictor
					actions[ghostNum] = predictPacman(ghostNum);
					break;
				case 1:
					// Second ghost directly chases
					actions[ghostNum] = chasePacman(ghostNum);
					break;
				case 2:
					// Third ghost patrols power pills
					actions[ghostNum] = patrolPowerPill(ghostNum);
					break;
				case 3:
					// Fourth ghost also patrols power pills
					actions[ghostNum] = patrolPowerPill(ghostNum);
					break;
			}
		}
		return actions;
	}

	/**
	 * <h1>chasePacman</h1>
	 * The chasePacman function, will always chase pacman directly.
	 * @param currGhost an integer representing a ghost ID.
	 * @return Direction needed to approach pacman directly.
	 */
	private int chasePacman(int currGhost) {
		// Create a variable for the passed in ghost
		Defender activeGhost = this.currentGame.getDefender(currGhost);
		// Return the exact location of Pacman
		return activeGhost.getNextDir(currentGame.getAttacker().getLocation(), true);
	}

	/**
	 * <h1>patrolPowerPill</h1>
	 * The patrolPowerPill method will send the ghost on a patrolling
	 * path around specific power pill nodes. As the power pills are eaten
	 * the ghosts will change patrolling locations to viable power pill
	 * locations.
	 * @param currGhost an integer representing a ghost ID.
	 * @return Directions to a specific power pill. If none left, will use
	 * the fallback behavior.
	 */
	private int patrolPowerPill(int currGhost) {
		/*
		Create a list of the currently available power pills, along with
		a variable for our passed in ghost
		 */
		List<Node> powerPills = this.currentGame.getPowerPillList();
		Defender activeGhost = this.currentGame.getDefender(currGhost);

		// If there are no power pills remaining, use fallback behavior
		if (powerPills.size() == 0) {
			return fallbackBehavior(currGhost);
		}
		// If there is only one power pill, patrol it.
		else if (powerPills.size() == 1) {
			return activeGhost.getNextDir(powerPills.get(0), true);
		}
		// Otherwise if there are > 1 power pill remaining, patrol them separately
		else if (powerPills.size() == 2) {
			return activeGhost.getNextDir(powerPills.get(currGhost - 2), true);
		} else {
			return activeGhost.getNextDir(powerPills.get(currGhost - 1), true);
		}
	}

	/**
	 * <h1>predictPacman</h1>
	 * The predictPacman function uses pacman's current location as a reference
	 * point. It then attempts to "predict" distancePredicted units in the direction
	 * pacman is facing.
	 * <p>
	 * This method will check if the predicted location is null after creation, if the
	 * location is found to be null, it will directly chase pacman instead.
	 * @param currGhost an integer representing a ghost ID.
	 * @return Direction needed to approach either prediction, or pacman directly.
	 */
	private int predictPacman(int currGhost) {
		// Create a variable for the ghost passed in, along with the attacker
		Defender activeGhost = this.currentGame.getDefender(currGhost);
		Attacker pacman = this.currentGame.getAttacker();
		// Create variable for how many units to predict
		int distancePredicted = 4;

		// Get Pacman's current location
		Node predictedLocation = pacman.getLocation();

		// Loop until we have predicted 4 spaces ahead
		while (distancePredicted > 0) {
			if (predictedLocation != null) {
				predictedLocation = predictedLocation.getNeighbor(pacman.getDirection());
			}
			distancePredicted--;
		}

		/*
		If Pacman is moving either Up, or Right, we will attempt to predict a flank
		instead of trying to reach in front of him. Beneficial based on the layout's
		of most levels, and power pill locations.
		 */
		if (pacman.getDirection() == 0 || pacman.getDirection() == 1) {
			if (predictedLocation != null) {
				switch(pacman.getDirection()) {
					// Case for moving Up, we flank his Right
					case 0:
						predictedLocation = predictedLocation.getNeighbor(1);
						break;
					// Case for moving Right, we flank directly behind
					case 1:
						predictedLocation = predictedLocation.getNeighbor(3);
						break;
				}
			}
		}

		// Check if the prediction is in a valid location
		if (predictedLocation != null) {
			// If prediction is valid, return it.
			return activeGhost.getNextDir(predictedLocation, true);
		} else {
			// Otherwise, fallback onto chasing Pacman directly.
			return chasePacman(currGhost);
		}
	}

	/**
	 * <h1>fallbackBehavior</h1>
	 * The fallbackBehavior method decides what style of chasing
	 * a ghost should use, if their unique behavior is failing.
	 * @param currGhost an integer representing a ghost ID.
	 * @return either predictPacman's result or chasePacman depending on distance.
	 */
	private int fallbackBehavior(int currGhost) {
		// Create a variable for the ghost given in arguments
		Defender activeGhost = this.currentGame.getDefender(currGhost);

		// Decide how to approach Pacman, based on distance
		if (activeGhost.getLocation().getPathDistance(this.currentGame.getAttacker().getLocation()) > 10) {
			return predictPacman(currGhost);
		} else {
			return chasePacman(currGhost);
		}
	}

	/**
	 * <h1>isNearPowerPill</h1>
	 * The isNearPowerPill function searches the list of available power pills
	 * and checks if Pacman is within a certain distance of one.
	 * @return True if nearby pill, false if not.
	 */
	private boolean isNearPowerPill() {
		/*
		Create an Attacker variable for Pacman, and a list of all available power
		pills. Lastly, create a Node variable for the closest pill.
		 */
		Attacker pacman = this.currentGame.getAttacker();
		List<Node> powerPills = this.currentGame.getPowerPillList();
		Node closestPowerPill;

		// Check if there are any viable power pill's remaining
		if (powerPills.size() == 0) {
			return false;
		}
		// If there is, find the closest one to Pacman
		else {
			closestPowerPill = pacman.getTargetNode(powerPills, true);
		}

		// Check if Pacman is within 50 units of a power pill and return true if so
		if (pacman.getLocation().getPathDistance(closestPowerPill) < 50) {
			return true;
		}

		// Otherwise, Pacman is not near a power pill
		return false;
	}

	/**
	 * <h1>runAway</h1>
	 * The runAway method flee's Pacman
	 * @param currGhost an integer representing a ghost ID.
	 * @return direction needed to flee Pacman.
	 */
	private int runAway(int currGhost) {
		// Create a variable for the passed in ghost
		Defender activeGhost = this.currentGame.getDefender(currGhost);
		// Run away from Pacman's location
		return activeGhost.getNextDir(this.currentGame.getAttacker().getLocation(), false);
	}
}