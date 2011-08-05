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
	private int iterations_per_generation;
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
		// Parse the file of city listings. (Name, x, y)
		FileReader file = new FileReader(path);
		BufferedReader bf = new BufferedReader(file);
		String line;
		ArrayList<City> list = new ArrayList<City>();
		while ((line = bf.readLine()) != null) {
			String[] params = line.split(",");
			String name = params[0].trim();
			double x = new Double(params[1].trim());
			double y = new Double(params[2].trim());
			City c = new City(name, list.size(), x, y);
			list.add(c);
		}

		// Store the number of cities to be traversed.
		numCities = list.size();

		// Create the "base" route and create the distance matrix based off of
		// the ordering of that route.
		baseCityArray = new City[numCities];
		for (int i = 0; i < numCities; ++i)
			baseCityArray[i] = list.get(i);
		createMatrix();

		// Initialize lookup table of city id's to their objects.
		directory = new HashMap<Integer, City>(numCities);
		for (City c : baseCityArray)
			directory.put(c.getId(), c);

		// Create the initial population of randomized routes.
		ArrayList<Route> init_routes = new ArrayList<Route>(POPULATION_SIZE);
		for (int i = 0; i < POPULATION_SIZE; ++i) {
			Collections.shuffle(list);
			City[] route = new City[numCities];
			for (int j = 0; j < numCities; ++j)
				route[j] = list.get(j);
			init_routes.add(new Route(route, calcRouteLength(route)));
		}
		Collections.sort(init_routes);

		// Pick the fittest routes.
		routes = new ArrayList<Route>(EVOLVING_POPULATION_SIZE);
		for (int i = 0; i < EVOLVING_POPULATION_SIZE; ++i)
			routes.add(init_routes.get(i));
		Collections.sort(routes);
	}

	private void createMatrix() {
		matrix = new double[numCities][numCities];
		for (int i = 0; i < numCities - 1; ++i) {
			matrix[i][i] = 0.0;
			for (int j = i + 1; j < numCities; ++j) {
				City city1 = baseCityArray[i];
				City city2 = baseCityArray[j];
				double t1 = (city1.getX() - city2.getX());
				double t2 = (city1.getY() - city2.getY());
				double distance = Math.sqrt(t1 * t1 + t2 * t2);
				matrix[i][j] = distance;
				matrix[j][i] = distance;
			}
		}
		matrix[numCities - 1][numCities - 1] = 0.0;
	}

	private double calcRouteLength(City[] route) {
		double distance = 0.0;
		int c1 = route[0].getId();
		for (int i = 1; i < numCities; ++i) {
			int c2 = route[i].getId();
			distance += matrix[c1][c2];
			c1 = c2;
		}
		distance += matrix[c1][route[0].getId()];
		return distance;
	}

	public void solve() {
		for (int i = 0; i < NUMBER_OF_GENERATIONS; ++i)
			evolve();
	}

	public void evolve() {

		// Create 2 new children each iteration until a full generation has been
		// born.
		iterations_per_generation = (int) Math.round((1 - ELITISM_PCT) * EVOLVING_POPULATION_SIZE);
		ArrayList<Route> newChildren = new ArrayList<Route>(iterations_per_generation);
		for (int j = 0; j < iterations_per_generation; ++j) {

			// Randomly select a set of routes.
			ArrayList<Route> possibleParents = new ArrayList<Route>(6);
			for (int i = 0; i < TOURNAMENT_SIZE; ++i) {
				int index = generator.nextInt(routes.size() - 1);
				possibleParents.add(routes.get(index));
			}

			// Select the 2 fittest to be parents.
			Collections.sort(possibleParents);
			Route dad = possibleParents.get(0);
			Route mom = possibleParents.get(1);

			Route child;

			// Roll dice for crossover.
			if (generator.nextDouble() > CROSSOVER_RATE) {
				Route[] dadMom = { dad, mom };
				child = new Route(dadMom[generator.nextInt(1)]);
			} else
				child = crossover(dad, mom);

			// Roll dice for mutation.
			if (generator.nextDouble() <= MUTATION_RATE)
				mutate(child);

			newChildren.add(child);
		}

		ArrayList<Route> temp = new ArrayList<Route>(EVOLVING_POPULATION_SIZE);

		for (int i = 0; i < EVOLVING_POPULATION_SIZE - iterations_per_generation; ++i)
			temp.add(routes.get(i));

		// Insert new generation.
		for (Route r : newChildren)
			temp.add(r);

		routes = temp;
		Collections.sort(routes);

	}

	public void mutate(Route r) {
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
		ArrayList<City> cityList = new ArrayList<City>(Arrays.asList(r.getRoute()));
		// for (City c : cityList)
		// System.out.print(c.getId() + ",");
		// System.out.print("\n");
		int c1 = generator.nextInt(numCities - 1);
		int c2 = generator.nextInt(numCities - 1);
		if (c2 < c1) {
			int temp = c1;
			c1 = c2;
			c2 = temp;
		}
		// System.out.println(c1 + ", " + c2);
		City[] strip = new City[c2 - c1 + 1];
		for (int i = strip.length - 1; i > -1; --i) {
			strip[i] = cityList.remove(c1);
		}
		int insertPt;
		if (cityList.size() == 0 || cityList.size() == 1) {
			insertPt = 0;
		} else {
			// System.out.println(cityList.size());
			insertPt = generator.nextInt(cityList.size() - 1);
		}
		City[] cityList2 = new City[numCities];

		int j = 0;
		int k = insertPt;
		for (int i = 0; i < numCities; ++i) {
			if (i < insertPt)
				cityList2[i] = cityList.get(i);
			else if (j < strip.length) {
				cityList2[i] = strip[j];
				++j;
			} else {
				cityList2[i] = cityList.get(k);
				++k;
			}
		}
		// for (City c : cityList2)
		// System.out.print(c.getId() + ",");
		// System.out.print("\n");
		r.setRoute(cityList2);
		r.setRouteLength(calcRouteLength(cityList2));
	}

	public Route crossover(Route dad, Route mom) {
		City[] dadList = dad.getRoute();
		City[] momList = mom.getRoute();

		// Create the edge map.
		Map<Integer, HashSet<Integer>> edgeMap = new HashMap<Integer, HashSet<Integer>>();
		for (int i = 0; i < numCities; ++i) {
			City c = dadList[i];
			int cityId = c.getId();
			// Get location of city i in mom.
			int momLoc = 0;
			for (int j = 0; j < numCities; ++j)
				if (momList[j].equals(c)) {
					momLoc = j;
					break;
				}
			edgeMap.put(cityId, getEdges(dadList, momList, i, momLoc));
		}

		City[] child = new City[numCities];
		ArrayList<Integer> unvisitedCityIds = new ArrayList<Integer>(numCities);
		for (int i = 0; i < numCities; ++i)
			unvisitedCityIds.add(i);

		// Pick start city.
		int dadInitialCityConnctions = edgeMap.get(dadList[0].getId()).size();
		int momInitialCityConnctions = edgeMap.get(momList[0].getId()).size();
		City currentCity;
		if (dadInitialCityConnctions >= momInitialCityConnctions)
			currentCity = dadList[0];
		else
			currentCity = momList[0];
		child[0] = currentCity;
		int i = 1;
		unvisitedCityIds.remove(unvisitedCityIds.indexOf(currentCity.getId()));
		edgeMap = removeFromEdgeMap(edgeMap, currentCity.getId());

		while (!unvisitedCityIds.isEmpty()) {
			if (!edgeMap.get(currentCity.getId()).isEmpty()) {
				// Step 4.
				currentCity = pickNextCity(edgeMap, currentCity.getId());
			} else {
				// Step 5.
				int nextCityId;
				if (unvisitedCityIds.size() == 1)
					nextCityId = unvisitedCityIds.get(0);
				else
					nextCityId = unvisitedCityIds.get(generator.nextInt(unvisitedCityIds.size() - 1));
				currentCity = directory.get(nextCityId);
			}
			child[i] = currentCity;
			++i;
			unvisitedCityIds.remove(unvisitedCityIds.indexOf(currentCity.getId()));
			edgeMap = removeFromEdgeMap(edgeMap, currentCity.getId());
		}

		Route r = new Route(child, calcRouteLength(child));
		return r;
	}

	public boolean validRoute(Route r) {
		City[] cityList = r.getRoute();
		for (int i = 0; i < numCities; ++i) {
			boolean found = false;
			for (int j = 0; j < numCities; ++j) {
				if (cityList[j].equals(baseCityArray[i])) {
					found = true;
				}
			}
			if (!found) {
				return false;
			}
		}
		return true;
	}

	public HashSet<Integer> getEdges(City[] cityList1, City[] cityList2, int i, int j) {

		HashSet<Integer> edges = new HashSet<Integer>();
		int front = (i + 1) % numCities;
		int back = (numCities + i - 1) % numCities;
		edges.add(cityList1[back].getId());
		edges.add(cityList1[front].getId());
		front = (j + 1) % numCities;
		back = (numCities + j - 1) % numCities;
		int id = cityList2[front].getId();
		if (edges.contains(id)) {
			edges.remove(id);
			edges.add(-1 * id);
		} else
			edges.add(id);
		id = cityList2[back].getId();
		if (edges.contains(id)) {
			edges.remove(id);
			edges.add(-1 * id);
		} else
			edges.add(id);
		return edges;
	}

	private City pickNextCity(Map<Integer, HashSet<Integer>> edgeMap, int id) {
		HashSet<Integer> citiesToConsider = edgeMap.get(id);

		City c;
		if (citiesToConsider.size() == 3) {
			ArrayList<Integer> edgeList = new ArrayList<Integer>(citiesToConsider);
			// pick the negative one
			for (int i = 0; i < 3; ++i)
				if (edgeList.get(i) < 0) {
					c = directory.get(-1 * edgeList.get(i));
					return c;
				}
		} else if (citiesToConsider.size() == 1) {
			ArrayList<Integer> edgeList = new ArrayList<Integer>(citiesToConsider);
			c = directory.get(Math.abs(edgeList.get(0)));
			return c;
		}

		int numMinConnections = Integer.MAX_VALUE;
		ArrayList<Integer> possibles = new ArrayList<Integer>();
		for (Map.Entry<Integer, HashSet<Integer>> e : edgeMap.entrySet()) {
			// System.out.println("conns:" + e.getKey() + ": " + e.getValue() +
			// ": consider" + citiesToConsider);
			if (citiesToConsider.contains(e.getKey()) || citiesToConsider.contains(-1 * e.getKey())) {
				if (e.getValue().size() < numMinConnections) {
					numMinConnections = e.getValue().size();
					possibles.clear();
					possibles.add(e.getKey());
				} else if (e.getValue().size() == numMinConnections) {
					possibles.add(e.getKey());
				}
			}
		}

		if (possibles.size() == 1) {
			c = directory.get(possibles.get(0));
		} else {
			c = directory.get(Math.abs(possibles.get(generator.nextInt(possibles.size() - 1))));
		}
		return c;
	}

	private Map<Integer, HashSet<Integer>> removeFromEdgeMap(Map<Integer, HashSet<Integer>> edgeMap, int id) {
		for (Map.Entry<Integer, HashSet<Integer>> e : edgeMap.entrySet()) {
			HashSet<Integer> connections = e.getValue();
			connections.remove(id);
			connections.remove(-1 * id);
		}
		return edgeMap;
	}
}
