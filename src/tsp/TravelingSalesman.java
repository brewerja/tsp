package tsp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;

public class TravelingSalesman {

	private int numCities;
	private City[] baseCityArray;
	private ArrayList<Route> routes;
	private double[][] matrix;
	private Random generator = new Random();
	private int generation_size;
	private Map<Integer, City> directory;

	private final int POPULATION_SIZE = 1000000;
	private final int EVOLVING_POPULATION_SIZE = 500;
	private final double ELITISM_PCT = 0.1;
	private final int NUMBER_OF_GENERATIONS = 200;
	private final double MUTATION_RATE = 0.3;
	private final double CROSSOVER_RATE = 0.9;
	private final int TOURNAMENT_SIZE = 10;

	public static void main(String[] args) throws NumberFormatException, IOException {
		// Run the algorithm a number of times and take the best result.
		int EVOLUTIONS = 10;
		ArrayList<Route> best = new ArrayList<Route>(EVOLUTIONS);
		for (int j = 0; j < EVOLUTIONS; ++j) {
			TravelingSalesman ts = new TravelingSalesman(args[0]);
			ts.solve();
			Collections.sort(ts.routes);
			Route topRoute = ts.routes.get(0);
			best.add(topRoute);
			System.out.println("Evolution " + (j + 1) + " complete: " + topRoute.getRouteLength());
		}

		// List all results, along with the winner.
		System.out.println("Evolutions Results:");
		Collections.sort(best);
		for (Route r : best)
			System.out.println(r.getRouteLength());
		System.out.println("Best result:" + best.get(0).getRouteLength());
		best.get(0).printRoute();
	}

	public TravelingSalesman(String path) throws NumberFormatException, IOException {
		// Parse the file of city listings.
		FileReader file = new FileReader(path);
		BufferedReader bf = new BufferedReader(file);
		String line;
		ArrayList<City> cities = new ArrayList<City>();
		while ((line = bf.readLine()) != null) {
			// Each line should be (Name, x, y).
			String[] params = line.split(",");
			String name = params[0].trim();
			double x = new Double(params[1].trim());
			double y = new Double(params[2].trim());
			// Create a City object for each line.
			City c = new City(name, cities.size(), x, y);
			cities.add(c);
		}

		// Store the number of cities to be traversed.
		numCities = cities.size();

		// Create the "base" route and create the distance matrix based off of
		// the ordering of that route.
		baseCityArray = new City[numCities];
		for (int i = 0; i < numCities; ++i)
			baseCityArray[i] = cities.get(i);
		createMatrix();

		// Initialize lookup table of city id's to their objects.
		directory = new HashMap<Integer, City>(numCities);
		for (City c : baseCityArray)
			directory.put(c.getId(), c);

		// Create the initial population of randomized routes.
		ArrayList<Route> init_routes = new ArrayList<Route>(POPULATION_SIZE);
		for (int i = 0; i < POPULATION_SIZE; ++i) {
			Collections.shuffle(cities);
			City[] route = new City[numCities];
			for (int j = 0; j < numCities; ++j)
				route[j] = cities.get(j);
			init_routes.add(new Route(route, calcRouteLength(route)));
		}

		// for (City c: baseCityArray)
		// init_routes.add(nearestNeighborTour(c));

		// Pick the fittest routes to form the evolving population.
		Collections.sort(init_routes);
		routes = new ArrayList<Route>(EVOLVING_POPULATION_SIZE);
		for (int i = 0; i < EVOLVING_POPULATION_SIZE; ++i)
			routes.add(init_routes.get(i));
	}

	private void createMatrix() {
		matrix = new double[numCities][numCities];
		for (int i = 0; i < numCities - 1; ++i) {
			matrix[i][i] = 0.0;
			City city1 = baseCityArray[i];
			for (int j = i + 1; j < numCities; ++j) {
				City city2 = baseCityArray[j];
				double delta_x = (city1.getX() - city2.getX());
				double delta_y = (city1.getY() - city2.getY());
				double distance = Math.round(Math.sqrt(delta_x * delta_x + delta_y * delta_y));
				matrix[i][j] = distance;
				matrix[j][i] = distance;
			}
		}
		matrix[numCities - 1][numCities - 1] = 0.0;
	}

	private double calcRouteLength(City[] cityArray) {
		double distance = 0;
		int c1 = cityArray[0].getId();
		for (int i = 1; i < numCities; ++i) {
			int c2 = cityArray[i].getId();
			distance += matrix[c1][c2];
			c1 = c2;
		}
		distance += matrix[c1][cityArray[0].getId()]; // Return to start.
		return distance;
	}

	public void solve() {
		for (int i = 0; i < NUMBER_OF_GENERATIONS; ++i)
			evolve();
	}

	public void evolve() {

		// Create a child each iteration until a full generation has been born.
		generation_size = (int) Math.round((1 - ELITISM_PCT) * EVOLVING_POPULATION_SIZE);
		ArrayList<Route> newChildren = new ArrayList<Route>(generation_size);

		for (int j = 0; j < generation_size; ++j) {

			// Randomly select a set of routes to vie for the right to parent.
			ArrayList<Route> possibleParents = new ArrayList<Route>(TOURNAMENT_SIZE);
			for (int i = 0; i < TOURNAMENT_SIZE; ++i) {
				int index = generator.nextInt(routes.size() - 1);
				possibleParents.add(routes.get(index));
			}

			// Select the 2 fittest to be parents.
			Collections.sort(possibleParents);
			Route dad = possibleParents.get(0);
			Route mom = possibleParents.get(1);

			Route child;

			// Roll dice for crossover. If not crossover, keep dad as is.
			if (generator.nextDouble() > CROSSOVER_RATE)
				child = new Route(dad);
			else
				child = crossover(dad, mom);

			// Roll dice for mutation.
			if (generator.nextDouble() <= MUTATION_RATE)
				//mutateRandomCitySwap(child);
				mutateIVM(child);

			newChildren.add(child);
		}

		// Combine the elite and the new generation to form the new population.
		ArrayList<Route> newPopulation = new ArrayList<Route>(EVOLVING_POPULATION_SIZE);
		for (int i = 0; i < EVOLVING_POPULATION_SIZE - generation_size; ++i)
			newPopulation.add(routes.get(i));
		newPopulation.addAll(newChildren);
		routes = newPopulation;
		Collections.sort(routes);

	}

	public void mutateRandomCitySwap(Route r) {
		// Randomly swap two cities in the route.
		City[] cityList = r.getRoute();
		int c1 = generator.nextInt(numCities - 1);
		int c2 = generator.nextInt(numCities - 1);
		City temp = cityList[c1];
		cityList[c1] = cityList[c2];
		cityList[c2] = temp;
		r.setRoute(cityList);
		r.setRouteLength(calcRouteLength(cityList));
	}

	public void mutateIVM(Route r) {
		// Select a random subtour, reverse it and randomly add it back in.
		ArrayList<City> cityArrayList = new ArrayList<City>(Arrays.asList(r.getRoute()));
		int c1 = generator.nextInt(numCities - 1);
		int c2 = generator.nextInt(numCities - 1);
		if (c2 < c1) {
			int temp = c1;
			c1 = c2;
			c2 = temp;
		}
		City[] subtour = new City[c2 - c1 + 1];
		for (int i = subtour.length - 1; i > -1; --i)
			// (Removing c1 shifts the list.)
			subtour[i] = cityArrayList.remove(c1);

		int insertPt;
		if (cityArrayList.size() == 0 || cityArrayList.size() == 1) {
			insertPt = 0;
		} else {
			insertPt = generator.nextInt(cityArrayList.size() - 1);
		}

		City[] newCityArray = new City[numCities];
		int j = 0;
		int k = insertPt;
		for (int i = 0; i < numCities; ++i) {
			if (i < insertPt)
				newCityArray[i] = cityArrayList.get(i);
			else if (j < subtour.length) {
				newCityArray[i] = subtour[j];
				++j;
			} else {
				newCityArray[i] = cityArrayList.get(k);
				++k;
			}
		}
		r.setRoute(newCityArray);
		r.setRouteLength(calcRouteLength(newCityArray));
	}

	public Route crossover(Route dad, Route mom) {
		// Enhanced Edge Recombination (ER) Algorithm.
		City[] dadArray = dad.getRoute();
		City[] momArray = mom.getRoute();

		// Create the edge map.
		// <CityId : List of neighboring CityId's in mom and dad>.
		Map<Integer, HashSet<Integer>> edgeMap = new HashMap<Integer, HashSet<Integer>>();
		for (int dadIndex = 0; dadIndex < numCities; ++dadIndex) {
			City c = dadArray[dadIndex];
			int cityId = c.getId();
			// Get location of current city in dad--in mom.
			int momIndex = 0;
			for (int j = 0; j < numCities; ++j)
				if (momArray[j].equals(c)) {
					momIndex = j;
					break;
				}
			edgeMap.put(cityId, getEdges(dadArray, momArray, dadIndex, momIndex));
		}

		City[] childCityArray = new City[numCities];
		ArrayList<Integer> unvisitedCityIds = new ArrayList<Integer>(numCities);
		for (int i = 0; i < numCities; ++i)
			unvisitedCityIds.add(i);

		// Pick start city...
		City currentCity;
		int dadInitialCityConnctions = edgeMap.get(dadArray[0].getId()).size();
		int momInitialCityConnctions = edgeMap.get(momArray[0].getId()).size();
		if (dadInitialCityConnctions >= momInitialCityConnctions)
			currentCity = dadArray[0];
		else
			currentCity = momArray[0];
		childCityArray[0] = currentCity;
		int i = 1;
		unvisitedCityIds.remove(unvisitedCityIds.indexOf(currentCity.getId()));
		edgeMap = removeFromEdgeMap(edgeMap, currentCity.getId());

		while (!unvisitedCityIds.isEmpty()) {
			if (!edgeMap.get(currentCity.getId()).isEmpty()) // Step 4.
				currentCity = pickNextCity(edgeMap, currentCity.getId());
			else { // Step 5.
				int nextCityId;
				if (unvisitedCityIds.size() == 1)
					nextCityId = unvisitedCityIds.get(0);
				else
					nextCityId = unvisitedCityIds.get(generator.nextInt(unvisitedCityIds.size() - 1));
				currentCity = directory.get(nextCityId);
			}
			childCityArray[i] = currentCity;
			++i;
			unvisitedCityIds.remove(unvisitedCityIds.indexOf(currentCity.getId()));
			edgeMap = removeFromEdgeMap(edgeMap, currentCity.getId());
		}

		return new Route(childCityArray, calcRouteLength(childCityArray));
	}

	public boolean validRoute(Route r) {
		City[] cityList = r.getRoute();
		for (int i = 0; i < numCities; ++i) {
			boolean found = false;
			for (int j = 0; j < numCities; ++j)
				if (cityList[j].equals(baseCityArray[i]))
					found = true;
			if (!found)
				return false;
		}
		return true;
	}

	public HashSet<Integer> getEdges(City[] dadArray, City[] momArray, int dadIndex, int momIndex) {
		HashSet<Integer> edges = new HashSet<Integer>();

		// Get dad's edges.
		int front = (dadIndex + 1) % numCities;
		int back = (numCities + dadIndex - 1) % numCities;
		edges.add(dadArray[back].getId());
		edges.add(dadArray[front].getId());

		// Get mom's edges.
		front = (momIndex + 1) % numCities;
		back = (numCities + momIndex - 1) % numCities;
		// If the city is already in the edges list by dad,
		// flag it with a negative sign...
		int[] frontBack = { front, back };
		for (int i = 0; i < 2; ++i) {
			int id = momArray[frontBack[i]].getId();
			if (edges.contains(id)) {
				edges.remove(id);
				edges.add(-1 * id);
			} else
				// ...otherwise add as usual.
				edges.add(id);
		}
		return edges;
	}

	private City pickNextCity(Map<Integer, HashSet<Integer>> edgeMap, int id) {
		City nextCity;
		ArrayList<Integer> citiesToConsider = new ArrayList<Integer>(edgeMap.get(id));

		// 3 Possibilities:
		// 4 cities to consider: all positive.
		// 3 cities to consider: one of them could be negative.
		// 2 cities to consider: both could be negative.

		if (citiesToConsider.size() == 3) {
			// Pick the negative one if it exists.
			for (int i = 0; i < 3; ++i)
				if (citiesToConsider.get(i) < 0) {
					nextCity = directory.get(-1 * citiesToConsider.get(i));
					return nextCity;
				}
		} else if (citiesToConsider.size() == 1) {
			nextCity = directory.get(Math.abs(citiesToConsider.get(0)));
			return nextCity;
		}

		// If not picking a negative, or if all are negative,
		// pick the one with the least connections.
		int numMinConnections = Integer.MAX_VALUE;
		ArrayList<Integer> possibleChoices = new ArrayList<Integer>();
		for (Map.Entry<Integer, HashSet<Integer>> e : edgeMap.entrySet()) {
			// City in edge map listing could be positive or negative.
			if (citiesToConsider.contains(e.getKey()) || citiesToConsider.contains(-1 * e.getKey())) {
				int numConnections = e.getValue().size();
				if (numConnections < numMinConnections) {
					numMinConnections = numConnections;
					possibleChoices.clear();
					possibleChoices.add(e.getKey());
				} else if (e.getValue().size() == numMinConnections)
					possibleChoices.add(e.getKey());
			}
		}
		// If there is a tie for the least connections, randomly choose.
		if (possibleChoices.size() == 1)
			nextCity = directory.get(possibleChoices.get(0));
		else
			nextCity = directory.get(Math.abs(possibleChoices.get(generator.nextInt(possibleChoices.size() - 1))));
		return nextCity;
	}

	private Map<Integer, HashSet<Integer>> removeFromEdgeMap(Map<Integer, HashSet<Integer>> edgeMap, int id) {
		// Remove the given id (positive or negative) from all entries on the
		// right side of the edge map.
		for (Map.Entry<Integer, HashSet<Integer>> e : edgeMap.entrySet()) {
			HashSet<Integer> connections = e.getValue();
			connections.remove(id);
			connections.remove(-1 * id);
		}
		return edgeMap;
	}

	private Route nearestNeighborTour(City c) {
		ArrayList<City> cityArrayList = new ArrayList<City>(numCities);
		HashSet<Integer> citiesVisited = new HashSet<Integer>();

		// loop
		for (int j = 0; j < numCities; ++j) {
			cityArrayList.add(c);
			int id = c.getId();
			citiesVisited.add(id);
			double minDist = Double.MAX_VALUE;
			int index = 0;
			for (int i = 0; i < numCities; ++i) {
				double val = matrix[id][i];
				if (!citiesVisited.contains(i)) {
					if (val < minDist) {
						minDist = val;
						index = i;
					}
				}
			}
			c = directory.get(index);
		}

		City[] cityList = new City[numCities];
		cityArrayList.toArray(cityList);
		Route r = new Route(cityList, calcRouteLength(cityList));
		return r;
	}
}
